# IMDb Lookup Feature

## Overview

The IMDb lookup feature allows you to press `i` while browsing shows/movies to view detailed information from IMDb, including ratings, plot, cast, and more.

## Setup

### First-Time Setup (Automatic Onboarding)

When you first browse a media list (Search/Trending/Recent), you'll see an **automatic onboarding modal**:

1. **Intro Screen**:
   - Explains the IMDb lookup feature
   - Shows what you can view (ratings, cast, plot, etc.)
   - Provides quick setup instructions
   - Press **Enter** to continue or **ESC** to skip

2. **API Key Input Screen**:
   - Enter your OMDb API key
   - Press **Enter** to save
   - Press **ESC** to skip setup

3. **Feature Hint** (after setup):
   - Brief reminder that you can press `i` for IMDb info
   - Press any key to dismiss

### Getting an OMDb API Key

1. Visit: https://www.omdbapi.com/apikey.aspx
2. Select "FREE" plan (1,000 requests/day)
3. Enter your email
4. Check your email and click the activation link
5. Copy your API key
6. Paste it in the onboarding screen OR Settings → Features → OMDb API Key

### Manual Setup (Skip Onboarding)

If you skip onboarding, you can set up later:
1. Open Topster
2. Navigate to Settings → Features
3. Select "OMDb API Key"
4. Enter your API key and press Enter

## Usage

1. Navigate to any media list (Search Results, Trending, Recent)
2. Highlight a show/movie using arrow keys
3. Press `i` to view IMDb information
4. A modal will appear with:
   - Title and year
   - IMDb rating and votes
   - Other ratings (Rotten Tomatoes, Metacritic)
   - Type (Movie/TV Series)
   - Rating (PG, R, etc.)
   - Runtime
   - Genre
   - Director
   - Cast
   - Full plot synopsis
   - Awards
5. Press `ESC` to close the modal

## Features

- **Aggressive Caching**:
  - In-memory cache for instant lookups during session
  - Persistent disk cache survives app restarts
  - 7-day cache TTL to minimize API calls
  - Dual-key caching (by title+year AND IMDb ID)
- **API Conservation**:
  - Only 1 API call per unique lookup
  - No automatic/background requests
  - Only triggers when you press 'i'
  - Removed fallback search to save API quota
- **Rich Information**: Shows comprehensive details from IMDb
- **Loading State**: Visual feedback while fetching data
- **Error Handling**: Clear error messages if lookup fails

## Troubleshooting

### "OMDb API key not configured"
- Make sure you've entered your API key in Settings → Features → OMDb API Key

### "No results found" or "Title not found"
- The show/movie might not be in IMDb's database
- Try a different title or check the spelling

### "Response": "False"
- Your API key might be invalid or expired
- Check if you've exceeded the daily limit (1,000 requests)
- Verify your API key at https://www.omdbapi.com/

## Technical Details

- **API**: OMDb API (https://www.omdbapi.com/)
- **Cache Strategy**:
  - **Location**: `~/.local/share/topster/imdb-cache.json`
  - **TTL**: 7 days
  - **Persistence**: Yes - survives app restarts
  - **Keys**: Dual-key (title+year and IMDb ID)
- **API Usage**:
  - 1 call per unique title lookup
  - No fallback searches (conserves quota)
  - User-initiated only (press 'i')
- **Rate Limit**: 1,000 requests per day (free tier)
- **Estimated Usage**: With caching, typical user should stay well under 1000/day limit

## Onboarding Flow Details

The onboarding system is smart and non-intrusive:

- **Triggers once**: Only shows when you first browse media (if no API key is set)
- **Skippable**: Press ESC at any time to skip
- **Remembers choice**: Won't nag you again if you skip
- **Persistent state**: Saved to `~/.local/share/topster/imdb-onboarding.json`
- **Auto-hint**: After setup, shows a one-time hint about pressing 'i'

## Files Modified

- `src/modules/imdb/index.ts` - API integration with caching
- `src/modules/imdb/types.ts` - TypeScript types
- `src/modules/imdb/onboarding.ts` - Onboarding state management
- `src/cli/components/IMDbModal.tsx` - Modal UI component
- `src/cli/components/IMDbOnboarding.tsx` - Onboarding UI
- `src/cli/components/MediaList.tsx` - Added 'i' key handler
- `src/cli/components/Settings.tsx` - Added API key setting
- `src/cli/ink-app.tsx` - Modal and onboarding orchestration
- `src/core/config.ts` - Added omdbApiKey config field
