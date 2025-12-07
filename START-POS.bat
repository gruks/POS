@echo off
:: Simple launcher that uses PowerShell to run the application
:: This works because Maven is in PowerShell's PATH

echo Starting POS System...
echo.

powershell -ExecutionPolicy Bypass -File "%~dp0run-pos.ps1"

if errorlevel 1 (
    echo.
    echo Application failed to start.
    pause
)
