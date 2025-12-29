# Topster Suite - Complete Overview

A comprehensive media streaming solution with CLI, Web, and TV app support, all powered by **Bun**.

## 🎯 What is Topster?

Topster is a full-featured media streaming suite that lets you:
- Stream movies and TV shows from the command line
- Browse and watch content in your browser
- Cast to TV apps on your local network
- Cast your entire screen to DLNA devices/projectors

## 📦 Components

### 1. **CLI Tool** (Bun + TypeScript)
**Location**: `/` (root directory)

**Features:**
- Stream movies/TV shows via FlixHQ
- Play with mpv, VLC, or IINA
- Watch history with resume
- IMDb integration with ratings
- Discord Rich Presence
- Video extractors (MegaCloud, VidCloud, RapidCloud, StreamSB)
- MCP server for API access

**Run:**
```bash
bun run index.ts
```

### 2. **Web App** (Next.js 16 + Bun)
**Location**: `/site/`

**Features:**
- Browse and search movies/TV shows
- HLS video player with subtitles
- Watch history dashboard
- Settings management
- TV app discovery and casting
- Responsive UI with Tailwind CSS

**Run:**
```bash
./start-web.sh
# or
cd site && bun run dev
```

**Tech Stack:**
- Next.js 16 (App Router)
- TypeScript
- Tailwind CSS v4
- Radix UI
- HLS.js
- **Powered by Bun** (`bun --bun next`)

### 3. **Screen Casting** (Bun + FFmpeg)
**Location**: `/cast-screen.ts`

**Features:**
- Cast Gentoo desktop to DLNA devices
- Stream to projectors wirelessly
- Customizable quality/FPS
- Audio support
- Device discovery

**Run:**
```bash
# Discover devices
bun cast-screen.ts --discover

# Cast to device
bun cast-screen.ts -d http://192.168.1.100:8080
```

### 4. **TV App** (Kotlin - To Be Built)
**Location**: `/tv/` (ready for development)

**Requirements:**
- Android TV / Google TV
- HTTP server on port 8765
- Endpoints: `/ping`, `/command`
- Registration with web app
- Heartbeat every 60s

**Protocol Docs**: See `site/README.md` TV App Integration section

## 🚀 Quick Start

### All-in-One Startup

```bash
# Start MCP server + Web app with Bun
./start-web.sh
```

This starts:
1. MCP server on port 3847
2. Web app on port 3000

### Individual Components

```bash
# CLI only
bun run index.ts

# MCP server only
bun run index.ts --mcp

# Web app only
cd site && bun run dev

# Screen casting
bun cast-screen.ts --discover
```

## 🏗️ Architecture

```
┌─────────────────────────────────────────────┐
│                                             │
│  User Interface Layer                       │
│  ├─ CLI (Ink/React Terminal)                │
│  ├─ Web App (Next.js + React)               │
│  └─ TV App (Kotlin/Android)                 │
│                                             │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│                                             │
│  API Layer (MCP Server)                     │
│  └─ Elysia.js HTTP Server (port 3847)       │
│     ├─ JSON-RPC over HTTP                   │
│     └─ Server-Sent Events (SSE)             │
│                                             │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│                                             │
│  Core Business Logic                        │
│  ├─ FlixHQ Scraper                          │
│  ├─ Video Extractors (MegaCloud, etc.)      │
│  ├─ SQLite Watch History                    │
│  ├─ IMDb Integration (OMDb API)             │
│  ├─ Configuration Management                │
│  └─ Player Integration (mpv/VLC/IINA)       │
│                                             │
└──────────────────┬──────────────────────────┘
                   │
┌──────────────────▼──────────────────────────┐
│                                             │
│  External Services                          │
│  ├─ FlixHQ (Content Provider)               │
│  ├─ Video CDNs (Streaming Sources)          │
│  ├─ OMDb API (IMDb Data)                    │
│  └─ Discord RPC                             │
│                                             │
└─────────────────────────────────────────────┘

Additional Components:

┌─────────────────────────────────────────────┐
│  Screen Casting                             │
│  └─ FFmpeg → DLNA/Projector                 │
└─────────────────────────────────────────────┘

┌─────────────────────────────────────────────┐
│  LAN Communication                          │
│  ├─ Web App ↔ TV App (HTTP REST)            │
│  └─ Device Discovery (Heartbeat)            │
└─────────────────────────────────────────────┘
```

