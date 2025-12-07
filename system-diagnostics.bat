@echo off
setlocal enabledelayedexpansion

:: ============================================
:: POS System Diagnostics
:: Comprehensive system check and diagnostics
:: ============================================

echo.
echo ================================================
echo   POS System Diagnostics
echo ================================================
echo.
echo Running comprehensive system check...
echo.

:: Create log file
set "LOG_FILE=diagnostics-%date:~-4,4%%date:~-10,2%%date:~-7,2%-%time:~0,2%%time:~3,2%%time:~6,2%.log"
set "LOG_FILE=%LOG_FILE: =0%"

echo Diagnostics Report > "%LOG_FILE%"
echo Generated: %date% %time% >> "%LOG_FILE%"
echo. >> "%LOG_FILE%"

:: ============================================
:: SYSTEM INFORMATION
:: ============================================
echo [1/8] System Information
echo ================================================ >> "%LOG_FILE%"
echo SYSTEM INFORMATION >> "%LOG_FILE%"
echo ================================================ >> "%LOG_FILE%"

echo   OS: %OS%
echo OS: %OS% >> "%LOG_FILE%"

for /f "tokens=2 delims==" %%a in ('wmic os get Caption /value') do set OS_NAME=%%a
echo   OS Name: !OS_NAME!
echo OS Name: !OS_NAME! >> "%LOG_FILE%"

for /f "tokens=2 delims==" %%a in ('wmic os get Version /value') do set OS_VERSION=%%a
echo   OS Version: !OS_VERSION!
echo OS Version: !OS_VERSION! >> "%LOG_FILE%"

for /f "tokens=2 delims==" %%a in ('wmic computersystem get TotalPhysicalMemory /value') do set RAM=%%a
set /a RAM_GB=!RAM:~0,-9!
echo   RAM: !RAM_GB! GB
echo RAM: !RAM_GB! GB >> "%LOG_FILE%"

echo.
echo. >> "%LOG_FILE%"

:: ============================================
:: JAVA CHECK
:: ============================================
echo [2/8] Java JDK
echo ================================================ >> "%LOG_FILE%"
echo JAVA JDK >> "%LOG_FILE%"
echo ================================================ >> "%LOG_FILE%"

java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo   Status: NOT INSTALLED
    echo   Required: Java 17 or higher
    echo Status: NOT INSTALLED >> "%LOG_FILE%"
) else (
    echo   Status: INSTALLED
    echo Status: INSTALLED >> "%LOG_FILE%"
    
    java -version 2>&1 | findstr /i "version" >> "%LOG_FILE%"
    
    for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
        set JAVA_VERSION=%%g
    )
    set JAVA_VERSION=!JAVA_VERSION:"=!
    echo   Version: !JAVA_VERSION!
    
    :: Check JAVA_HOME
    if defined JAVA_HOME (
        echo   JAVA_HOME: !JAVA_HOME!
        echo JAVA_HOME: !JAVA_HOME! >> "%LOG_FILE%"
    ) else (
        echo   JAVA_HOME: NOT SET
        echo JAVA_HOME: NOT SET >> "%LOG_FILE%"
    )
)

echo.
echo. >> "%LOG_FILE%"

:: ============================================
:: MAVEN CHECK
:: ============================================
echo [3/8] Apache Maven
echo ================================================ >> "%LOG_FILE%"
echo APACHE MAVEN >> "%LOG_FILE%"
echo ================================================ >> "%LOG_FILE%"

mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo   Status: NOT INSTALLED
    echo Status: NOT INSTALLED >> "%LOG_FILE%"
) else (
    echo   Status: INSTALLED
    echo Status: INSTALLED >> "%LOG_FILE%"
    
    mvn -version 2>&1 >> "%LOG_FILE%"
    
    for /f "tokens=3" %%g in ('mvn -version 2^>^&1 ^| findstr /i "Apache Maven"') do (
        set MAVEN_VERSION=%%g
    )
    echo   Version: !MAVEN_VERSION!
    
    :: Check M2_HOME
    if defined M2_HOME (
        echo   M2_HOME: !M2_HOME!
        echo M2_HOME: !M2_HOME! >> "%LOG_FILE%"
    )
)

