@echo off
cd /d %~dp0

echo ========================================
echo  MyComputerStats - Native Bridge Build
echo ========================================
echo.

:: find visual studio
set "VCVARS="

:: check common locations
if exist "C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat" (
    set "VCVARS=C:\Program Files\Microsoft Visual Studio\2022\Community\VC\Auxiliary\Build\vcvars64.bat"
)
if exist "C:\Program Files\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build\vcvars64.bat" (
    set "VCVARS=C:\Program Files\Microsoft Visual Studio\2022\Professional\VC\Auxiliary\Build\vcvars64.bat"
)
if exist "C:\Program Files\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build\vcvars64.bat" (
    set "VCVARS=C:\Program Files\Microsoft Visual Studio\2022\Enterprise\VC\Auxiliary\Build\vcvars64.bat"
)
if exist "C:\Program Files\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat" (
    set "VCVARS=C:\Program Files\Microsoft Visual Studio\2019\Community\VC\Auxiliary\Build\vcvars64.bat"
)
if exist "D:\Visual\VC\Auxiliary\Build\vcvars64.bat" (
    set "VCVARS=D:\Visual\VC\Auxiliary\Build\vcvars64.bat"
)

:: init environment if needed
if not defined DevEnvDir (
    if "%VCVARS%"=="" (
        echo ERROR: Visual Studio not found!
        echo.
        echo Install Visual Studio with C++ Desktop Development
        echo or run this from Developer Command Prompt
        pause
        exit /b 1
    )
    call "%VCVARS%"
)

echo.
echo compiling bridge.cpp...
cl /EHsc /O2 /MT Bridge.cpp /link /OUT:MonitorBridge.exe

if %errorLevel% equ 0 (
    echo.
    echo build successful!
    echo.
    
    :: copy to native folder
    if not exist "..\MyComputerStats\native" mkdir "..\MyComputerStats\native"
    copy /Y MonitorBridge.exe "..\MyComputerStats\native\MonitorBridge.exe" >nul
    copy /Y WinRing0x64.dll "..\MyComputerStats\native\WinRing0x64.dll" >nul
    copy /Y WinRing0x64.sys "..\MyComputerStats\native\WinRing0x64.sys" >nul
    
    echo files copied to MyComputerStats/native/
) else (
    echo.
    echo build failed!
)

echo.
pause
