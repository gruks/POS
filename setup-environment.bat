@echo off
setlocal enabledelayedexpansion

:: ============================================
:: POS System Environment Setup Script
:: Checks and installs: Java, Maven, PostgreSQL
:: ============================================

echo.
echo ========================================
echo   POS System Environment Setup
echo ========================================
echo.

:: Set colors for output
set "GREEN=[92m"
set "RED=[91m"
set "YELLOW=[93m"
set "BLUE=[94m"
set "RESET=[0m"

:: Track if any installations are needed
set "NEEDS_INSTALL=0"

:: ============================================
:: 1. CHECK JAVA
:: ============================================
echo %BLUE%[1/3] Checking Java JDK 17+...%RESET%
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%[X] Java is NOT installed%RESET%
    set "NEEDS_INSTALL=1"
    set "JAVA_MISSING=1"
) else (
    for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set JAVA_VERSION=%%g
    )
    set JAVA_VERSION=!JAVA_VERSION:"=!
    echo %GREEN%[OK] Java found: !JAVA_VERSION!%RESET%
    
    :: Check if version is 17 or higher
    for /f "tokens=1,2 delims=." %%a in ("!JAVA_VERSION!") do (
        set JAVA_MAJOR=%%a
        if %%a equ 1 set JAVA_MAJOR=%%b
    )
    if !JAVA_MAJOR! lss 17 (
        echo %YELLOW%[!] Java version is less than 17. Java 17+ is required.%RESET%
        set "NEEDS_INSTALL=1"
        set "JAVA_MISSING=1"
    )
)
echo.

:: ============================================
:: 2. CHECK MAVEN
:: ============================================
echo %BLUE%[2/3] Checking Apache Maven...%RESET%
mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%[X] Maven is NOT installed%RESET%
    set "NEEDS_INSTALL=1"
    set "MAVEN_MISSING=1"
) else (
    for /f "tokens=3" %%g in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do (
        set MAVEN_VERSION=%%g
    )
    echo %GREEN%[OK] Maven found: !MAVEN_VERSION!%RESET%
)
echo.

:: ============================================
:: 3. CHECK POSTGRESQL
:: ============================================
echo %BLUE%[3/3] Checking PostgreSQL...%RESET%
psql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo %RED%[X] PostgreSQL is NOT installed%RESET%
    set "NEEDS_INSTALL=1"
    set "PSQL_MISSING=1"
) else (
    for /f "tokens=3" %%g in ('psql --version') do (
        set PSQL_VERSION=%%g
    )
    echo %GREEN%[OK] PostgreSQL found: !PSQL_VERSION!%RESET%
    
    :: Check if PostgreSQL service is running
    sc query postgresql-x64-16 >nul 2>&1
    if %errorlevel% equ 0 (
        echo %GREEN%[OK] PostgreSQL service is installed%RESET%
    ) else (
        sc query postgresql-x64-15 >nul 2>&1
        if %errorlevel% equ 0 (
            echo %GREEN%[OK] PostgreSQL service is installed%RESET%
        ) else (
            echo %YELLOW%[!] PostgreSQL is installed but service not found%RESET%
        )
    )
)
echo.

:: ============================================
:: SUMMARY
:: ============================================
echo ========================================
echo   Summary
echo ========================================
echo.

if %NEEDS_INSTALL% equ 0 (
    echo %GREEN%[SUCCESS] All requirements are installed!%RESET%
    echo.
    echo You can now run the POS system:
    echo   1. Start PostgreSQL service
    echo   2. Run: mvn clean javafx:run
    echo.
    pause
    exit /b 0
)

:: ============================================
:: INSTALLATION SECTION
:: ============================================
echo %YELLOW%Some requirements are missing. Installation required.%RESET%
echo.
echo This script will guide you through installing:
if defined JAVA_MISSING echo   - Java JDK 17
if defined MAVEN_MISSING echo   - Apache Maven
if defined PSQL_MISSING echo   - PostgreSQL
echo.
echo %YELLOW%Note: This requires administrator privileges.%RESET%
echo.
set /p CONFIRM="Do you want to proceed with installation? (Y/N): "
if /i not "%CONFIRM%"=="Y" (
    echo Installation cancelled.
    pause
    exit /b 1
)

:: Check for admin rights
net session >nul 2>&1
if %errorlevel% neq 0 (
    echo.
    echo %RED%[ERROR] Administrator privileges required!%RESET%
    echo Please run this script as Administrator.
    echo.
    echo Right-click on this file and select "Run as administrator"
    pause
    exit /b 1
)

:: ============================================
:: INSTALL CHOCOLATEY (Package Manager)
:: ============================================
echo.
echo %BLUE%Installing Chocolatey package manager...%RESET%
where choco >nul 2>&1
if %errorlevel% neq 0 (
    echo Installing Chocolatey...
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Set-ExecutionPolicy Bypass -Scope Process -Force; [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))"
    
    if %errorlevel% neq 0 (
        echo %RED%[ERROR] Failed to install Chocolatey%RESET%
        echo Please install manually from: https://chocolatey.org/install
        pause
        exit /b 1
    )
    
    :: Refresh environment variables
    call refreshenv
    echo %GREEN%[OK] Chocolatey installed%RESET%
) else (
    echo %GREEN%[OK] Chocolatey already installed%RESET%
)
echo.

