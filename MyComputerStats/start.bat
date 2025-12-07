@echo off
setlocal
cd /d "%~dp0"

: admin check
net session >nul 2>&1
if %errorLevel% neq 0 (
    echo Requesting Administrator privileges...
    powershell -Command "Start-Process '%~0' -Verb RunAs"
    exit /b
)

: build check
if not exist "target\MyComputerStats-1.0-SNAPSHOT.jar" (
    echo JAR not found. Building project...
    call mvn clean package
    if %errorLevel% neq 0 (
        echo Build failed!
        pause
        exit /b 1
    )
)

: run application
echo Starting MyComputerStats (Native Bridge Mode)...

: rebuild java
echo Updating Java application...
call mvn package -DskipTests
if %errorLevel% neq 0 (
    echo Java build failed!
    pause
    exit /b
)

: check native bridge
if not exist "..\NativeBridge\MonitorBridge.exe" (
    echo WARNING: MonitorBridge.exe not found in ..\NativeBridge!
    echo Please compile it manually as requested.
)

java -jar "target\MyComputerStats-1.0-SNAPSHOT.jar"
pause
