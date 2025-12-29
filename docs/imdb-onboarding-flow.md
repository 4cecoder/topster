# IMDb Feature Onboarding Flow

## Visual Flow Diagram

```
┌─────────────────────────────────────────┐
│  User Opens App for First Time         │
│  (No OMDb API Key Configured)           │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  User Navigates to Media List           │
│  (Search/Trending/Recent)                │
└─────────────┬───────────────────────────┘
              │
              ▼
┌─────────────────────────────────────────┐
│  ┌─────────────────────────────────┐   │
│  │  🎬 New Feature: IMDb Lookup!   │   │
│  │                                 │   │
│  │  • Ratings & reviews            │   │
│  │  • Cast & crew                  │   │
│  │  • Plot synopsis                │   │
│  │  • Awards & more                │   │
│  │                                 │   │
│  │  Quick Setup (30 seconds)       │   │
│  │  1. Visit omdbapi.com           │   │
│  │  2. Get free API key            │   │
│  │  3. Paste below                 │   │
│  │                                 │   │
│  │  [Enter] Continue  [ESC] Skip   │   │
│  └─────────────────────────────────┘   │
└─────────┬──────────────────┬────────────┘
          │                  │
    [Enter]                [ESC]
          │                  │
          ▼                  ▼
┌──────────────────┐  ┌──────────────────┐
│  API Key Input   │  │  Skip - Done     │
│                  │  │  (Won't ask      │
│  Paste key:      │  │   again)         │
│  [________]      │  └──────────────────┘
│                  │
│  [Enter] Save    │
│  [ESC] Skip      │
└────┬──────┬──────┘
     │      │
[Enter]   [ESC]
     │      │
     │      └─────────────┐
     ▼                    ▼
┌──────────────────┐  ┌──────────────────┐
│  Key Saved!      │  │  Skip - Done     │
│  Show Hint       │  │                  │
└────┬─────────────┘  └──────────────────┘
     │
     ▼
┌──────────────────────────────────────┐
│  ┌────────────────────────────────┐ │
│  │  💡 Tip: IMDb Lookup Enabled   │ │
│  │                                │ │
│  │  Press 'i' on any show/movie   │ │
│  │  to view IMDb info!            │ │
│  │                                │ │
│  │  [Press any key to continue]   │ │
│  └────────────────────────────────┘ │
└──────────────────────────────────────┘
     │
     ▼
┌──────────────────────────────────────┐
│  Ready to Use!                       │
│  Browse media and press 'i' anytime  │
└──────────────────────────────────────┘
```

## State Persistence

The onboarding system tracks state in `~/.local/share/topster/imdb-onboarding.json`:

```json
{
  "imdbOnboardingCompleted": false,  // Set to true after onboarding (even if skipped)
  "imdbFeatureShown": false          // Set to true after showing the hint
}
```

## User Experience Goals

✅ **Non-intrusive**: Only shows once, at a natural moment (when browsing media)
✅ **Skippable**: User can press ESC at any time
✅ **Helpful**: Provides clear instructions with exact URL and steps
✅ **Smart**: Remembers if user skipped, won't nag again
✅ **Quick**: 30-second setup if user chooses to proceed
✅ **Contextual**: Shows hint after setup so user knows how to use it

## Alternative Setup Paths

Users can also configure the API key later:

1. **Via Settings Menu**:
   - Main Menu → Settings → Features → OMDb API Key

2. **Via Config File** (Advanced):
   - Edit `~/.config/topster/config.json`
   - Add: `"omdbApiKey": "your-key-here"`

## Onboarding Triggers

The onboarding will trigger when:
- User has NO API key configured
- User has NOT completed onboarding before
- User navigates to: Search Results, Trending, or Recent screens

The onboarding will NOT trigger:
- If user already has an API key
- If user previously skipped onboarding
- On the main menu or settings screens
