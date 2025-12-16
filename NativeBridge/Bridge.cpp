/*
 * MonitorBridge - native cpu monitoring for MyComputerStats
 * 
 * reads temperature, frequency, voltage, throttling and power from intel/amd cpus
 * using winring0 for low-level msr and pci access
 * 
 * output format:
 *   INIT,coreCount,tjMax,cpuName
 *   DATA,pkgTemp,c0T,c0F,c0V,...,thermalThrottle,powerThrottle,powerWatts
 */

#include <windows.h>
#include <stdio.h>
#include <intrin.h>

// === winring0 function pointers ===
typedef int  (__stdcall *pfnInitializeOls)();
typedef void (__stdcall *pfnDeinitializeOls)();
typedef int  (__stdcall *pfnRdmsr)(DWORD index, PDWORD eax, PDWORD edx);
typedef BOOL (__stdcall *pfnReadPciConfigDwordEx)(DWORD pciAddress, DWORD regAddress, PDWORD value);
typedef BOOL (__stdcall *pfnWritePciConfigDwordEx)(DWORD pciAddress, DWORD regAddress, DWORD value);

pfnInitializeOls InitializeOls = NULL;
pfnDeinitializeOls DeinitializeOls = NULL;
pfnRdmsr Rdmsr = NULL;
pfnReadPciConfigDwordEx ReadPciConfigDwordEx = NULL;
pfnWritePciConfigDwordEx WritePciConfigDwordEx = NULL;

HMODULE hWinRing0 = NULL;

// === msr addresses ===
// intel thermal and performance
#define MSR_THERM_STATUS        0x19C   // thermal status and throttle bits
#define MSR_TEMPERATURE_TARGET  0x1A2   // tjmax value
#define MSR_PERF_STATUS         0x198   // frequency multiplier and voltage

// intel rapl (power measurement)
#define MSR_RAPL_POWER_UNIT     0x606   // energy unit conversion
#define MSR_PKG_ENERGY_STATUS   0x611   // package energy counter

// amd pstate
#define AMD_PSTATE_STATUS       0xC0010063
#define AMD_PSTATE_0            0xC0010064

// amd rapl
#define AMD_RAPL_POWER_UNIT     0xC0010299
#define AMD_PKG_ENERGY_STATUS   0xC001029B

// amd smn for temperature
#define AMD_SMN_INDEX           0x60
#define AMD_SMN_DATA            0x64
#define AMD_TEMP_REG            0x00059800

// === globals ===
enum CpuVendor { CPU_UNKNOWN, CPU_INTEL, CPU_AMD };

int g_TjMax = 100;
int g_CoreCount = 0;
CpuVendor g_Vendor = CPU_UNKNOWN;
DWORD g_AmdPciAddr = 0;
bool g_AmdSmnOk = false;

// rapl tracking
ULONGLONG g_LastEnergy = 0;
ULONGLONG g_LastEnergyTime = 0;
double g_EnergyUnit = 0;
double g_CurrentPower = 0;

// === cpu detection ===
CpuVendor DetectVendor() {
    int info[4] = {0};
    char vendor[13] = {0};
    
    __cpuid(info, 0);
    memcpy(vendor, &info[1], 4);
    memcpy(vendor + 4, &info[3], 4);
    memcpy(vendor + 8, &info[2], 4);
    
    if (strcmp(vendor, "GenuineIntel") == 0) return CPU_INTEL;
    if (strcmp(vendor, "AuthenticAMD") == 0) return CPU_AMD;
    return CPU_UNKNOWN;
}

void GetCpuName(char* buf, int size) {
    int info[4] = {-1};
    char brand[49] = {0};
    
    __cpuid(info, 0x80000000);
    if ((unsigned)info[0] >= 0x80000004) {
        for (int i = 0x80000002; i <= 0x80000004; i++) {
            __cpuid(info, i);
            memcpy(brand + (i - 0x80000002) * 16, info, sizeof(info));
        }
    }
    snprintf(buf, size, "%s", brand);
}

// === driver loading ===
bool LoadDriver() {
    hWinRing0 = LoadLibraryA("WinRing0x64.dll");
    if (!hWinRing0) {
        fprintf(stderr, "cant load winring0x64.dll\n");
        return false;
    }

    InitializeOls = (pfnInitializeOls)GetProcAddress(hWinRing0, "InitializeOls");
    DeinitializeOls = (pfnDeinitializeOls)GetProcAddress(hWinRing0, "DeinitializeOls");
    Rdmsr = (pfnRdmsr)GetProcAddress(hWinRing0, "Rdmsr");
    ReadPciConfigDwordEx = (pfnReadPciConfigDwordEx)GetProcAddress(hWinRing0, "ReadPciConfigDwordEx");
    WritePciConfigDwordEx = (pfnWritePciConfigDwordEx)GetProcAddress(hWinRing0, "WritePciConfigDwordEx");

    if (!InitializeOls || !DeinitializeOls || !Rdmsr) {
        fprintf(stderr, "missing winring0 functions\n");
        return false;
    }

    if (!InitializeOls()) {
        fprintf(stderr, "driver init failed\n");
        return false;
    }

    return true;
}

