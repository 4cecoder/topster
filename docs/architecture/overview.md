# Topster Architecture

## High-Level Overview

Topster is a multi-platform streaming application that provides:
- **CLI App**: Terminal-based media browsing and playback
- **Web App**: Browser-based interface for streaming
- **Android TV**: Smart TV application with remote control support
- **MCP Server**: Model Context Protocol server for AI integration

## Technology Stack

### Core Platform (CLI & Shared)
- **Runtime**: Bun (JavaScript runtime)
- **CLI Framework**: Ink (React for terminal UI)
- **Database**: SQLite with Drizzle ORM
- **Language**: TypeScript
- **MCP Server**: Custom server implementation

### Web Platform
- **Framework**: Next.js 14+ with App Router
- **UI Framework**: React 18+ with shadcn/ui components
- **Styling**: Tailwind CSS
- **State Management**: Zustand (optional)
- **HTTP Client**: Built-in fetch API

### Android TV Platform
- **UI Framework**: Jetpack Compose (Material 3)
- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Player**: ExoPlayer (Media3)
- **Database**: Room (SQLite) with Drizzle ORM
- **Linting**: Android Lint + Detekt (Kotlin code quality)

## Module Organization

```
src/
├── cli/              # CLI application
│   ├── components/     # React components for Ink
│   └── commands.ts    # Command handlers
├── core/              # Shared types, errors, config
│   ├── types.ts      # Core TypeScript types
│   ├── errors.ts     # Custom error classes
│   └── config.ts     # Configuration management
├── modules/           # Feature modules
│   ├── scraper/      # Web scraping (extractors, sources)
│   ├── player/       # Media player (ExoPlayer wrapper)
│   ├── history/       # Watch history management
│   ├── imdb/          # IMDb API integration
│   ├── cache/         # Caching layer
│   └── discord/       # Discord webhook integration
├── mcp/               # Model Context Protocol server
├── utils/             # Shared utility functions
└── index.ts           # CLI entry point

tv/
├── android/            # Android TV application
│   ├── app/
│   │   ├── src/main/java/com/topster/tv/
│   │   │   ├── ui/screens/       # Compose screens
│   │   │   ├── ui/components/    # Reusable Compose components
│   │   │   ├── database/         # Room database setup
│   │   │   ├── player/           # ExoPlayer setup
│   │   │   ├── api/              # API models
│   │   │   ├── scraper/          # Video source extraction
│   │   │   └── viewmodels/       # MVVM ViewModels
│   │   └── build.gradle.kts   # Build configuration
│   └── docs/             # Android-specific docs

site/                    # Next.js web application
├── app/              # Next.js app pages
├── components/         # React components
├── lib/               # Shared libraries
└── public/            # Static assets

drizzle/               # Database migrations
└── docs/               # Project documentation
```

## Data Flow

### 1. User Interaction Flow
```
User → [CLI/Web/Android TV]
         ↓
Search/Filter Media
         ↓
Select Media Item
         ↓
Fetch Season/Episode List
         ↓
Select & Play Media
         ↓
Update Watch History
```

### 2. Database Flow
```
TopsterDatabase (Room/Drizzle)
    ↓
historyDao / watchHistory table
    ↓
Store: mediaId, title, type, episodeId, position, percentWatched, completed
    ↓
Query: getIncomplete() for "Continue Watching"
    ↓
Query: getAllHistory() for grouped display (Android)
```

### 3. Media Scraping Flow
```
Scraper Module
    ↓
Search on Streaming Site
    ↓
Extract Video Sources (M3U8)
    ↓
Extract Subtitles/Captions
    ↓
Filter by Quality/Format
    ↓
Return to Player
```

### 4. MCP Server Flow
```
User Request → MCP Client
         ↓
Topster MCP Server
         ↓
Fetch from Topster API
         ↓
Format & Return to AI
```

## Key Design Principles

### 1. Separation of Concerns
- **UI Layer**: Compose components/screens (CLI), React (Web), Jetpack (Android)
- **Business Logic**: Service modules (scraper, player, history)
- **Data Layer**: Database DAOs and migrations
- **API Layer**: Scrapers and external APIs

### 2. Code Reuse
- **Types**: Shared in `src/core/types.ts`
- **Errors**: Custom classes in `src/core/errors.ts`
- **Config**: Centralized in `src/core/config.ts`
- **Utilities**: Common functions in `src/utils/`

### 3. Platform Optimization
- **CLI**: Minimal dependencies (Bun only), fast startup
- **Web**: React Server Components (RSC) for performance
- **Android TV**: Lazy loading, paging, room database caching

### 4. Error Handling
- Custom error classes for each domain (NetworkError, ScrapingError, etc.)
- Centralized error formatting via `formatError()`
- User-friendly error messages in CLI

## Technology Decisions

