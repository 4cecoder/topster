# Media Filtering System

## Overview

Topster provides a powerful filtering system to help you find content quickly. Filter by media type, year range, and quality.

## Features

### Media Type Filter

**Options:**
- 🎬 **Movies** - Show only movies
- 📺 **TV Shows** - Show only TV series
- **All** - Show both movies and TV shows

**Implementation:**
- CLI: `FilterBar` component with toggle
- Android TV: `FilterBar` Compose component with chips
- Web: Filter sidebar or modal

### Year Range Filter

**Options:**
- **Custom Range** - Filter by min/max year
- **Specific Year** - Show only content from that year
- **All Years** - Disable year filtering

**Usage:**
```kotlin
// Android TV
FilterBar(
    filters = FilterOptions(
        yearRange = YearRange(min = 2020, max = 2024)
    ),
    onFilterChange = { applyFilters(it) }
)
```

### Quality Filter

**Options:**
- **720p (HD)** - High Definition
- **1080p (Full HD)** - Full High Definition
- **2160p (4K)** - Ultra HD

**Implementation:**
- Multi-select (can select multiple qualities)
- Show only available qualities for content
- Filter applies to streaming sources

## Technical Implementation

### Android TV (Kotlin)
**File**: `tv/android/app/src/main/java/com/topster/tv/ui/components/FilterBar.kt`

**Data Classes:**
```kotlin
data class FilterOptions(
    val type: MediaType? = null,
    val yearRange: YearRange? = null,
    val quality: List<String>? = null,
    val hasYearOnly: Boolean? = null
)

data class YearRange(
    val min: Int? = null,
    val max: Int? = null
)

enum class MediaType {
    ALL, MOVIE, TV_SHOW
}

enum class MediaQuality(val displayName: String, val value: String) {
    HD("HD", "720p"),
    FHD("Full HD", "1080p"),
    UHD("4K", "2160p");
}
```

### CLI (TypeScript)
**File**: `src/cli/components/FilterBar.tsx`

**Interface:**
```typescript
export interface Filters {
  type?: MediaType;
  yearRange?: { min?: number; max?: number };
  quality?: string[];
  hasYear?: boolean;
}
```

## Usage Examples

### CLI
```typescript
import { FilterBar } from './components/FilterBar';

<FilterBar
  filters={{
    type: 'tv',
    yearRange: { min: 2020, max: 2024 },
    quality: ['1080p', '4K']
  }}
  totalItems={150}
  filteredItems={42}
  onClearFilters={() => setFilters({})}
/>
```

### Android TV
```kotlin
@Composable
fun FilterBar(
    filters: FilterOptions,
    totalItems: Int,
    filteredItems: Int,
    onFilterChange: (FilterOptions) -> Unit
)
```

## Performance Considerations

- Filter operations are **instant** (client-side filtering)
- No API calls required for basic filters
- Only applies filters when fetching new content
- Works with pagination and infinite scroll

## See Also
- [History Feature](features/history.md) - How filtering integrates with watch history
- [Player Configuration](features/player.md) - Player settings and controls
- [Getting Started](guides/getting-started.md) - Basic usage guide
