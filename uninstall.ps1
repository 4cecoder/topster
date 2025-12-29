# Topster Uninstallation Script for Windows PowerShell
# Run with: PowerShell -ExecutionPolicy Bypass -File uninstall.ps1

$ErrorActionPreference = "Stop"

function Write-Color($Message, $Color) {
    switch ($Color) {
        "Red" { Write-Host $Message -ForegroundColor Red }
        "Green" { Write-Host $Message -ForegroundColor Green }
        "Yellow" { Write-Host $Message -ForegroundColor Yellow }
        "Blue" { Write-Host $Message -ForegroundColor Blue }
        "Cyan" { Write-Host $Message -ForegroundColor Cyan }
        "Gray" { Write-Host $Message -ForegroundColor Gray }
        default { Write-Host $Message }
    }
}

Write-Color "🗑️  Topster Uninstallation" "Red"
Write-Color "========================" "Red"
Write-Host ""

# Remove binary
$bunBin = "$env:USERPROFILE\.bun\bin\topster.ps1"
if (Test-Path $bunBin) {
    Write-Color "🗑️  Removing binary: $bunBin" "Yellow"
    Remove-Item $bunBin -Force
    Write-Color "✅ Binary removed" "Green"
} else {
    Write-Color "⚠️  Binary not found: $bunBin" "Yellow"
}

# Remove config directory (ask user first)
$configDir = "$env:USERPROFILE\.topster"
if (Test-Path $configDir) {
    Write-Host ""
    Write-Color "⚠️  Remove config directory and all history?" "Yellow"
    Write-Color "   $configDir" "Red"
    $confirm = Read-Host "   Type 'yes' to confirm"
    Write-Host ""
    
    if ($confirm -eq "yes") {
        Remove-Item $configDir -Recurse -Force
        Write-Color "✅ Config directory removed" "Green"
    } else {
        Write-Color "⏭️  Config directory kept" "Yellow"
    }
}

# Summary
Write-Host ""
Write-Color "=======================================" "Cyan"
Write-Color "✅ Topster Uninstallation Complete!" "Green"
Write-Color "=======================================" "Cyan"
Write-Host ""
Write-Color "⚠️  Note: You may need to restart your terminal" "Yellow"
Write-Host ""