## 📂 Project Structure

```
topster/
├── index.ts                    # CLI entry point
├── start-web.sh               # Start MCP + Web app
├── cast-screen.ts             # Screen casting tool
│
├── src/                       # CLI source code
│   ├── cli/                   # Ink components
│   ├── core/                  # Core types & config
│   ├── modules/
│   │   ├── scraper/          # FlixHQ scraper
│   │   ├── player/           # Media player
│   │   ├── history/          # SQLite history
│   │   ├── imdb/             # IMDb integration
│   │   └── discord/          # Rich presence
│   └── mcp/                  # MCP server
│
├── site/                      # Web app (Next.js + Bun)
│   ├── app/                  # Next.js pages
│   │   ├── page.tsx          # Home/Browse
│   │   ├── watch/[id]/       # Video player
│   │   ├── history/          # Watch history
│   │   ├── settings/         # Settings
│   │   └── api/              # API routes
│   │       ├── tv/           # TV device APIs
│   │       └── proxy/        # CORS proxy
│   ├── components/           # React components
│   ├── lib/                  # Utilities
│   │   ├── types.ts         # Shared types
│   │   ├── mcp-client.ts    # MCP client
│   │   ├── lan-discovery.ts # TV discovery
│   │   └── actions/         # Server actions
│   ├── bunfig.toml          # Bun configuration
│   ├── bun-server.ts        # Bun server
│   └── package.json         # Bun scripts
│
├── tv/                       # TV app (Kotlin - TBD)
│
└── docs/
    ├── WEB_APP.md           # Web app architecture
    └── SCREEN_CASTING.md   # Casting guide
```

## 🔌 Integration Points

### CLI ↔ Web App
- **Protocol**: MCP (Model Context Protocol) over HTTP
- **Transport**: JSON-RPC
- **Port**: 3847
- **Tools**: search, trending, video sources, history, config

### Web App ↔ TV App
- **Protocol**: HTTP REST
- **Registration**: `POST /api/tv/devices`
- **Heartbeat**: `POST /api/tv/heartbeat` (every 60s)
- **Commands**: `POST http://<tv-ip>:<tv-port>/command`

### Screen Casting
- **Method**: FFmpeg streaming
- **Protocol**: HTTP MPEG-TS or DLNA
- **Port**: Configurable (default: 8080)

## 🛠️ Technologies

### Runtime
- **Bun** - Primary runtime for everything
  - CLI: `bun run index.ts`
  - Web: `bun --bun next dev`
  - Casting: `bun cast-screen.ts`

### Languages
- **TypeScript** - All JavaScript/TS code
- **Kotlin** - TV app (to be built)

### Frameworks
- **Ink** - React for terminal (CLI)
- **Next.js 16** - React framework (Web)
- **Elysia.js** - HTTP server (MCP)
- **Drizzle ORM** - Database (History)

### UI Libraries
- **Tailwind CSS v4** - Styling
- **Radix UI** - Accessible components
- **Lucide React** - Icons
- **HLS.js** - Video player

### Tools
- **FFmpeg** - Screen capture/streaming
- **Cheerio** - HTML parsing
- **SQLite** - Watch history

## 📱 Platform Support

### CLI
- ✅ Linux (Gentoo, Ubuntu, Arch, etc.)
- ✅ macOS
- ✅ Windows (WSL recommended)

### Web App
- ✅ All modern browsers
- ✅ Desktop & Mobile
- ✅ PWA-ready (future)

### Screen Casting
- ✅ Linux (X11)
- ⚠️  Wayland (limited support)
- ❌ Windows/macOS (not implemented)

### TV App
- 🔜 Android TV
- 🔜 Google TV
- 🔜 Fire TV (potential)

## 🎬 Workflows

### Workflow 1: CLI Streaming
```bash
bun run index.ts
# 1. Search for content
# 2. Select movie/episode
# 3. Choose quality
# 4. Play in mpv/VLC
# 5. Progress saved to history
```