### Why Bun?
- **Performance**: 10x faster startup than Node.js
- **Bundle size**: Smaller by default
- **Built-in**: Native SQLite support without external dependencies

### Why Ink for CLI?
- **Terminal-native**: Designed for terminal output
- **Accessible**: Good keyboard navigation support
- **Declarative**: Component-based UI (React-like)
- **Lightweight**: Small bundle size for CLI

### Why Jetpack Compose for Android TV?
- **Modern UI**: Latest Android UI framework
- **TV-optimized**: Built-in D-pad support, focus management
- **Declarative**: Less boilerplate, better state management
- **Preview**: Live preview for rapid development

### Why Next.js for Web?
- **SSR**: Server-side rendering for better SEO
- **App Router**: File-based routing for performance
- **API Routes**: Server components for better data fetching
- **Active Development**: Frequent updates, strong community

## Database Schema

### History Table (SQLite/Room)
```sql
CREATE TABLE watch_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    mediaId TEXT NOT NULL,
    title TEXT NOT NULL,
    type TEXT NOT NULL,  -- 'movie' or 'tv'
    url TEXT,
    posterImage TEXT,
    episodeId TEXT,
    episodeTitle TEXT,
    seasonNumber INTEGER,
    episodeNumber INTEGER,
    position INTEGER,
    duration INTEGER,
    percentWatched REAL,
    completed INTEGER DEFAULT 0,
    lastWatched INTEGER,
    createdAt INTEGER,
    updatedAt INTEGER,
    UNIQUE(mediaId, episodeId)
);
```

## Cross-Platform Features

### Watch History
- **CLI**: JSON-based history with automatic backup
- **Web**: Server-side storage with database
- **Android TV**: Local Room database with grouped navigation
- **Key Feature**: Continue Watching, grouped shows, season/episode drill-down

### Filtering
- **CLI**: Media type, year range, quality filters
- **Android TV**: Expandable filter bar with chips for D-pad navigation
- **Web**: (To be implemented)
- **Integration**: Filter applies to search results and history

### Player
- **CLI**: System player (mpv, vlc, etc.)
- **Web**: HTML5 video player
- **Android TV**: ExoPlayer with customizable controls
- **Key Features**: Playback speed, seek intervals, custom buttons (SmartTube-inspired)

## API Integration

### Scrapers
- **Primary**: FlixHQ (main source)
- **Backup**: Multiple extractors (VidCloud, RapidCloud, MegaCloud, StreamSB)
- **Strategy**: Fallback to backups if primary fails
- **Extraction**: M3U8 HLS parsing, subtitle extraction

### External APIs
- **IMDb**: Ratings, cast, plot metadata (OEMDb API)
- **Quality Filtering**: Stream quality selection (HD, FHD, 4K)

## Performance Optimization

### Caching Strategies
- **CLI/Android**: In-memory cache during session
- **Web**: Redis caching (if available)
- **Database**: Index on `mediaId` and `episodeId`
- **API**: 7-day TTL for IMDb lookups, aggressive caching

### Lazy Loading
- **CLI**: Load only active screen data
- **Web**: Code splitting, route-based loading
- **Android TV**: LazyVerticalGrid for media lists, paging

## Security Considerations

### Input Validation
- All URLs validated before playback
- User inputs sanitized (SQL injection protection)
- SSRF protection for HTTP requests

### Data Protection
- API keys stored securely (not in git)
- Database encryption at rest (Android KeyStore)
- No sensitive data in error logs

## Testing Strategy

### Unit Tests
- **CLI**: Bun test runner, `*.test.ts` files
- **Web**: Jest (to be added)
- **Android TV**: Robolectric for unit tests

### Integration Tests
- **CLI**: Manual testing
- **Web**: E2E tests (to be added)
- **Android TV**: Espresso for UI tests

### Linting
- **CLI/TS**: Bun type checking
- **Android TV**: Android Lint + Detekt (strict mode)
- **Web**: ESLint (Next.js configuration)

## Deployment

### CLI
- **Distribution**: NPM package, bun install
- **Binary**: Single executable
- **Cross-platform**: Works on macOS, Linux, Windows

### Web
- **Hosting**: Vercel (recommended) or any Node.js host
- **Build**: Static export (bun run build)
- **Performance**: ISR for SEO, RSC for speed

### Android TV
- **Distribution**: Google Play Store or APK sideloading
- **Build**: Gradle (./gradlew assembleStableRelease)
- **Signing**: Release APK must be signed
- **Flavors**: Stable and Beta variants

## Future Considerations

### Scalability
- MCP Server can support concurrent connections
- Database schema designed for future migration
- Scraper architecture allows adding new sources

### Extensibility
- Plugin system for scrapers
- Modular player backends (can add other players)
- Configurable UI components
- MCP tools can be easily extended

---

**Last Updated**: December 29, 2025
