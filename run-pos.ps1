# Restaurant POS System Launcher (PowerShell)

Write-Host ""
Write-Host "========================================"
Write-Host "  Restaurant POS System"
Write-Host "========================================"
Write-Host ""

$canRun = $true

# Check Java
Write-Host "Checking Java..."
try {
    $javaVersion = java -version 2>&1 | Select-String "version"
    Write-Host "[OK] Java found" -ForegroundColor Green
    Write-Host $javaVersion
} catch {
    Write-Host "[ERROR] Java is not installed" -ForegroundColor Red
    $canRun = $false
}

# Check Maven
Write-Host "`nChecking Maven..."
try {
    $mavenVersion = mvn -version 2>&1 | Select-String "Apache Maven"
    Write-Host "[OK] Maven found" -ForegroundColor Green
    Write-Host $mavenVersion
} catch {
    Write-Host "[ERROR] Maven is not installed" -ForegroundColor Red
    $canRun = $false
}

# Check PostgreSQL
Write-Host "`nChecking PostgreSQL..."
try {
    $psqlVersion = psql --version
    Write-Host "[OK] PostgreSQL found" -ForegroundColor Green
    Write-Host $psqlVersion
} catch {
    Write-Host "[ERROR] PostgreSQL is not installed" -ForegroundColor Red
    $canRun = $false
}

# Check .env file
Write-Host "`nChecking configuration..."
if (Test-Path ".env") {
    Write-Host "[OK] Configuration found" -ForegroundColor Green
} else {
    Write-Host "[ERROR] .env file not found" -ForegroundColor Red
    $canRun = $false
}

if (-not $canRun) {
    Write-Host ""
    Write-Host "========================================"
    Write-Host "  Cannot start application"
    Write-Host "========================================"
    Write-Host ""
    Write-Host "Please fix the errors above and try again."
    Write-Host ""
    Write-Host "For help, run: .\check-requirements.bat"
    Write-Host ""
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "[OK] All requirements met" -ForegroundColor Green
Write-Host ""

# Check PostgreSQL service
Write-Host "Checking PostgreSQL service..."
$pgService = Get-Service -Name "postgresql-x64-*" -ErrorAction SilentlyContinue | Where-Object {$_.Status -eq "Running"} | Select-Object -First 1
if ($pgService) {
    Write-Host "[OK] PostgreSQL service is running" -ForegroundColor Green
} else {
    Write-Host "[WARNING] PostgreSQL service not running" -ForegroundColor Yellow
    Write-Host "Attempting to start PostgreSQL..."
    try {
        Start-Service -Name "postgresql-x64-16" -ErrorAction Stop
        Write-Host "[OK] PostgreSQL service started" -ForegroundColor Green
    } catch {
        Write-Host "[WARNING] Could not start PostgreSQL automatically" -ForegroundColor Yellow
        Write-Host "Please start it manually"
    }
}

Write-Host ""
Write-Host "Starting POS System..."
Write-Host ""
Write-Host "========================================"
Write-Host "  Application Running"
Write-Host "========================================"
Write-Host ""
Write-Host "The application window should open shortly..."
Write-Host "Press Ctrl+C to stop the application"
Write-Host ""

# Run the application
mvn javafx:run

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "========================================"
    Write-Host "  Application Error"
    Write-Host "========================================"
    Write-Host ""
    Write-Host "The application encountered an error."
    Write-Host ""
    Write-Host "Common issues:"
    Write-Host "  1. Database not running - Start PostgreSQL service"
    Write-Host "  2. Wrong credentials - Check .env file"
    Write-Host "  3. Build errors - Run: mvn clean package"
    Write-Host ""
    Read-Host "Press Enter to exit"
}
