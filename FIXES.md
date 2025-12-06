# TUI Crash Fixes & Debug Improvements

## Problems Fixed

### 1. **Video Stream Crashes in TUI**
**Problem:** When Ink TUI was running and a video player was spawned with `stdio: 'inherit'`, it would crash the terminal because Ink had control of stdout/stderr.

**Solution:**
- Exit the Ink app before spawning the video player
- Use `exit()` from Ink's `useApp()` hook to properly unmount the UI
- This returns terminal control to the user before the player starts

### 2. **No Error Logging**
**Problem:** When crashes occurred, there were no logs to debug what went wrong.

**Solution:**
- Added debug logging to `~/.topster/debug.log`
- Logs all major operations: search, media selection, source loading, playback
- Includes timestamps and stack traces for errors
- Automatically created on first run

### 3. **Can't Test Without Actually Playing**
**Problem:** Testing required actually playing videos, making debugging slow.

**Solution:**
- Added `--dry-run` flag that shows all details without playing
- Displays:
  - Video URL
  - Quality
  - Subtitle options
  - Resume position
  - Media/Episode IDs

## How to Use

### Debug Logging

Logs are automatically written to `~/.topster/debug.log`:

```bash
# View debug log in real-time
tail -f ~/.topster/debug.log

# View last 50 lines
tail -50 ~/.topster/debug.log

# Clear debug log
> ~/.topster/debug.log
```

### Dry Run Mode

Test what would happen without actually playing:

```bash
# With old CLI (manual selection)
./dist/index.js wednesday --dry-run
# Then select: 1 (Wednesday) -> 2 (Season 2) -> 2 (Episode 2)

# Direct search (requires interaction)
./dist/index.js "wednesday" --dry-run

# With trending
./dist/index.js --trending --dry-run
```

**Output Example:**
```
=== DRY RUN MODE ===

Title: Wednesday S02E02
Media ID: tv/1234
Episode ID: ep/5678

Video Source:
  URL: https://example.com/playlist.m3u8
  Quality: 1080p
  Type: Episode

Subtitles: 15 available
  [1] English - https://example.com/en.vtt
  [2] Spanish - https://example.com/es.vtt
  [3] French - https://example.com/fr.vtt
  [4] German - https://example.com/de.vtt
  [5] Italian - https://example.com/it.vtt
  ... and 10 more

Resume Position: 00:15:30
History Entry: Yes

=== END DRY RUN ===
```

### Testing Workflow

1. **Test with dry run first:**
   ```bash
   ./dist/index.js "wednesday" --dry-run
   ```

2. **Check debug log if something fails:**
   ```bash
   tail -50 ~/.topster/debug.log
   ```

3. **Test actual playback:**
   ```bash
   ./dist/index.js "wednesday"
   ```

4. **If it crashes, check logs:**
   ```bash
   cat ~/.topster/debug.log | grep ERROR
   ```

## Debug Log Format

```
[2025-12-06T10:30:00.000Z] === Topster TUI Started ===
[2025-12-06T10:30:05.123Z] Searching for: wednesday
[2025-12-06T10:30:06.456Z] Found 5 results
[2025-12-06T10:30:10.789Z] Selected media: Wednesday (tv)
[2025-12-06T10:30:11.012Z] Loaded 2 seasons
[2025-12-06T10:30:15.345Z] Selected episode: S2E2 - Episode Title
[2025-12-06T10:30:16.678Z] Getting video sources for: ep/1234
[2025-12-06T10:30:18.901Z] Video URL: https://example.com/video.m3u8
[2025-12-06T10:30:18.902Z] Subtitles: 15 available
[2025-12-06T10:30:18.903Z] Resume position: 0s
[2025-12-06T10:30:18.904Z] Starting player: Wednesday S02E02
[2025-12-06T10:45:30.123Z] Playback finished. Exit code: 0
[2025-12-06T10:45:30.124Z] Updated history position: 00:25:12
[2025-12-06T10:45:30.125Z] Playback completed successfully
```

## Error Messages

All errors now show helpful messages:

- **No video sources:** Clear error + check log message
- **Player not found:** Tells you which player is missing
- **Network errors:** Full error message + stack trace in log
- **Scraper errors:** Detailed error with context

## Technical Details

### Files Modified

1. **src/cli/ink-app.tsx**
   - Added `debugLog()` function
   - Added `useApp().exit()` before playback
   - Added dry-run mode support
   - Added proper error handling with stack traces

2. **src/cli/commands.ts**
   - Added `dryRun` to `CommandContext`
   - Added dry-run logic to `playMedia()`
   - Added `formatTime()` helper

3. **src/cli/index.ts**
   - Added `--dry-run` CLI flag
   - Pass `dryRun` to context

### Key Changes

**Before:**
```typescript
// Player spawned while Ink is running
await playMedia(media, episode, season, ctx);
// CRASH! Ink and player fight over stdout
```

**After:**
```typescript
// Exit Ink first
exit();
// Now safe to spawn player
await playMedia(media, episode, season, ctx);
```

## Testing Notes

- Debug log is append-only, so it grows over time
- Clear it periodically with `> ~/.topster/debug.log`
- Dry-run mode does NOT update history
- Dry-run mode does NOT connect to Discord
- All network requests still happen (to get video URLs)

## Future Improvements

- [ ] Structured JSON logging option
- [ ] Log rotation (max 10MB)
- [ ] Export logs command
- [ ] Filter logs by level (ERROR, INFO, DEBUG)
- [ ] Live log viewer in TUI (press L to toggle)