// === amd smn access ===
DWORD MakePciAddr(BYTE bus, BYTE dev, BYTE func) {
    return ((DWORD)bus << 8) | ((dev & 0x1F) << 3) | (func & 0x7);
}

DWORD ReadSmn(DWORD addr) {
    if (!g_AmdSmnOk) return 0;
    if (!WritePciConfigDwordEx(g_AmdPciAddr, AMD_SMN_INDEX, addr)) return 0;
    
    DWORD result = 0;
    if (!ReadPciConfigDwordEx(g_AmdPciAddr, AMD_SMN_DATA, &result)) return 0;
    return result;
}

bool InitAmdSmn() {
    if (!ReadPciConfigDwordEx || !WritePciConfigDwordEx) return false;
    
    g_AmdPciAddr = MakePciAddr(0, 0, 0);
    g_AmdSmnOk = true;
    
    DWORD testVal = ReadSmn(AMD_TEMP_REG);
    if (testVal == 0 || testVal == 0xFFFFFFFF) {
        g_AmdSmnOk = false;
        return false;
    }
    return true;
}

// === rapl initialization ===
void InitRapl() {
    DWORD eax = 0, edx = 0;
    DWORD msrUnit = (g_Vendor == CPU_INTEL) ? MSR_RAPL_POWER_UNIT : AMD_RAPL_POWER_UNIT;
    
    if (Rdmsr(msrUnit, &eax, &edx)) {
        // energy unit is in bits 12:8, value = 1 / (2^unit) joules
        int energyBits = (eax >> 8) & 0x1F;
        g_EnergyUnit = 1.0 / (1 << energyBits);
    }
    
    // read initial energy value
    DWORD msrEnergy = (g_Vendor == CPU_INTEL) ? MSR_PKG_ENERGY_STATUS : AMD_PKG_ENERGY_STATUS;
    if (Rdmsr(msrEnergy, &eax, &edx)) {
        g_LastEnergy = ((ULONGLONG)edx << 32) | eax;
        g_LastEnergyTime = GetTickCount64();
    }
}

// === intel readings ===
int GetTjMax() {
    DWORD eax, edx;
    if (Rdmsr(MSR_TEMPERATURE_TARGET, &eax, &edx)) {
        return (eax >> 16) & 0xFF;
    }
    return 100;
}

int ReadIntelTemp() {
    DWORD eax = 0, edx = 0;
    if (Rdmsr(MSR_THERM_STATUS, &eax, &edx)) {
        if (eax & 0x80000000) {
            int offset = (eax >> 16) & 0x7F;
            return g_TjMax - offset;
        }
    }
    return 0;
}

void ReadIntelPerf(int* freq, double* volt) {
    DWORD eax = 0, edx = 0;
    if (Rdmsr(MSR_PERF_STATUS, &eax, &edx)) {
        *volt = (edx & 0xFFFF) / 8192.0;
        *freq = ((eax >> 8) & 0xFF) * 100;
    }
}

// read throttle status from therm_status msr
// bit 0: thermal throttle active now
// bit 1: thermal throttle log (has occurred since last clear)
// bit 10: prochot or forcepr event
bool ReadIntelThermalThrottle() {
    DWORD eax = 0, edx = 0;
    if (Rdmsr(MSR_THERM_STATUS, &eax, &edx)) {
        return (eax & 0x1) != 0;  // bit 0 = thermal status
    }
    return false;
}

bool ReadIntelPowerThrottle() {
    DWORD eax = 0, edx = 0;
    if (Rdmsr(MSR_THERM_STATUS, &eax, &edx)) {
        return (eax & (1 << 10)) != 0;  // bit 10 = prochot log
    }
    return false;
}

// === amd readings ===
int ReadAmdTemp() {
    DWORD val = ReadSmn(AMD_TEMP_REG);
    if (val == 0) return 0;
    
    int tempRaw = (val >> 21) & 0x7FF;
    int tempMilliC = tempRaw * 125;
    if (val & (1 << 19)) tempMilliC -= 49000;
    
    return tempMilliC / 1000;
}