echo.
echo. >> "%LOG_FILE%"

:: ============================================
:: POSTGRESQL CHECK
:: ============================================
echo [4/8] PostgreSQL
echo ================================================ >> "%LOG_FILE%"
echo POSTGRESQL >> "%LOG_FILE%"
echo ================================================ >> "%LOG_FILE%"

psql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo   Status: NOT INSTALLED
    echo Status: NOT INSTALLED >> "%LOG_FILE%"
) else (
    echo   Status: INSTALLED
    echo Status: INSTALLED >> "%LOG_FILE%"
    
    psql --version >> "%LOG_FILE%"
    
    for /f "tokens=3" %%g in ('psql --version') do (
        set PSQL_VERSION=%%g
    )
    echo   Version: !PSQL_VERSION!
    
    :: Check service status
    echo   Checking PostgreSQL services...
    sc query postgresql-x64-16 >nul 2>&1
    if %errorlevel% equ 0 (
        for /f "tokens=4" %%a in ('sc query postgresql-x64-16 ^| findstr "STATE"') do (
            echo   Service (v16): %%a
            echo Service (v16): %%a >> "%LOG_FILE%"
        )
    )
    
    sc query postgresql-x64-15 >nul 2>&1
    if %errorlevel% equ 0 (
        for /f "tokens=4" %%a in ('sc query postgresql-x64-15 ^| findstr "STATE"') do (
            echo   Service (v15): %%a
            echo Service (v15): %%a >> "%LOG_FILE%"
        )
    )
)

echo.
echo. >> "%LOG_FILE%"

:: ============================================
:: DATABASE CONNECTION CHECK
:: ============================================
echo [5/8] Database Connection
echo ================================================ >> "%LOG_FILE%"
echo DATABASE CONNECTION >> "%LOG_FILE%"
echo ================================================ >> "%LOG_FILE%"

if exist ".env" (
    echo   .env file: FOUND
    echo .env file: FOUND >> "%LOG_FILE%"
    
    for /f "tokens=1,2 delims==" %%a in (.env) do (
        if "%%a"=="DB_NAME" set DB_NAME=%%b
        if "%%a"=="DB_USER" set DB_USER=%%b
        if "%%a"=="DB_PASSWORD" set DB_PASSWORD=%%b
        if "%%a"=="DB_HOST" set DB_HOST=%%b
        if "%%a"=="DB_PORT" set DB_PORT=%%b
    )
    
    echo   Host: !DB_HOST!
    echo   Port: !DB_PORT!
    echo   Database: !DB_NAME!
    echo   User: !DB_USER!
    
    echo Host: !DB_HOST! >> "%LOG_FILE%"
    echo Port: !DB_PORT! >> "%LOG_FILE%"
    echo Database: !DB_NAME! >> "%LOG_FILE%"
    echo User: !DB_USER! >> "%LOG_FILE%"
    
    :: Test connection
    set PGPASSWORD=!DB_PASSWORD!
    psql -h !DB_HOST! -p !DB_PORT! -U !DB_USER! -d !DB_NAME! -c "SELECT version();" >nul 2>&1
    if %errorlevel% equ 0 (
        echo   Connection: SUCCESS
        echo Connection: SUCCESS >> "%LOG_FILE%"
    ) else (
        echo   Connection: FAILED
        echo Connection: FAILED >> "%LOG_FILE%"
    )
) else (
    echo   .env file: NOT FOUND
    echo .env file: NOT FOUND >> "%LOG_FILE%"
)

echo.
echo. >> "%LOG_FILE%"

:: ============================================
:: PROJECT FILES CHECK
:: ============================================
echo [6/8] Project Files
echo ================================================ >> "%LOG_FILE%"
echo PROJECT FILES >> "%LOG_FILE%"
echo ================================================ >> "%LOG_FILE%"

