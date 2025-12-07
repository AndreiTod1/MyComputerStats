#include <windows.h>
#include <stdio.h>

// OlsApi Typedefs (WinRing0)
typedef int (WINAPI *_InitializeOls)();
typedef void (WINAPI *_DeinitializeOls)();
typedef DWORD (WINAPI *_GetDllStatus)();
typedef int (WINAPI *_IsCpuid)();
typedef int (WINAPI *_IsMsr)();
typedef int (WINAPI *_Rdmsr)(DWORD index, DWORD *eax, DWORD *edx);

// Global Function Pointers
_InitializeOls InitializeOls = NULL;
_DeinitializeOls DeinitializeOls = NULL;
_GetDllStatus GetDllStatus = NULL;
_IsCpuid IsCpuid = NULL;
_IsMsr IsMsr = NULL;
_Rdmsr Rdmsr = NULL;

extern "C" __declspec(dllexport) int CPUMonitor_Init() {
    HMODULE hDll = LoadLibrary(L"WinRing0x64.dll");
    if (!hDll) {
        // Try current directory
        hDll = LoadLibrary(L".\\WinRing0x64.dll");
    }

    if (!hDll) return -1; // DLL Not Found

    InitializeOls = (_InitializeOls)GetProcAddress(hDll, "InitializeOls");
    DeinitializeOls = (_DeinitializeOls)GetProcAddress(hDll, "DeinitializeOls");
    GetDllStatus = (_GetDllStatus)GetProcAddress(hDll, "GetDllStatus");
    IsCpuid = (_IsCpuid)GetProcAddress(hDll, "IsCpuid");
    IsMsr = (_IsMsr)GetProcAddress(hDll, "IsMsr");
    Rdmsr = (_Rdmsr)GetProcAddress(hDll, "Rdmsr");

    if (!InitializeOls || !IsMsr || !Rdmsr) return -2; // Functions missing

    if (!InitializeOls()) return -3; // Driver Init Failed

    return 1; // Success
}

// NOTE: This is a legacy source file for CPUMonitor.dll.
// The active application uses MonitorBridge.exe (Source: Bridge.cpp)