int ReadAmdFreq() {
    DWORD eax = 0, edx = 0;
    if (!Rdmsr(AMD_PSTATE_STATUS, &eax, &edx)) return 0;
    int pstate = eax & 0x7;
    
    if (!Rdmsr(AMD_PSTATE_0 + pstate, &eax, &edx)) return 0;
    int fid = eax & 0xFF;
    int did = (eax >> 8) & 0x3F;
    
    return did ? (200 * fid) / did : 0;
}

double ReadAmdVolt() {
    DWORD eax = 0, edx = 0;
    if (!Rdmsr(AMD_PSTATE_STATUS, &eax, &edx)) return 0.0;
    int pstate = eax & 0x7;
    
    if (!Rdmsr(AMD_PSTATE_0 + pstate, &eax, &edx)) return 0.0;
    int vid = (eax >> 14) & 0xFF;
    
    double v = 1.55 - (vid * 0.00625);
    return v > 0 ? v : 0;
}

// amd throttle detection - based on temp vs tjmax
bool ReadAmdThermalThrottle() {
    int temp = ReadAmdTemp();
    return temp >= 95;  // typical amd throttle point
}

// === power reading ===
double ReadPackagePower() {
    DWORD eax = 0, edx = 0;
    DWORD msrEnergy = (g_Vendor == CPU_INTEL) ? MSR_PKG_ENERGY_STATUS : AMD_PKG_ENERGY_STATUS;
    
    if (!Rdmsr(msrEnergy, &eax, &edx)) return 0;
    
    ULONGLONG currentEnergy = ((ULONGLONG)edx << 32) | eax;
    ULONGLONG currentTime = GetTickCount64();
    
    double power = 0;
    if (g_LastEnergyTime > 0 && currentTime > g_LastEnergyTime) {
        ULONGLONG energyDiff = currentEnergy - g_LastEnergy;
        double timeDiff = (currentTime - g_LastEnergyTime) / 1000.0;
        
        // handle counter wrap
        if (currentEnergy < g_LastEnergy) {
            energyDiff = (0xFFFFFFFF - g_LastEnergy) + currentEnergy;
        }
        
        if (timeDiff > 0) {
            power = (energyDiff * g_EnergyUnit) / timeDiff;
        }
    }
    
    g_LastEnergy = currentEnergy;
    g_LastEnergyTime = currentTime;
    g_CurrentPower = power;
    
    return power;
}

// === main loop ===
int main(int argc, char* argv[]) {
    g_Vendor = DetectVendor();
    
    if (!LoadDriver()) {
        printf("ERROR: cant load driver, run as admin\n");
        return 1;
    }
    
    if (g_Vendor == CPU_AMD) {
        InitAmdSmn();
    }
    
    InitRapl();

    g_CoreCount = GetActiveProcessorCount(ALL_PROCESSOR_GROUPS);
    g_TjMax = (g_Vendor == CPU_INTEL) ? GetTjMax() : 95;
    
    char cpuName[64] = "Unknown";
    GetCpuName(cpuName, 64);

    printf("INIT,%d,%d,%s\n", g_CoreCount, g_TjMax, cpuName);
    fflush(stdout);

    while (true) {
        printf("DATA,%d", 0);

        // per-core data
        for (int i = 0; i < g_CoreCount; i++) {
            SetThreadAffinityMask(GetCurrentThread(), (DWORD_PTR)1 << i);
            Sleep(1);

            int temp = 0;
            int freq = 0;
            double volt = 0.0;

            if (g_Vendor == CPU_INTEL) {
                temp = ReadIntelTemp();
                ReadIntelPerf(&freq, &volt);
            } else if (g_Vendor == CPU_AMD) {
                temp = ReadAmdTemp();
                freq = ReadAmdFreq();
                volt = ReadAmdVolt();
            }
            
            printf(",%d,%d,%.4f", temp, freq, volt);
        }
        
        // throttle and power data at end
        int thermalThrottle = 0;
        int powerThrottle = 0;
        double power = ReadPackagePower();
        
        if (g_Vendor == CPU_INTEL) {
            thermalThrottle = ReadIntelThermalThrottle() ? 1 : 0;
            powerThrottle = ReadIntelPowerThrottle() ? 1 : 0;
        } else if (g_Vendor == CPU_AMD) {
            thermalThrottle = ReadAmdThermalThrottle() ? 1 : 0;
            powerThrottle = 0;  // amd doesn't expose this easily
        }
        
        printf(",%d,%d,%.2f\n", thermalThrottle, powerThrottle, power);
        fflush(stdout);
        Sleep(250);
    }

    DeinitializeOls();
    FreeLibrary(hWinRing0);
    return 0;
}