if exist "pom.xml" (
    echo   pom.xml: FOUND
    echo pom.xml: FOUND >> "%LOG_FILE%"
) else (
    echo   pom.xml: NOT FOUND
    echo pom.xml: NOT FOUND >> "%LOG_FILE%"
)

if exist "src\main\java\com\example\pos\Main.java" (
    echo   Main.java: FOUND
    echo Main.java: FOUND >> "%LOG_FILE%"
) else (
    echo   Main.java: NOT FOUND
    echo Main.java: NOT FOUND >> "%LOG_FILE%"
)

if exist "src\main\java\module-info.java" (
    echo   module-info.java: FOUND
    echo module-info.java: FOUND >> "%LOG_FILE%"
) else (
    echo   module-info.java: NOT FOUND
    echo module-info.java: NOT FOUND >> "%LOG_FILE%"
)

if exist "target" (
    echo   target folder: EXISTS
    echo target folder: EXISTS >> "%LOG_FILE%"
) else (
    echo   target folder: NOT FOUND (run mvn package)
    echo target folder: NOT FOUND >> "%LOG_FILE%"
)

echo.
echo. >> "%LOG_FILE%"

:: ============================================
:: PRINTER CHECK
:: ============================================
echo [7/8] Printers
echo ================================================ >> "%LOG_FILE%"
echo PRINTERS >> "%LOG_FILE%"
echo ================================================ >> "%LOG_FILE%"

echo   Installed Printers:
echo Installed Printers: >> "%LOG_FILE%"

wmic printer get name 2>nul | findstr /v "Name" | findstr /v "^$" > temp_printers.txt
if exist temp_printers.txt (
    for /f "delims=" %%a in (temp_printers.txt) do (
        echo     - %%a
        echo   - %%a >> "%LOG_FILE%"
    )
    del temp_printers.txt
) else (
    echo     No printers found
    echo   No printers found >> "%LOG_FILE%"
)

echo.
echo. >> "%LOG_FILE%"

:: ============================================
:: NETWORK CHECK
:: ============================================
echo [8/8] Network
echo ================================================ >> "%LOG_FILE%"
echo NETWORK >> "%LOG_FILE%"
echo ================================================ >> "%LOG_FILE%"

echo   Testing localhost connection...
ping -n 1 localhost >nul 2>&1
if %errorlevel% equ 0 (
    echo   localhost: OK
    echo localhost: OK >> "%LOG_FILE%"
) else (
    echo   localhost: FAILED
    echo localhost: FAILED >> "%LOG_FILE%"
)

echo   Testing port 5432 (PostgreSQL)...
netstat -an | findstr ":5432" >nul 2>&1
if %errorlevel% equ 0 (
    echo   Port 5432: LISTENING
    echo Port 5432: LISTENING >> "%LOG_FILE%"
) else (
    echo   Port 5432: NOT LISTENING
    echo Port 5432: NOT LISTENING >> "%LOG_FILE%"
)

echo.
echo. >> "%LOG_FILE%"

:: ============================================
:: SUMMARY
:: ============================================
echo ================================================
echo   Diagnostics Complete
echo ================================================
echo.
echo Report saved to: %LOG_FILE%
echo.

echo ================================================ >> "%LOG_FILE%"
echo RECOMMENDATIONS >> "%LOG_FILE%"
echo ================================================ >> "%LOG_FILE%"

:: Generate recommendations
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo - Install Java 17 or higher >> "%LOG_FILE%"
)

mvn -version >nul 2>&1
if %errorlevel% neq 0 (
    echo - Install Apache Maven >> "%LOG_FILE%"
)

psql --version >nul 2>&1
if %errorlevel% neq 0 (
    echo - Install PostgreSQL >> "%LOG_FILE%"
)

if not exist ".env" (
    echo - Create .env file with database configuration >> "%LOG_FILE%"
)

if not exist "target" (
    echo - Run: mvn clean package >> "%LOG_FILE%"
)

echo.
echo To view full report, open: %LOG_FILE%
echo.
pause