### Workflow 2: Web Streaming
```bash
./start-web.sh
# 1. Browse trending in browser
# 2. Search for content
# 3. Click to watch
# 4. Play in HLS player
# 5. Progress auto-saved
```

### Workflow 3: Cast to TV
```bash
./start-web.sh
# Web app discovers TV apps
# 1. Browse content in browser
# 2. Click "Cast to TV"
# 3. Select TV device
# 4. Video plays on TV
```

### Workflow 4: Screen Mirroring
```bash
bun cast-screen.ts --discover
bun cast-screen.ts -d http://192.168.1.100:8080
# Your entire Gentoo desktop streams to DLNA device
```

## 🔧 Configuration

### CLI Config
**Location**: `~/.config/topster/config.json`

```json
{
  "baseUrl": "https://flixhq.to",
  "provider": "Vidcloud",
  "player": "mpv",
  "quality": "1080",
  "historyEnabled": true,
  "mcpPort": 3847,
  "omdbApiKey": "your-key"
}
```

### Web App Config
**Location**: `site/.env.local`

```env
NEXT_PUBLIC_MCP_HOST=localhost
NEXT_PUBLIC_MCP_PORT=3847
```

## 📊 Features Comparison

| Feature | CLI | Web | TV App |
|---------|-----|-----|--------|
| Search & Browse | ✅ | ✅ | 🔜 |
| Video Playback | ✅ | ✅ | 🔜 |
| Watch History | ✅ | ✅ | 🔜 |
| Subtitles | ✅ | ✅ | 🔜 |
| Quality Selection | ✅ | ✅ | 🔜 |
| IMDb Ratings | ✅ | ✅ | 🔜 |
| Download | ✅ | ❌ | ❌ |
| Discord RPC | ✅ | ❌ | ❌ |
| Remote Control | ❌ | ✅ | ✅ |
| Screen Casting | ❌ | ❌ | ❌ |

Screen casting is a separate utility.

## 🚢 Deployment

### Development
```bash
# Start everything
./start-web.sh
```

### Production

**Web App:**
```bash
cd site
bun run build
bun run start
```

**Systemd Service:**
```bash
# Create services for MCP and Web
sudo systemctl enable topster-mcp
sudo systemctl enable topster-web
```

### Hosting Options
- Self-hosted (Systemd)
- PM2 process manager
- Reverse proxy (Nginx/Caddy)
- No Docker needed (runs natively with Bun)

## 📚 Documentation

- **`README.md`** - Main project readme
- **`site/README.md`** - Web app setup guide
- **`docs/WEB_APP.md`** - Web app architecture
- **`SCREEN_CASTING.md`** - Screen casting guide
- **`SUITE_OVERVIEW.md`** - This file

## 🎯 Future Enhancements

### Short Term
- [ ] Build Kotlin TV app
- [ ] PWA support for web app
- [ ] Offline mode
- [ ] Watchlist feature
- [ ] Recommendations

### Medium Term
- [ ] User accounts & sync
- [ ] Chromecast integration
- [ ] AirPlay support
- [ ] Download manager in web
- [ ] Multi-language support

### Long Term
- [ ] Mobile apps (React Native)
- [ ] Smart TV apps (Tizen, webOS)
- [ ] P2P streaming
- [ ] Content caching
- [ ] Community features

## 🐛 Troubleshooting

### MCP Server Won't Start
```bash
# Check if port is in use
lsof -i :3847

# Kill existing process
kill $(lsof -t -i :3847)

# Restart
bun run index.ts --mcp
```

### Web App Build Fails
```bash
cd site
rm -rf .next node_modules
bun install
bun run build
```

### Screen Casting No Audio
```bash
# List audio sources
pactl list sources short

# Update cast-screen.ts with correct source
```

### TV App Not Detected
- Check same network
- Verify TV app is sending heartbeats
- Check firewall rules
- Test `/api/tv/devices` endpoint

## 📄 License

Same as parent Topster project.

## 🙏 Credits

Built with:
- Bun - Ultra-fast JavaScript runtime
- Next.js - React framework
- FlixHQ - Content provider
- FFmpeg - Multimedia framework
- And many other open source projects

---

**Enjoy streaming! 🎬**
