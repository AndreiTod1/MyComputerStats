@echo off
setlocal
cd /d "%~dp0"

echo ========================================
echo  MyComputerStats
echo ========================================
echo.

:: admin check
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo need admin rights for cpu monitoring
    echo requesting elevation...
    powershell -Command "Start-Process '%~0' -Verb RunAs"
    exit /b
)

:: check native bridge
if not exist "native\MonitorBridge.exe" (
    echo native bridge not found!
    echo.
    echo run NativeBridge\compile_bridge.bat first
    pause
    exit /b 1
)

:: check java
java -version >nul 2>&1
if %errorLevel% neq 0 (
    echo java not found! install java 17+
    pause
    exit /b 1
)

:: build if needed
if not exist "target\MyComputerStats-1.0-SNAPSHOT.jar" (
    echo building project...
    call mvn package -q -DskipTests
    if %errorLevel% neq 0 (
        echo build failed!
        pause
        exit /b 1
    )
)

:: run
echo starting app...
java -jar "target\MyComputerStats-1.0-SNAPSHOT.jar"
