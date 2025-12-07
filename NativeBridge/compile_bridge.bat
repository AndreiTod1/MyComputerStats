@echo off
cd /d "%~dp0"

:: Use the known working path for VS
call "D:\Visual\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
if %errorLevel% neq 0 (
    call "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat" >nul 2>&1
)

echo Compiling MonitorBridge...
cl /EHsc /O2 Bridge.cpp /link /OUT:MonitorBridge.exe

if %errorLevel% equ 0 (
    echo [SUCCESS] MonitorBridge.exe created.
) else (
    echo [ERROR] Compilation failed.
    pause
)
