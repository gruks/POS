@echo off
setlocal

title Restaurant POS System

echo.
echo ========================================
echo   Restaurant POS System
echo ========================================
echo.

:: Try to find Maven
set "MVN_CMD=mvn"

where mvn >nul 2>&1
if errorlevel 1 (
    echo Maven not found in PATH. Searching for Maven...
    
    :: Check common Maven locations
    if exist "C:\Program Files\Apache\Maven" (
        for /d %%D in ("C:\Program Files\Apache\Maven\*") do (
            if exist "%%D\bin\mvn.cmd" (
                set "MVN_CMD=%%D\bin\mvn.cmd"
                echo Found Maven at: %%D\bin
                goto :maven_found
            )
        )
    )
    
    echo.
    echo [ERROR] Maven not found!
    echo.
    echo Please do one of the following:
    echo   1. Run: fix-maven-path.bat (to add Maven to PATH)
    echo   2. Run: setup-environment.bat (as Administrator to install Maven)
    echo.
    pause
    exit /b 1
)

:maven_found

echo Using Maven: %MVN_CMD%
echo.

:: Check Java
where java >nul 2>&1
if errorlevel 1 (
    echo [ERROR] Java not found!
    echo Please install Java 17 or run: setup-environment.bat
    pause
    exit /b 1
)

:: Check PostgreSQL
where psql >nul 2>&1
if errorlevel 1 (
    echo [WARNING] PostgreSQL not found in PATH
    echo Make sure PostgreSQL is installed and running
)

:: Check .env
if not exist ".env" (
    echo [ERROR] .env file not found!
    echo Please create .env file with database configuration
    pause
    exit /b 1
)

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
"%MVN_CMD%" javafx:run

if errorlevel 1 (
    echo.
    echo ========================================
    echo   Application Error
    echo ========================================
    echo.
    echo The application encountered an error.
    echo.
    echo Common issues:
    echo   1. Database not running - Start PostgreSQL service
    echo   2. Wrong credentials - Check .env file
    echo   3. Build errors - Run: mvn clean package
    echo.
    pause
)
