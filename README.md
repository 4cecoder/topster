# Topster

> Stream movies and TV shows from your terminal with MCP support

**TL;DR:**
- **Linux/macOS:** `bash install.sh`
- **Windows:** `PowerShell -ExecutionPolicy Bypass -File install.ps1`
- **Then run:** `topster`

---

## Quick Start

### Installation

**Linux / macOS:**
```bash
bash install.sh
```

**Windows:**
```powershell
PowerShell -ExecutionPolicy Bypass -File install.ps1
```

### Usage

After installation, just use the `topster` command:

```bash
# Interactive TUI
topster

# Search
topster "the matrix"

# Trending
topster --trending

# Continue watching
topster --continue

# Download
topster "movie" --download ~/Movies
```

### Uninstallation

**Linux / macOS:**
```bash
bash uninstall.sh
```

**Windows:**
```powershell
PowerShell -ExecutionPolicy Bypass -File uninstall.ps1
```

---

## Requirements

- **Bun** (auto-installed)
- **Media Player:** mpv, VLC, or IINA (auto-detected)
- **OS:** Linux, macOS, Windows 10+

## Features

- 🎬 Stream movies & TV shows
- 🔍 Powerful search
- 📺 Android TV app (in dev)
- 🌐 Web interface
- 📜 Watch history
- ⌨️ Beautiful TUI

## Documentation

- [Full Docs](docs)
- [Web App](docs/WEB_APP.md)
- [Android TV](docs/tv/README.md)
- [CLI Guide](docs/archive/QUICK_START.md)

## Development

```bash
# Install deps
bun install

# Build
bun run build

# Test
bun test

# Type check
bun run typecheck

# Install globally (dev)
bun run install:global
```

---

**Made with 🎬 by [4cecoder](https://github.com/4cecoder)**
