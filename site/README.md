# Topster Web App

A Next.js web interface for Topster - stream movies and TV shows from your browser.

## Features

- 🔍 **Search & Browse** - Search for movies and TV shows, browse trending and recent content
- 🎬 **Video Playback** - HLS video player with subtitle support and progress tracking
- 📺 **LAN TV App Detection** - Automatically detect and stream to Topster TV apps on your local network
- 📊 **Watch History** - Track your viewing progress and continue where you left off
- ⚙️ **Settings** - Configure quality, subtitles, providers, and more
- 🌐 **MCP Integration** - Connects to the Topster CLI's MCP server for backend operations

## Prerequisites

Before running the web app, you need to start the Topster CLI's MCP server:

```bash
# From the project root
topster --mcp
```

This starts the MCP (Model Context Protocol) server on `localhost:3847`, which the web app uses for all backend operations.

## Installation

```bash
cd site
bun install
```

> **Note**: This app is built to run with **Bun**, not Node.js. All scripts use `bun --bun` to ensure Bun's native runtime is used for maximum performance.

## Development

```bash
# Quick start (recommended)
../start-web.sh

# Or manually:
bun run dev
```

This runs Next.js with Bun's native runtime, which is significantly faster than Node.js.

Open [http://localhost:3000](http://localhost:3000) in your browser.

## Production Build

```bash
bun run build
bun run start
```

## Architecture

### Frontend Stack

- **Framework**: Next.js 16 (App Router)
- **Language**: TypeScript
- **Styling**: Tailwind CSS v4
- **UI Components**: Radix UI primitives
- **Video Player**: HLS.js for adaptive streaming
- **Icons**: Lucide React

### Backend Integration

The web app uses **Server Actions** to communicate with the Topster CLI:

```
Web App (Next.js) → Server Actions → MCP Client → MCP Server (Topster CLI)
```

### Key Components

#### Server Actions (`lib/actions/`)

- **`media.ts`** - Search, trending, seasons, episodes, video sources
- **`history.ts`** - Watch history tracking
- **`config.ts`** - Configuration management

#### API Routes (`app/api/`)

- **`/api/tv/devices`** - TV device registration and discovery
- **`/api/tv/heartbeat`** - TV device keep-alive
- **`/api/proxy`** - CORS proxy for streaming URLs

#### Pages

- **`/`** - Browse and search interface
- **`/watch/[id]`** - Video player with episode selector
- **`/history`** - Watch history dashboard
- **`/settings`** - Configuration panel

## TV App Integration

The web app can detect and communicate with Topster TV apps (Kotlin) on your LAN.

### TV App Protocol

#### 1. Registration

TV apps register themselves with the web server:

```http
POST /api/tv/devices
Content-Type: application/json

{
  "id": "unique-device-id",
  "name": "Living Room TV",
  "ip": "192.168.1.100",
  "port": 8765
}
```

#### 2. Heartbeat

TV apps send periodic heartbeats to stay "online":

```http
POST /api/tv/heartbeat
Content-Type: application/json

{
  "deviceId": "unique-device-id"
}
```

#### 3. Discovery

Web app discovers registered TV devices:

```http
GET /api/tv/devices

Response:
{
  "devices": [
    {
      "id": "unique-device-id",
      "name": "Living Room TV",
      "ip": "192.168.1.100",
      "port": 8765,
      "online": true,
      "lastSeen": "2025-12-20T12:00:00Z"
    }
  ]
}
```

#### 4. Streaming Commands

Web app sends play commands to TV devices:

```http
POST http://<tv-ip>:<tv-port>/command
Content-Type: application/json

{
  "command": "play",
  "data": {
    "mediaId": "movie-123",
    "title": "Example Movie",
    "videoUrl": "https://example.com/video.m3u8",
    "subtitles": [
      { "url": "https://example.com/en.vtt", "lang": "en" }
    ]
  }
}
```

### TV App Requirements

Your Kotlin TV app should implement:

1. **HTTP Server** on a configurable port (default: 8765)
2. **Endpoints**:
   - `GET /ping` - Health check (return 200 OK)
   - `POST /command` - Accept commands (play, pause, stop, etc.)
3. **Registration** - Call `/api/tv/devices` on startup
4. **Heartbeat** - Call `/api/tv/heartbeat` every 60 seconds

### Example TV App Code

```kotlin
// Kotlin TV App - Registration
fun registerWithWebApp(webAppUrl: String) {
    val client = OkHttpClient()
    val json = JSONObject().apply {
        put("id", getDeviceId())
        put("name", "Living Room TV")
        put("ip", getLocalIP())
        put("port", 8765)
    }

    val request = Request.Builder()
        .url("$webAppUrl/api/tv/devices")
        .post(json.toString().toRequestBody("application/json".toMediaType()))
        .build()

    client.newCall(request).execute()
}

// Heartbeat coroutine
launch {
    while (true) {
        sendHeartbeat(webAppUrl)
        delay(60_000) // 60 seconds
    }
}
```

## Environment Variables

Create a `.env.local` file in the `site/` directory:

```env
# MCP Server Configuration
NEXT_PUBLIC_MCP_HOST=localhost
NEXT_PUBLIC_MCP_PORT=3847

# Optional: Override base URL
NEXT_PUBLIC_API_URL=http://localhost:3000
```

## Troubleshooting

### "MCP server is not running" Error

Make sure the Topster CLI MCP server is running:

```bash
topster --mcp
```

The server should show:
```
🚀 MCP server listening on http://localhost:3847
```

### Video won't play

1. Check browser console for CORS errors
2. Try using the `/api/proxy` route for video URLs
3. Ensure you're using a supported browser (Chrome, Firefox, Safari)

### TV app not detected

1. Ensure TV app and web app are on the same network
2. Check TV app is sending heartbeats every 60s
3. Verify firewall isn't blocking the TV app's port
4. Check `/api/tv/devices` returns your device

## Development Tips

### Testing without MCP Server

Create mock data in your server actions:

```typescript
// lib/actions/media.ts
export async function getTrendingMedia(): Promise<MediaItem[]> {
  if (process.env.NODE_ENV === 'development') {
    return mockTrendingData; // Return mock data
  }
  // Real implementation
}
```

### Hot Module Replacement

Next.js supports HMR out of the box. Edit any component and see changes instantly.

### Type Safety

All types are shared in `lib/types.ts` to ensure consistency between:
- CLI types
- Server actions
- React components
- API routes

## Deployment

### Production with Bun

```bash
# Build the app
bun run build

# Start production server
bun run start

# Or use PM2 for process management
pm2 start "bun run start" --name topster-web

# Or run directly
bun --bun next start -p 3000
```

### Systemd Service (Linux)

Create `/etc/systemd/system/topster-web.service`:

```ini
[Unit]
Description=Topster Web App
After=network.target

[Service]
Type=simple
User=your-user
WorkingDirectory=/path/to/topster/site
ExecStart=/usr/bin/bun --bun next start -p 3000
Restart=always

[Install]
WantedBy=multi-user.target
```

Then:
```bash
sudo systemctl enable topster-web
sudo systemctl start topster-web
```

## Contributing

When adding new features:

1. Add types to `lib/types.ts`
2. Create server actions in `lib/actions/`
3. Build UI components in `components/`
4. Create pages in `app/`
5. Update this README

## License

Same as parent Topster project.
