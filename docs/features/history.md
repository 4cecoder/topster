# Watch History Feature

## Overview

Topster provides a comprehensive watch history system that tracks your viewing progress across all platforms (CLI, Web, Android TV). History is stored locally and grouped hierarchically for TV shows.

## Features

### 1. Hierarchical History (Android TV)

**Navigation Structure:**
```
Continue Watching (Main History Screen)
├── 📺 TV Shows (grouped by show)
│   ↓ Select → Show Details
│   └── Season List
│       ↓ Select → Episode List
│           ↓ Select → Play from last position
└── 🎬 Movies (individual entries)
```

**Key Capabilities:**
- **One entry per TV show** - No duplicate shows, shows latest episode
- **Latest episode info** - Displays `S01E03 - Episode Name`
- **Total episodes tracked** - Shows how many episodes from this show are in history
- **Season/episode drill-down** - Full hierarchical navigation
- **Continue watching** - Resume from last position

### 2. CLI & Web History

**Features:**
- Grouped history (shows grouped together)
- Progress indicators for each entry
- Pagination support
- Clean display with episode info

## Database Schema

### History Table
```sql
CREATE TABLE watch_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    mediaId TEXT NOT NULL,              -- Media ID (IMDb or internal)
    title TEXT NOT NULL,               -- Display title
    type TEXT NOT NULL,               -- 'movie' or 'tv'
    url TEXT,                         -- Stream URL
    posterImage TEXT,                  -- Thumbnail URL
    episodeId TEXT,                   -- Episode ID (for TV shows)
    episodeTitle TEXT,                 -- Episode name
    seasonNumber INTEGER,               -- Season number
    episodeNumber INTEGER,               -- Episode number
    position INTEGER,                   -- Current playback position (ms)
    duration INTEGER,                   -- Total duration (ms)
    percentWatched REAL,               -- Percentage (0-100)
    completed INTEGER DEFAULT 0,         -- 1 if 90%+ watched
    lastWatched INTEGER                -- Timestamp of last watch
    createdAt INTEGER                   -- When entry was created
    updatedAt INTEGER                   -- Last update time
    UNIQUE(mediaId, episodeId) ON CONFLICT REPLACE
);
```

## API Usage

### Android TV (Kotlin)
```kotlin
// Get all history for grouping
val history = historyManager.getAllWatchHistory()

// Get history for specific TV show
val showHistory = historyManager.getHistoryByShow(showTitle)

// Clear all history
historyManager.clearHistory()
```

### CLI (TypeScript)
```typescript
// Get grouped history
const result = await history.getGroupedHistory(10, 0);

// Get incomplete for Continue Watching
const incomplete = await history.getIncomplete();
```

## Configuration

### CLI
- History file: `~/.local/share/topster/history.json.backup`
- Auto-save interval: Every 30 seconds during playback

### Android TV
- Database: Room database at `~/.local/share/topster/history.db`
- Automatic persistence: On every position update

### Web
- Server-side: Stored in MCP server database
- Real-time sync via WebSocket

## Storage Locations

| Platform | Location |
|----------|----------|
| CLI | `~/.local/share/topster/history.json.backup` |
| Android TV | `~/.local/share/topster/history.db` |
| Web | MCP server database |

## Technical Implementation

### Android TV
- **File**: `tv/android/app/src/main/java/com/topster/tv/database/HistoryManager.kt`
- **DAO**: `tv/android/app/src/main/java/com/topster/tv/database/HistoryDao.kt`
- **Screen**: `tv/android/app/src/main/java/com/topster/tv/ui/screens/HistoryScreen.kt`

### CLI/Web
- **Core**: `src/modules/history/index.ts`
- **Storage**: `src/modules/history/sqlite.ts`
- **Schema**: `src/modules/history/schema.ts`

## See Also
- [Architecture Overview](architecture/overview.md)
- [Filtering System](features/filtering.md)
- [Player Configuration](features/player.md)
- [Getting Started](guides/getting-started.md)
