# Topster Installation Script for Windows PowerShell
# Run with: PowerShell -ExecutionPolicy Bypass -File install.ps1

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

Write-Color "🎬 Topster Installation Script" "Cyan"
Write-Color "=============================" "Cyan"
Write-Host ""

# Check for Administrator privileges
$isAdmin = ([Security.Principal.WindowsPrincipal] [Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Color "⚠️  Warning: Not running as Administrator. Some features may require elevation." "Yellow"
}

# Check for Bun
Write-Host ""
Write-Color "🔍 Checking for Bun installation..." "Blue"
$bunPath = Get-Command bun -ErrorAction SilentlyContinue
if (-not $bunPath) {
    Write-Color "📦 Installing Bun..." "Yellow"
    irm bun.sh/install.ps1 | iex
    
    # Refresh environment variables
    $env:Path = [System.Environment]::GetEnvironmentVariable("Path","Machine") + ";" + [System.Environment]::GetEnvironmentVariable("Path","User")
    
    Write-Color "✅ Bun installed successfully!" "Green"
    Write-Color "⚠️  Please restart your terminal to use Bun" "Yellow"
    Write-Color "   Then run this script again." "Yellow"
    exit 0
} else {
    $bunVersion = bun --version 2>&1
    Write-Color "✅ Bun already installed: $bunVersion" "Green"
}

# Check dependencies
Write-Host ""
Write-Color "🔍 Checking system dependencies..." "Blue"

# Check for media player
$defaultPlayer = "mpv"
if (Get-Command mpv -ErrorAction SilentlyContinue) {
    Write-Color "✅ mpv found" "Green"
    $defaultPlayer = "mpv"
} elseif (Get-Command vlc -ErrorAction SilentlyContinue) {
    Write-Color "✅ VLC found" "Green"
    $defaultPlayer = "vlc"
} else {
    Write-Color "⚠️  No media player found. Install mpv or VLC:" "Yellow"
    Write-Color "   winget install VideoLAN.VLC" "Gray"
    Write-Color "   OR" "Gray"
    Write-Color "   Download from: https://mpv.io/" "Gray"
}

# Check for ffmpeg
if (Get-Command ffmpeg -ErrorAction SilentlyContinue) {
    Write-Color "✅ ffmpeg found" "Green"
} else {
    Write-Color "⚠️  ffmpeg not found. Install for subtitle support:" "Yellow"
    Write-Color "   winget install ffmpeg" "Gray"
}

# Install dependencies
Write-Host ""
Write-Color "📦 Installing Topster dependencies..." "Blue"
bun install

# Build project
Write-Host ""
Write-Color "🔨 Building Topster..." "Blue"
bun run build

# Install binary globally
Write-Host ""
Write-Color "📦 Installing Topster binary globally..." "Blue"
bun install --global

# Create config directory
$configDir = "$env:USERPROFILE\.topster"
Write-Host ""
Write-Color "📁 Creating config directory: $configDir" "Blue"
New-Item -ItemType Directory -Force -Path $configDir | Out-Null

# Create default config if it doesn't exist
$configFile = "$configDir\config.json"
if (-not (Test-Path $configFile)) {
    Write-Color "📝 Creating default config..." "Yellow"
    
    # Detect default language
    $defaultLang = "en"
    if ($env:LANG) {
        $defaultLang = ($env:LANG -split '-')[0]
    }
    
    $config = @{
        player = $defaultPlayer
        quality = "auto"
        language = $defaultLang
        subtitles = $true
        historyFile = "$configDir\history.json"
        cacheDir = "$configDir\cache"
        logFile = "$configDir\debug.log"
        mcpServer = @{
            enabled = $true
            port = 3847
        }
    }
    
    $config | ConvertTo-Json -Depth 10 | Out-File -FilePath $configFile -Encoding utf8
    Write-Color "✅ Config created: $configFile" "Green"
} else {
    Write-Color "✅ Config already exists: $configFile" "Green"
}

# Run initial typecheck
Write-Host ""
Write-Color "🔍 Running type check..." "Blue"
bun run typecheck

# Check if topster command is available
$topsterInstalled = $false
try {
    $null = Get-Command topster -ErrorAction Stop
    $topsterInstalled = $true
} catch {
    $topsterInstalled = $false
}

# Summary
Write-Host ""
Write-Color "=======================================" "Cyan"
Write-Color "✅ Topster Installation Complete!" "Green"
Write-Color "=======================================" "Cyan"
Write-Host ""
$installDir = Get-Location | Select-Object -ExpandProperty Path
Write-Color "📁 Installation directory: $installDir" "White"
Write-Color "⚙️  Config file: $configFile" "White"
Write-Color "📜 Log file: $configDir\debug.log" "White"
Write-Host ""
Write-Color "🚀 Quick Start:" "Cyan"
Write-Host ""

if ($topsterInstalled) {
    Write-Color "  # Now you can just run:" "Gray"
    Write-Color "  topster" "Green" -NoNewline
    Write-Host ""
    Write-Host ""
    Write-Color "  # Launch interactive TUI" "Gray"
    Write-Color "  topster" "Green" -NoNewline
    Write-Host ""
    Write-Host ""
    Write-Color "  # Search for a movie" "Gray"
    Write-Color '  topster "the matrix"' "Green" -NoNewline
    Write-Host ""
    Write-Host ""
    Write-Color "  # View trending" "Gray"
    Write-Color "  topster --trending" "Green" -NoNewline
    Write-Host ""
    Write-Host ""
    Write-Color "  # Continue watching" "Gray"
    Write-Color "  topster --continue" "Green" -NoNewline
    Write-Host ""
    Write-Host ""
    Write-Color "  # Download instead of stream" "Gray"
    Write-Color '  topster "movie name" --download ~\Downloads' "Green" -NoNewline
    Write-Host ""
    Write-Host ""
    Write-Color "✅ 'topster' command is now available!" "Green"
} else {
    Write-Color "  # The 'topster' command is installed globally" "Gray"
    Write-Color "  # But you may need to restart your terminal first" "Yellow"
    Write-Host ""
    Write-Color "  # After restart, run:" "Gray"
    Write-Color "  topster" "Green" -NoNewline
    Write-Host ""
}

Write-Host ""
Write-Color "📚 Documentation: https://github.com/4cecoder/topster" "White"
Write-Host ""
