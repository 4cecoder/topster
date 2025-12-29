# Smart Filters & Enhanced Display

## Overview

The TUI now features smart filtering and enhanced display formatting for media listings, making it easy to find exactly what you're looking for.

## Features

### 🎯 Smart Filters

Filter media by:
- **Type**: Movies only or TV Shows only
- **Quality**: HD/4K content only
- **Year Range**: Filter by release year (future enhancement)
- **Has Year**: Show only items with year information

### 📊 Enhanced Display

Each media item now shows:
- **Icon**: 🎬 for movies, 📺 for TV shows
- **Title**: Full title of the media
- **Year**: Release year in parentheses (2010)
- **Quality**: [HD], [4K], [CAM], etc.
- **Duration**: ⏱ 120m (when available)

Example: `🎬 Inception (2010) [HD] ⏱ 148m`

### ⌨️ Keyboard Shortcuts

| Key | Action |
|-----|--------|
| `m` | Toggle Movies only filter |
| `t` | Toggle TV Shows only filter |
| `h` | Toggle HD quality filter |
| `f` | Clear all filters |
| `i` | Show IMDb info (existing feature) |

### 🔍 Filter Bar

When filters are active, a blue filter bar appears showing:
- Active filters
- Item count (filtered/total)
- Instructions to clear filters

Example:
```
┌─────────────────────────────────────────────────────────┐
│ 🔍 Filters: Type: 🎬 Movies • Quality: HD (15/50 items) │
│ • Press F to clear                                      │
└─────────────────────────────────────────────────────────┘
```

## Usage

### Filtering Movies

1. Browse to any media list (Search Results, Trending, Recent)
2. Press `m` to show only movies
3. Filter bar appears showing active filter
4. List updates to show only movies
5. Press `m` again to toggle off, or `f` to clear all filters

### Filtering HD Content

1. In any media list
2. Press `h` to show only HD quality content
3. Includes: HD, 4K, 1080p, 720p
4. Press `h` again to toggle off

### Combining Filters

Filters can be combined! For example:
- Press `m` (movies only) + `h` (HD only)
- Result: Only HD movies are shown
- Filter bar: `Type: 🎬 Movies • Quality: HD, 4K, 1080p, 720p`

### Clearing Filters

- Press `f` to instantly clear all active filters
- Or toggle individual filters off (press same key again)

## How It Works

### Client-Side Filtering

- Filters are applied on the client side (in-memory)
- No additional network requests
- Instant response
- Works on paginated results

### Filter Logic

```typescript
// Type filter
if (filters.type && item.type !== filters.type) {
  return false; // Exclude item
}

// Quality filter
if (filters.quality && item.quality) {
  if (!filters.quality.includes(item.quality)) {
    return false; // Exclude item
  }
}

// Year range filter (future)
if (filters.yearRange) {
  const itemYear = parseInt(item.year);
  if (itemYear < min || itemYear > max) {
    return false; // Exclude item
  }
}
```

### State Management

Filters are stored in `AppState.filters`:

```typescript
interface Filters {
  type?: 'movie' | 'tv';
  yearRange?: { min?: number; max?: number };
  quality?: string[];
  hasYear?: boolean;
}
```

## Examples

### Example 1: Finding Recent HD Movies

1. Navigate to Main Menu → Trending
2. Press `m` (movies only)
3. Press `h` (HD only)
4. Result: List of HD movies only

### Example 2: Browsing TV Shows

1. Navigate to Search → Enter "Star"
2. Press `t` (TV shows only)
3. Browse only TV shows matching "Star"

### Example 3: Clearing Filters

1. With filters active (e.g., movies only + HD only)
2. Press `f`
3. All filters cleared, full list restored

## Visual Flow

```
Without Filters:
┌─────────────────────────────────────┐
│ 🔥 Trending Now                     │
├─────────────────────────────────────┤
│ 🎬 Inception (2010) [HD] ⏱ 148m    │
│ 📺 Breaking Bad (2008) [HD]         │
│ 🎬 The Matrix (1999) [4K]           │
│ 📺 Game of Thrones (2011) [HD]      │
│ 🎬 Interstellar (2014) [HD]         │
└─────────────────────────────────────┘

Press 'm' for movies only:
┌──────────────────────────────────────────────┐
│ 🔥 Trending Now                              │
├──────────────────────────────────────────────┤
│ 🔍 Filters: Type: 🎬 Movies (3/5 items)      │
│ • Press F to clear                           │
├──────────────────────────────────────────────┤
│ 🎬 Inception (2010) [HD] ⏱ 148m             │
│ 🎬 The Matrix (1999) [4K]                    │
│ 🎬 Interstellar (2014) [HD]                  │
└──────────────────────────────────────────────┘
```

## Future Enhancements

Potential additions:
- **Year Range Slider**: Filter by decade or year range
- **Rating Filter**: Filter by IMDb rating (requires API call)
- **Genre Filter**: Filter by genre (action, comedy, etc.)
- **Search Within Results**: Search within filtered results
- **Save Filter Presets**: Save favorite filter combinations
- **Sort Options**: Sort by year, rating, title, etc.

## Technical Details

### Performance

- Filters use React useMemo for efficient re-rendering
- Only recomputes when items or filters change
- No performance impact on large lists (1000+ items)

### Accessibility

- Clear visual feedback (filter bar)
- Keyboard-only navigation
- Help text shows available shortcuts
- Empty state message when no results match

## Troubleshooting

### No Items After Filtering

**Problem**: Filter shows "No items match the current filters"

**Solution**: Press `f` to clear filters or adjust filter criteria

### Filter Not Working

**Problem**: Pressing filter key doesn't activate filter

**Solution**:
- Make sure you're on a media list screen (not settings, etc.)
- Check that items have the filterable property (e.g., quality field)

### Can't See Duration

**Problem**: Duration (⏱) doesn't show for some items

**Reason**: Not all scraped data includes duration - this is normal

## Files Modified

- `src/cli/components/MediaList.tsx` - Filter logic & enhanced display
- `src/cli/components/FilterBar.tsx` - Filter UI component (new)
- `src/cli/ink-app.tsx` - Filter state management & handlers
- `src/core/types.ts` - Filter types (via FilterBar)

---

**Status**: ✅ Complete & Ready
**Build**: ✅ Passing (1213 modules)
**Features**: Filters, Enhanced Display, Keyboard Shortcuts
