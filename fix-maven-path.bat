@echo off
setlocal enabledelayedexpansion

echo.
echo ========================================
echo   Maven PATH Fix Utility
echo ========================================
echo.

:: Check if Maven is already in PATH
where mvn >nul 2>&1
if not errorlevel 1 (
    echo Maven is already accessible in PATH!
    echo.
    mvn -version
    echo.
    pause
    exit /b 0
)

echo Maven is not in PATH. Searching for Maven installation...
echo.

:: Common Maven installation locations
set "MAVEN_PATHS=C:\Program Files\Apache\Maven"
set "MAVEN_PATHS=%MAVEN_PATHS%;C:\Program Files\Maven"
set "MAVEN_PATHS=%MAVEN_PATHS%;C:\Maven"
set "MAVEN_PATHS=%MAVEN_PATHS%;%USERPROFILE%\Maven"

set "FOUND_MAVEN="

for %%P in (%MAVEN_PATHS%) do (
    if exist "%%P" (
        echo Checking: %%P
        for /d %%D in ("%%P\*") do (
            if exist "%%D\bin\mvn.cmd" (
                set "FOUND_MAVEN=%%D\bin"
                echo Found Maven at: %%D\bin
                goto :found
            )
        )
    )
)

:found
if not defined FOUND_MAVEN (
    echo.
    echo [ERROR] Maven installation not found!
    echo.
    echo Please install Maven:
    echo   1. Download from: https://maven.apache.org/download.cgi
    echo   2. Extract to: C:\Program Files\Apache\Maven
    echo   3. Run this script again
    echo.
    echo Or run: setup-environment.bat (as Administrator)
    echo.
    pause
    exit /b 1
)

echo.
echo Found Maven at: %FOUND_MAVEN%
echo.
echo To add Maven to PATH, you have two options:
echo.
echo Option 1: Add to System PATH (Permanent - Requires Admin)
echo   This will make Maven available for all users and all command prompts
echo.
echo Option 2: Add to User PATH (Permanent - No Admin Required)
echo   This will make Maven available for your user account only
echo.
echo Option 3: Temporary (Current Session Only)
echo   This will only work in the current command prompt window
echo.

set /p CHOICE="Choose option (1/2/3): "

if "%CHOICE%"=="1" (
    echo.
    echo Adding to System PATH requires Administrator privileges...
    echo Please run this script as Administrator!
    echo.
    echo Right-click on this file and select "Run as administrator"
    pause
    exit /b 1
)

if "%CHOICE%"=="2" (
    echo.
    echo Adding Maven to User PATH...
    setx PATH "%PATH%;%FOUND_MAVEN%"
    echo.
    echo [SUCCESS] Maven added to User PATH!
    echo.
    echo IMPORTANT: Close this window and open a NEW Command Prompt
    echo The changes will only take effect in new command prompt windows.
    echo.
    pause
    exit /b 0
)

if "%CHOICE%"=="3" (
    echo.
    echo Adding Maven to current session...
    set "PATH=%PATH%;%FOUND_MAVEN%"
    echo.
    echo [SUCCESS] Maven added to PATH for this session!
    echo.
    echo Testing Maven...
    mvn -version
    echo.
    echo NOTE: This is temporary. Maven will only work in THIS window.
    echo To make it permanent, run this script again and choose option 1 or 2.
    echo.
    pause
    exit /b 0
)

echo.
echo Invalid choice. Please run the script again.
pause
