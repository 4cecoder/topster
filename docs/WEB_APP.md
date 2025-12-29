# Topster Web App - Architecture & Integration Guide

## Overview

The Topster web app is a full-featured Next.js 16 application that provides a browser-based interface for streaming movies and TV shows. It integrates seamlessly with the Topster CLI via the MCP (Model Context Protocol) server and includes support for LAN-based TV app discovery and remote playback.

## Architecture

```
┌─────────────────┐
│   Next.js Web   │
│   (Browser UI)  │
└────────┬────────┘
         │
         ├── Server Actions (lib/actions/)
         │   ├── media.ts    - Search, browse, video sources
         │   ├── history.ts  - Watch history tracking
         │   └── config.ts   - Settings management
         │
         ├── MCP Client (lib/mcp-client.ts)
         │   └── HTTP JSON-RPC to localhost:3847
         │
         └── API Routes (app/api/)
             ├── /api/tv/devices    - TV registration
             ├── /api/tv/heartbeat  - Keep-alive
             └── /api/proxy         - CORS proxy

                 ↓

┌─────────────────────────────┐
│  Topster CLI MCP Server     │
│  (Elysia.js on port 3847)   │
└──────────────┬──────────────┘
               │
               ├── Video Extractors
               ├── FlixHQ Scraper
               ├── SQLite History
               ├── IMDb Integration
               └── Config Management
```

## Key Technologies

### Frontend
- **Next.js 16** - React framework with App Router
- **TypeScript** - Type-safe development
- **Tailwind CSS v4** - Utility-first styling
- **Radix UI** - Accessible component primitives
- **HLS.js** - Adaptive video streaming
- **Lucide React** - Icon library

### Backend Integration
- **Server Actions** - React Server Components for data fetching
- **MCP Protocol** - JSON-RPC communication with CLI
- **API Routes** - REST endpoints for TV app integration

## Features Implemented

### 1. Browse & Search
- **Path**: `/`
- **Components**:
  - `SearchBar` - Debounced search input
  - `MediaCard` - Movie/TV show cards with posters
- **Server Actions**:
  - `searchMedia(query)` - Search for content
  - `getTrendingMedia()` - Get trending movies/shows
  - `getRecentMedia()` - Get recent releases

### 2. Video Playback
- **Path**: `/watch/[id]`
- **Components**:
  - `VideoPlayer` - Custom HLS player with controls
- **Features**:
  - Adaptive streaming (HLS)
  - Subtitle support (VTT/SRT)
  - Quality selection
  - Progress tracking
  - Fullscreen mode
- **Server Actions**:
  - `getMediaSeasons(mediaId)` - Get TV show seasons
  - `getSeasonEpisodes(mediaId, season)` - Get episodes
  - `getVideoSources(episodeId)` - Get streaming URLs

### 3. Watch History
- **Path**: `/history`
- **Features**:
  - Resume playback from last position
  - Progress bars showing completion
  - Filter by movies/TV shows
  - Last watched timestamps
- **Server Actions**:
  - `getWatchHistory()` - Load history
  - `updateWatchHistory(...)` - Save progress

### 4. Settings
- **Path**: `/settings`
- **Features**:
  - Video quality selection (auto, 1080p, 720p, 480p)
  - Streaming provider (Vidcloud, UpCloud)
  - Subtitle language
  - Enable/disable features
  - OMDb API key configuration
  - MCP server connection settings

### 5. TV App Integration
- **API Routes**:
  - `POST /api/tv/devices` - Register TV device
  - `GET /api/tv/devices` - List registered devices
  - `POST /api/tv/heartbeat` - Device keep-alive
  - `DELETE /api/tv/devices?id=...` - Unregister device

- **Discovery**:
  - Automatic detection of TV apps on LAN
  - Online/offline status tracking
  - "Cast to TV" functionality

## File Structure

```
site/
├── app/
│   ├── page.tsx              # Home - Browse & Search
│   ├── watch/[id]/page.tsx   # Video Player
│   ├── history/page.tsx      # Watch History
│   ├── settings/page.tsx     # Settings Panel
│   ├── layout.tsx            # Root Layout
│   ├── globals.css           # Global Styles
│   └── api/
│       ├── tv/
│       │   ├── devices/route.ts    # TV Registration
│       │   └── heartbeat/route.ts  # Keep-alive
│       └── proxy/route.ts          # CORS Proxy
│
├── components/
│   ├── media-card.tsx        # Movie/TV Card
│   ├── search-bar.tsx        # Search Input
│   └── video-player.tsx      # HLS Video Player
│
├── lib/
│   ├── types.ts              # Shared TypeScript Types
│   ├── utils.ts              # Utility Functions
│   ├── mcp-client.ts         # MCP Protocol Client
│   ├── lan-discovery.ts      # TV App Discovery
│   └── actions/
│       ├── media.ts          # Media Server Actions
│       ├── history.ts        # History Server Actions
│       └── config.ts         # Config Server Actions
│
├── public/
│   └── placeholder.jpg       # Placeholder Image
│
├── package.json
├── tsconfig.json
├── next.config.ts
├── tailwind.config.ts
└── README.md
```

## Data Flow

### Search Flow
```
1. User types in SearchBar
2. Component calls searchMedia(query) server action
3. Server action calls mcpClient.search(query)
4. MCP client sends JSON-RPC to CLI server
5. CLI scrapes FlixHQ and returns results
6. Results flow back up the chain
7. MediaCards render with results
```

### Video Playback Flow
```
1. User clicks MediaCard
2. Navigate to /watch/[id]
3. Load seasons/episodes via server actions
4. User selects episode to play
5. getVideoSources() fetches streaming URLs
6. VideoPlayer component loads HLS stream
7. Progress updates sent to updateWatchHistory()
8. History saved to SQLite via MCP server
```