:: ============================================
:: INSTALL JAVA
:: ============================================
if defined JAVA_MISSING (
    echo %BLUE%Installing Java JDK 17...%RESET%
    choco install openjdk17 -y
    if %errorlevel% neq 0 (
        echo %RED%[ERROR] Failed to install Java%RESET%
        pause
        exit /b 1
    )
    call refreshenv
    echo %GREEN%[OK] Java JDK 17 installed%RESET%
    echo.
)

:: ============================================
:: INSTALL MAVEN
:: ============================================
if defined MAVEN_MISSING (
    echo %BLUE%Installing Apache Maven...%RESET%
    choco install maven -y
    if %errorlevel% neq 0 (
        echo %RED%[ERROR] Failed to install Maven%RESET%
        pause
        exit /b 1
    )
    call refreshenv
    echo %GREEN%[OK] Maven installed%RESET%
    echo.
)

:: ============================================
:: INSTALL POSTGRESQL
:: ============================================
if defined PSQL_MISSING (
    echo %BLUE%Installing PostgreSQL...%RESET%
    echo.
    echo %YELLOW%PostgreSQL will be installed with default settings:%RESET%
    echo   - Port: 5432
    echo   - Superuser: postgres
    echo   - You will need to set a password
    echo.
    
    choco install postgresql16 --params '/Password:postgres' -y
    if %errorlevel% neq 0 (
        echo %RED%[ERROR] Failed to install PostgreSQL%RESET%
        pause
        exit /b 1
    )
    call refreshenv
    echo %GREEN%[OK] PostgreSQL installed%RESET%
    echo.
    
    :: Start PostgreSQL service
    echo Starting PostgreSQL service...
    net start postgresql-x64-16
    echo.
)

:: ============================================
:: VERIFY INSTALLATIONS
:: ============================================
echo.
echo ========================================
echo   Verifying Installations
echo ========================================
echo.

java -version
echo.
mvn -version
echo.
psql --version
echo.

:: ============================================
:: SETUP DATABASE
:: ============================================
echo ========================================
echo   Database Setup
echo ========================================
echo.
echo %YELLOW%Would you like to setup the POS database now?%RESET%
set /p SETUP_DB="Setup database? (Y/N): "
if /i "%SETUP_DB%"=="Y" (
    echo.
    echo Setting up database...
    echo.
    
    :: Check if .env file exists
    if exist ".env" (
        echo Reading database configuration from .env file...
        for /f "tokens=1,2 delims==" %%a in (.env) do (
            if "%%a"=="DB_NAME" set DB_NAME=%%b
            if "%%a"=="DB_USER" set DB_USER=%%b
            if "%%a"=="DB_PASSWORD" set DB_PASSWORD=%%b
        )
    ) else (
        echo .env file not found. Using defaults...
        set DB_NAME=pos_db
        set DB_USER=pos_user
        set DB_PASSWORD=pos_password
    )
    
    echo.
    echo Database Configuration:
    echo   Database: !DB_NAME!
    echo   User: !DB_USER!
    echo   Password: !DB_PASSWORD!
    echo.
    
    :: Create database and user
    echo Creating database and user...
    set PGPASSWORD=postgres
    psql -U postgres -c "CREATE DATABASE !DB_NAME!;" 2>nul
    psql -U postgres -c "CREATE USER !DB_USER! WITH PASSWORD '!DB_PASSWORD!';" 2>nul
    psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE !DB_NAME! TO !DB_USER!;" 2>nul
    psql -U postgres -c "ALTER DATABASE !DB_NAME! OWNER TO !DB_USER!;" 2>nul
    
    echo %GREEN%[OK] Database setup complete%RESET%
    echo.
    
    :: Run setup SQL if exists
    if exist "setup-local-database.sql" (
        echo Running database initialization script...
        set PGPASSWORD=!DB_PASSWORD!
        psql -U !DB_USER! -d !DB_NAME! -f setup-local-database.sql
        echo %GREEN%[OK] Database initialized%RESET%
    )
)

:: ============================================
:: FINAL INSTRUCTIONS
:: ============================================
echo.
echo ========================================
echo   Installation Complete!
echo ========================================
echo.
echo %GREEN%All requirements have been installed successfully!%RESET%
echo.
echo %YELLOW%Next Steps:%RESET%
echo   1. Close this window and open a NEW command prompt
echo      (to load updated environment variables)
echo.
echo   2. Navigate to the project directory:
echo      cd %~dp0
echo.
echo   3. Build the project:
echo      mvn clean package
echo.
echo   4. Run the application:
echo      mvn javafx:run
echo.
echo %YELLOW%Database Connection:%RESET%
echo   Host: localhost
echo   Port: 5432
echo   Database: !DB_NAME!
echo   Username: !DB_USER!
echo   Password: !DB_PASSWORD!
echo.
echo %YELLOW%PostgreSQL Default Password:%RESET%
echo   Username: postgres
echo   Password: postgres
echo.
echo For more information, see README.md
echo.
pause
