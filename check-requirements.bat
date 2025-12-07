@echo off
setlocal

echo.
echo ========================================
echo   POS System Requirements Check
echo ========================================
echo.

set "ALL_OK=1"

:: CHECK JAVA
echo [1/3] Checking Java JDK 17+...
where java >nul 2>&1
if errorlevel 1 (
    echo [X] Java is NOT installed or not in PATH
    echo     Download from: https://adoptium.net/
    set "ALL_OK=0"
) else (
    java -version 2>&1 | findstr "version" >nul
    if errorlevel 1 (
        echo [X] Java command failed
        set "ALL_OK=0"
    ) else (
        echo [OK] Java is installed
        java -version 2>&1 | findstr "version"
    )
)
echo.

:: CHECK MAVEN
echo [2/3] Checking Apache Maven...
where mvn >nul 2>&1
if errorlevel 1 (
    echo [X] Maven is NOT installed or not in PATH
    echo     Download from: https://maven.apache.org/download.cgi
    echo     Or run: setup-environment.bat (as Administrator)
    echo.
    echo     Maven should be at: C:\Program Files\Apache\Maven\...\bin
    echo     Add to PATH if installed but not found
    set "ALL_OK=0"
) else (
    mvn -version >nul 2>&1
    if errorlevel 1 (
        echo [X] Maven command failed
        set "ALL_OK=0"
    ) else (
        echo [OK] Maven is installed
        mvn -version 2>&1 | findstr "Apache Maven"
    )
)
echo.

:: CHECK POSTGRESQL
echo [3/3] Checking PostgreSQL...
where psql >nul 2>&1
if errorlevel 1 (
    echo [X] PostgreSQL is NOT installed or not in PATH
    echo     Download from: https://www.postgresql.org/download/windows/
    echo     Or run: setup-environment.bat (as Administrator)
    set "ALL_OK=0"
) else (
    psql --version >nul 2>&1
    if errorlevel 1 (
        echo [X] PostgreSQL command failed
        set "ALL_OK=0"
    ) else (
        echo [OK] PostgreSQL is installed
        psql --version
    )
)
echo.

:: CHECK DATABASE CONNECTION
echo [4/4] Checking Database Configuration...
if exist ".env" (
    echo [OK] .env file found
    echo.
    echo Database Configuration:
    type .env | findstr "DB_"
    echo.
) else (
    echo [X] .env file not found
    echo     Please create .env file with database configuration
    set "ALL_OK=0"
)
echo.

:: SUMMARY
echo ========================================
echo   Summary
echo ========================================
echo.

if "%ALL_OK%"=="1" (
    echo [SUCCESS] All requirements are met!
    echo.
    echo You can now run the POS system:
    echo   run-pos.bat
    echo.
) else (
    echo [FAILED] Some requirements are missing.
    echo.
    echo To fix:
    echo   1. Run: setup-environment.bat (as Administrator)
    echo   2. Or install manually from the links above
    echo   3. Make sure to add to PATH after installation
    echo.
    echo To add Maven to PATH manually:
    echo   1. Search "Environment Variables" in Windows
    echo   2. Edit "Path" variable
    echo   3. Add: C:\Program Files\Apache\Maven\...\bin
    echo   4. Restart Command Prompt
    echo.
)

pause