### TV App Communication Flow
```
1. TV app starts and calls POST /api/tv/devices
2. Web app stores device info in memory
3. TV app sends periodic POST /api/tv/heartbeat
4. Web app marks device as "online"
5. User's browser calls GET /api/tv/devices
6. Available devices shown in UI
7. User clicks "Cast to TV"
8. Web app sends POST to TV's /command endpoint
9. TV app receives and plays the content
```

## Type Safety

All types are defined in `lib/types.ts` and shared across:
- CLI (can import from site/lib/types.ts)
- Server actions
- React components
- API routes

Example:
```typescript
export interface MediaItem {
  id: string;
  title: string;
  url: string;
  image: string;
  releaseDate?: string;
  seasons?: number;
  type?: 'Movie' | 'TV Series';
}
```

## Environment Variables

The web app supports these environment variables (`.env.local`):

```env
# MCP Server Configuration
NEXT_PUBLIC_MCP_HOST=localhost
NEXT_PUBLIC_MCP_PORT=3847

# Optional: API URL override
NEXT_PUBLIC_API_URL=http://localhost:3000
```

## Development Workflow

### 1. Start Both Services
```bash
./start-web.sh
```

This script:
- Starts MCP server if not running
- Starts Next.js dev server
- Handles cleanup on exit

### 2. Manual Start
```bash
# Terminal 1: Start MCP server
topster --mcp

# Terminal 2: Start web app
cd site
bun run dev
```

### 3. Production Build
```bash
cd site
bun run build
bun run start
```

## TV App Implementation Guide

To create a compatible TV app (Kotlin/Android TV):

### 1. HTTP Server Setup
```kotlin
class TopsterTVServer(private val port: Int = 8765) {
    private val server = embeddedServer(Netty, port) {
        routing {
            get("/ping") {
                call.respond(HttpStatusCode.OK)
            }

            post("/command") {
                val command = call.receive<Command>()
                handleCommand(command)
                call.respond(HttpStatusCode.OK)
            }
        }
    }

    fun start() {
        server.start(wait = false)
    }
}
```

### 2. Registration
```kotlin
suspend fun registerWithWebApp(webAppUrl: String) {
    client.post("$webAppUrl/api/tv/devices") {
        contentType(ContentType.Application.Json)
        setBody(DeviceInfo(
            id = Settings.Secure.getString(resolver, Settings.Secure.ANDROID_ID),
            name = Build.MODEL,
            ip = getLocalIPAddress(),
            port = 8765
        ))
    }
}
```

### 3. Heartbeat
```kotlin
launch {
    while (isActive) {
        try {
            client.post("$webAppUrl/api/tv/heartbeat") {
                contentType(ContentType.Application.Json)
                setBody(mapOf("deviceId" to deviceId))
            }
        } catch (e: Exception) {
            Log.e("Heartbeat", "Failed", e)
        }
        delay(60_000) // 60 seconds
    }
}
```

### 4. Command Handling
```kotlin
data class Command(
    val command: String,
    val data: PlayData?
)

data class PlayData(
    val mediaId: String,
    val title: String,
    val videoUrl: String,
    val subtitles: List<Subtitle>?
)

fun handleCommand(command: Command) {
    when (command.command) {
        "play" -> playVideo(command.data!!)
        "pause" -> pauseVideo()
        "stop" -> stopVideo()
        "seek" -> seekTo(command.data?.position ?: 0)
    }
}
```

## Deployment Options

### Vercel (Recommended)
```bash
cd site
vercel deploy
```

### Docker
```dockerfile
FROM oven/bun:latest
WORKDIR /app
COPY site/package.json site/bun.lock ./
RUN bun install
COPY site/ .
RUN bun run build
EXPOSE 3000
CMD ["bun", "run", "start"]
```

### PM2
```bash
cd site
bun run build
pm2 start "bun run start" --name topster-web
```

## Future Enhancements

1. **Direct Integration** - Import CLI modules directly instead of MCP
2. **Advanced TV Features**:
   - Auto-discovery via mDNS/Bonjour
   - Screen mirroring
   - Queue management
   - Multi-device sync
3. **PWA Support** - Offline functionality, install to home screen
4. **User Accounts** - Personal watchlists, preferences
5. **Download Manager** - Web-based download queue
6. **Chromecast Support** - Native casting integration
7. **Subtitle Editor** - Sync and edit subtitles
8. **Recommendations** - AI-powered suggestions

## Troubleshooting

### Build Errors
```bash
cd site
rm -rf .next node_modules
bun install
bun run build
```

### MCP Connection Issues
1. Check MCP server is running: `lsof -i :3847`
2. Verify server actions can reach localhost
3. Check CORS settings in MCP server

### TV App Not Detected
1. Ensure same network
2. Check firewall rules
3. Verify heartbeat interval
4. Test `/api/tv/devices` endpoint

### Video Playback Issues
1. Check browser console for CORS errors
2. Try `/api/proxy` route
3. Verify video source format (HLS/M3U8)
4. Test in different browsers

## Performance Optimizations

1. **Image Optimization** - Next.js Image component with blur placeholder
2. **Code Splitting** - Dynamic imports for video player
3. **Caching** - HTTP cache headers on video sources
4. **Lazy Loading** - Intersection Observer for media cards
5. **Suspense Boundaries** - Streaming SSR for faster loads

## Security Considerations

1. **CORS** - Proxy API route prevents direct CORS issues
2. **Input Validation** - Server actions validate all inputs
3. **XSS Protection** - React escapes all content
4. **Rate Limiting** - Consider adding to API routes
5. **API Keys** - Store OMDb key server-side in production

## License

Same as parent Topster project.
