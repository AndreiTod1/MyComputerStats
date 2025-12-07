#include <windows.h>
#include <stdio.h>
#include <iostream>
#include <vector>
#include <intrin.h>

// WinRing0 function signatures
typedef int (__stdcall *pfnInitializeOls)();
typedef void (__stdcall *pfnDeinitializeOls)();
typedef int (__stdcall *pfnRdmsr)(DWORD index, PDWORD eax, PDWORD edx);
typedef int (__stdcall *pfnCpuid)(DWORD index, PDWORD eax, PDWORD ebx, PDWORD ecx, PDWORD edx);

pfnInitializeOls InitializeOls = NULL;
pfnDeinitializeOls DeinitializeOls = NULL;
pfnRdmsr Rdmsr = NULL;
pfnCpuid Cpuid = NULL;

// CPU MSRs
#define MSR_IA32_THERM_STATUS      0x19C
#define MSR_IA32_TEMPERATURE_TARGET 0x1A2
#define MSR_IA32_PERF_STATUS       0x198

struct CoreData {
    int temp;
    int freq;
    double volt;
};

// Global handles
HMODULE hWinRing0 = NULL;
int g_TjMax = 100;
int g_CoreCount = 0;

bool LoadDriver() {
    hWinRing0 = LoadLibraryA("WinRing0x64.dll");
    if (!hWinRing0) return false;

    InitializeOls = (pfnInitializeOls)GetProcAddress(hWinRing0, "InitializeOls");
    DeinitializeOls = (pfnDeinitializeOls)GetProcAddress(hWinRing0, "DeinitializeOls");
    Rdmsr = (pfnRdmsr)GetProcAddress(hWinRing0, "Rdmsr");
    Cpuid = (pfnCpuid)GetProcAddress(hWinRing0, "Cpuid");

    if (!InitializeOls || !DeinitializeOls || !Rdmsr || !Cpuid) return false;

    return InitializeOls() != 0;
}

int GetTjMax() {
    DWORD eax, edx;
    if (Rdmsr(MSR_IA32_TEMPERATURE_TARGET, &eax, &edx)) {
        return (eax >> 16) & 0xFF;
    }
    return 100; // Default fallback
}

void GetCPUName(char* buffer, int size) {
    int cpuInfo[4] = { -1 };
    char brand[49] = { 0 };
    __cpuid(cpuInfo, 0x80000000);
    unsigned int nExIds = cpuInfo[0];
    
    if (nExIds >= 0x80000004) {
        for (int i = 0x80000002; i <= 0x80000004; i++) {
            __cpuid(cpuInfo, i);
            memcpy(brand + (i - 0x80000002) * 16, cpuInfo, sizeof(cpuInfo));
        }
    }
    snprintf(buffer, size, "%s", brand);
}

int main(int argc, char* argv[]) {
    if (!LoadDriver()) {
        printf("ERROR: WinRing0 load failed. Run as Admin?\n");
        return 1;
    }

    g_CoreCount = GetActiveProcessorCount(ALL_PROCESSOR_GROUPS);
    g_TjMax = GetTjMax();
    
    char cpuName[64] = "Unknown CPU";
    GetCPUName(cpuName, 64);

    // Initial Handshake
    printf("INIT,%d,%d,%s\n", g_CoreCount, g_TjMax, cpuName);
    fflush(stdout);

    // Monitor Loop
    while (true) {
        printf("DATA,%d", 0); // Package Temp (dummy 0 for now)

        for (int i = 0; i < g_CoreCount; i++) {
            HANDLE hThread = GetCurrentThread();
            DWORD_PTR mask = (DWORD_PTR)1 << i;
            SetThreadAffinityMask(hThread, mask);
            Sleep(0); // Yield to switch core

            DWORD eax = 0, edx = 0;
            int temp = 0;
            int freq = 0;
            double volt = 0.0;

            // Read Temp
            if (Rdmsr(MSR_IA32_THERM_STATUS, &eax, &edx)) {
                if (eax & 0x80000000) { // Reading valid
                    int digitalReadout = (eax >> 16) & 0x7F;
                    temp = g_TjMax - digitalReadout;
                }
            }

            // Read Voltage (VID) - very simplified
            if (Rdmsr(MSR_IA32_PERF_STATUS, &eax, &edx)) {
                int vid = (edx >> 16) & 0xFF; // Bits 47:32 of full MSR
                // Simple approx for standard Intel VID (not accurate for all)
                volt = vid > 0 ? 0.6 + (vid * 0.005) : 0.0; 
                
                int multiplier = (eax >> 8) & 0xFF;
                freq = multiplier * 100; // Approx Bus Speed 100MHz
            }
            
            // Output: temp, freq, volt(mv)
            printf(",%d,%d,%d", temp, freq, (int)(volt * 1000));
        }
        printf("\n");
        fflush(stdout);
        Sleep(1000);
    }

    DeinitializeOls();
    FreeLibrary(hWinRing0);
    return 0;
}
