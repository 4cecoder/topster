# Quick Start Guide

## Installation & Build

```bash
# Install dependencies
bun install

# Build
bun run build

# Run
./dist/index.js
```

## Usage

### Interactive TUI (New!)

```bash
# Launch the new TUI
./dist/index.js

# Navigate with arrow keys
# Press Enter to select
# Press ESC to go back
# Press Q to quit from main menu
```

### Old CLI (Still Works)

```bash
# Direct search
./dist/index.js "wednesday"

# Trending content
./dist/index.js --trending

# Recent movies
./dist/index.js --recent movie

# Recent TV shows
./dist/index.js --recent tv

# Continue watching
./dist/index.js --continue

# Download instead of stream
./dist/index.js "matrix" --download ~/Downloads
```

## Debug & Testing

### Dry Run (No Video Playback)

```bash
# Test without playing - shows all details
./dist/index.js "wednesday" --dry-run
```

### Debug Logging

```bash
# View logs in real-time
tail -f ~/.topster/debug.log

# View recent logs
tail -50 ~/.topster/debug.log

# Search for errors
grep ERROR ~/.topster/debug.log

# Clear logs
> ~/.topster/debug.log
```

### Enable Debug Mode

```bash
# More verbose output
./dist/index.js "wednesday" --debug
```

## Common Options

```bash
--dry-run           # Show details without playing
--debug             # Enable debug output
--download [path]   # Download instead of stream
--quality <q>       # Video quality (480|720|1080|auto)
--language <lang>   # Subtitle language filter
--no-subs           # Disable subtitles
--discord           # Enable Discord Rich Presence
--config            # Show configuration
--edit              # Edit config file
```

## Examples

### Watch Wednesday S02E02

**With TUI:**
```bash
./dist/index.js
# Select Search
# Type "wednesday"
# Select Wednesday
# Select Season 2
# Select Episode 2
```

**With CLI:**
```bash
echo -e "1\n2\n2" | ./dist/index.js "wednesday"
```

**Test First (Dry Run):**
```bash
echo -e "1\n2\n2" | ./dist/index.js "wednesday" --dry-run
```

### Download The Matrix

```bash
./dist/index.js "the matrix" --download ~/Movies
```

### Stream with Spanish Subtitles

```bash
./dist/index.js "money heist" --language spanish
```

### No Subtitles

```bash
./dist/index.js "dark" --no-subs
```

## Troubleshooting

### Video Won't Play

1. Check debug log:
   ```bash
   tail -50 ~/.topster/debug.log
   ```

2. Try dry-run to see video URL:
   ```bash
   ./dist/index.js "show name" --dry-run
   ```

3. Check if player is installed:
   ```bash
   which mpv  # or vlc, or iina
   ```

### TUI Looks Broken

- Resize terminal window
- Use modern terminal (iTerm2, Alacritty, Windows Terminal)
- Try old CLI mode instead: `./dist/index.js "query"`

### Search Not Working

- Check internet connection
- Look at debug log for scraper errors
- Try a different search term

### Player Not Found

Edit config to set your player:

```bash
./dist/index.js --edit
```

Set `player` to one of: `mpv`, `vlc`, or `iina`

## File Locations

```
~/.topster/
  ├── config.json      # Configuration
  ├── history.json     # Watch history
  └── debug.log        # Debug logs
```

## Quick Tips

1. **Always check logs first** when debugging: `tail -50 ~/.topster/debug.log`
2. **Use dry-run** to test without playing: `--dry-run`
3. **Exit TUI** with ESC multiple times or press Q at main menu
4. **Resume watching** with Continue Watching option or `--continue`
5. **Arrow keys** work everywhere in the new TUI!

## What's New in 2026 TUI

- 🎨 Rainbow gradient logo
- ⌨️ Arrow key navigation (no more typing numbers!)
- 🔙 ESC key for back navigation
- 🍞 Breadcrumb trail
- 🎭 Animated spinners
- 📊 Visual progress bars
- 🐛 Debug logging to file
- 🧪 Dry-run mode for testing
- ✨ Modern, clean design

Enjoy! 🎬
