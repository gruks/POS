@echo off
setlocal enabledelayedexpansion

title Restaurant POS System

echo.
echo ========================================
echo   Restaurant POS System
echo ========================================
echo.

set "CAN_RUN=1"

:: Check Java
echo Checking Java...
java -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java is not installed
    echo Please run: setup-environment.bat
    set "CAN_RUN=0"
) else (
    echo [OK] Java found
)

:: Check Maven
echo Checking Maven...
mvn -version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Maven is not installed
    echo Please run: setup-environment.bat
    set "CAN_RUN=0"
) else (
    echo [OK] Maven found
)

:: Check PostgreSQL
echo Checking PostgreSQL...
psql --version >nul 2>&1
if errorlevel 1 (
    echo [ERROR] PostgreSQL is not installed
    echo Please run: setup-environment.bat
    set "CAN_RUN=0"
) else (
    echo [OK] PostgreSQL found
)

:: Check .env file
echo Checking configuration...
if not exist ".env" (
    echo [ERROR] .env file not found
    echo Please create .env file with database configuration
    set "CAN_RUN=0"
) else (
    echo [OK] Configuration found
)

if "%CAN_RUN%"=="0" (
    echo.
    echo ========================================
    echo   Cannot start application
    echo ========================================
    echo.
    echo Please fix the errors above and try again.
    echo.
    echo For help, run: check-requirements.bat
    echo.
    pause
    exit /b 1
)

echo.
echo [OK] All requirements met
echo.

:: Check if PostgreSQL is running
echo Checking PostgreSQL service...
sc query postgresql-x64-16 | findstr "RUNNING" >nul 2>&1
if errorlevel 1 (
    echo PostgreSQL service not running. Attempting to start...
    net start postgresql-x64-16 >nul 2>&1
    if errorlevel 1 (
        echo [WARNING] Could not start PostgreSQL automatically
        echo Please start it manually: net start postgresql-x64-16
        pause
    ) else (
        echo [OK] PostgreSQL service started
    )
) else (
    echo [OK] PostgreSQL service is running
)

echo.
echo Starting POS System...
echo.
echo ========================================
echo   Application Running
echo ========================================
echo.
echo The application window should open shortly...
echo Press Ctrl+C to stop the application
echo.

:: Run the application
mvn javafx:run

if errorlevel 1 (
    echo.
    echo ========================================
    echo   Application Error
    echo ========================================
    echo.
    echo The application encountered an error.
    echo Please check the error messages above.
    echo.
    echo Common issues:
    echo   1. Database not running - Start PostgreSQL service
    echo   2. Wrong credentials - Check .env file
    echo   3. Build errors - Run: mvn clean package
    echo.
    echo For diagnostics, run: system-diagnostics.bat
    echo.
    pause
)
