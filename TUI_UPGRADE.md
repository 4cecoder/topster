# 🎨 TUI Upgrade - 2026 Edition

## Overview

Topster has been completely revamped with a modern, interactive Terminal User Interface (TUI) powered by **Ink** (React for CLIs). Say goodbye to typing numbers and hello to intuitive arrow-key navigation!

## 🚀 What's New

### Interactive Navigation
- **Arrow keys (↑/↓)** - Navigate through lists
- **Enter** - Select items
- **ESC** - Go back to previous screen
- **Ctrl+C** - Exit application
- **Q** - Quick exit from main menu

### Visual Enhancements
- **Rainbow gradient logo** with big ASCII text on startup
- **Color-coded elements** - Cyan for headers, green for quality tags, etc.
- **Breadcrumb navigation** - Always know where you are
- **Animated spinners** - Loading states look slick
- **Clean borders** - Rounded boxes using Unicode characters
- **Status indicators** - ✓ ✗ ℹ ⚠ for success/error/info/warning

### Modern UX Features
- **No more number typing** - Pure keyboard navigation
- **Inline descriptions** - Know what each option does
- **Back navigation** - ESC key works everywhere
- **Search-as-you-type** - Just start typing
- **Progress indicators** - Visual feedback for all operations
- **Error recovery** - Graceful error handling with clear messages

## 🎯 Architecture

### Component-Based Design

The new TUI is built with React components for maximum maintainability:

```
src/cli/components/
├── Header.tsx          - Rainbow logo and branding
├── Menu.tsx            - Selectable menu with descriptions
├── SearchInput.tsx     - Type-ahead search input
├── MediaList.tsx       - Browse movies and TV shows
├── SeasonList.tsx      - Select TV show seasons
├── EpisodeList.tsx     - Select episodes
├── HistoryList.tsx     - Continue watching interface
├── LoadingSpinner.tsx  - Animated loading states
├── StatusMessage.tsx   - Success/error/info messages
├── ProgressBar.tsx     - Download/playback progress
└── index.ts            - Component exports
```

### State Management

- **React Hooks** for local component state
- **Centralized app state** in ink-app.tsx
- **Proper state updates** trigger re-renders
- **Navigation stack** with breadcrumbs

### Screen Flow

```
Main Menu
 ├─ Search → Search Input → Results → Media Selection
 ├─ Trending → Media List → Media Selection
 ├─ Recent → Media List → Media Selection
 ├─ Continue Watching → History List → Resume Playback
 └─ Exit

Media Selection (TV Shows)
 └─ Season Selection → Episode Selection → Playback

Media Selection (Movies)
 └─ Direct Playback
```

## 🛠️ Technology Stack

- **Ink** (v6.5.1) - React for terminal UIs
- **ink-select-input** - Arrow-key selectable lists
- **ink-text-input** - Text input with placeholder support
- **ink-spinner** - Loading animations
- **ink-gradient** - Rainbow text effects
- **ink-big-text** - ASCII art text
- **React** (v19.2.1) - Component framework
- **Chalk** (v4.1.2) - Terminal color support

## 📋 Features Comparison

### Before (Old CLI)
```
[1] The Matrix
[2] Inception
[3] Interstellar
[0] Cancel

Select (0-3): _
```
**Problems:**
- Had to type numbers manually
- No visual feedback
- Easy to mistype
- Linear, no back navigation
- Basic ASCII output

### After (New TUI)
```
┌─────────────────────────────────────┐
│ 🎬 TOPSTER | Stream movies & TV     │
└─────────────────────────────────────┘

Main Menu › Search › Results

🔍 Search Results for "matrix"

  ▶ 🎬 The Matrix (1999) [1080p]
    🎬 The Matrix Reloaded (2003) [1080p]
    🎬 The Matrix Revolutions (2003) [720p]
    ← Back

Use ↑↓ arrows to navigate, Enter to select
```
**Benefits:**
- Arrow key navigation
- Rich visual design
- Contextual help text
- Breadcrumb trail
- Quality indicators
- Back navigation

## 🎨 UI Elements

### Colors
- **Cyan** - Headers, selected items, branding
- **Green** - Quality tags, success messages
- **Red** - Error messages
- **Yellow** - Warnings
- **Gray/Dim** - Secondary text, hints
- **Rainbow** - Logo gradient

### Icons
- 🎬 Movies
- 📺 TV Shows / Seasons
- 🔍 Search
- 🔥 Trending
- 🆕 Recent
- 📜 History
- ❌ Exit
- ← Back
- ▶ Selected/Playing
- ✓ Success
- ✗ Error
- ℹ Info
- ⚠ Warning

### Borders
- Rounded box style (`┌─┐└─┘`) for headers
- Clean separation between sections
- Progress bars with `█` and `░` characters

## 🔧 How It Works

### Entry Point

When you run `topster` without arguments, it now launches the interactive TUI:

```typescript
// src/cli/index.ts
// Default: launch interactive TUI
const { runInkApp } = await import('./ink-app.js');
await runInkApp(ctx);
```

### Rendering

Ink renders React components to the terminal:

```typescript
// src/cli/ink-app.tsx
import { render } from 'ink';

export async function runInkApp(ctx: CommandContext) {
  render(<InkApp ctx={ctx} />);
}
```

### State Updates

React's `useState` hook manages screen transitions:

```typescript
const [state, setState] = useState<AppState>({
  screen: 'main-menu',
  breadcrumbs: ['Main Menu'],
});

const updateState = (updates: Partial<AppState>) => {
  setState(prev => ({ ...prev, ...updates }));
};
```

### Navigation

Breadcrumbs track navigation history:

```typescript
Main Menu
Main Menu › Search
Main Menu › Search › Results
Main Menu › Search › Results › The Matrix
Main Menu › Search › Results › The Matrix › Season 1
```

## 🎮 Usage Examples

### Basic Usage
```bash
# Launch interactive TUI
topster

# Direct search (skips TUI)
topster "the matrix"

# Show trending (skips TUI)
topster --trending
```

### Navigation Flow
1. Start app → Main Menu
2. Select "Search" → Search Input
3. Type "matrix" → Results List
4. Select "The Matrix" → Playback
5. After playback → Main Menu

### TV Show Flow
1. Main Menu → Trending
2. Select TV show → Season List
3. Select Season 1 → Episode List
4. Select Episode 1 → Playback
5. ESC at any point → Previous screen

## 🚦 Development

### Adding New Components

```typescript
// src/cli/components/MyComponent.tsx
import React from 'react';
import { Box, Text } from 'ink';

export const MyComponent: React.FC<{ title: string }> = ({ title }) => {
  return (
    <Box>
      <Text color="cyan" bold>{title}</Text>
    </Box>
  );
};
```

### Adding New Screens

Update the `Screen` type and add a case to `renderScreen()`:

```typescript
type Screen =
  | 'main-menu'
  | 'my-new-screen'; // Add here

// In renderScreen()
case 'my-new-screen':
  return <MyNewScreen />;
```

### Testing Components

```bash
bun run build
bun run start
```

## 🐛 Troubleshooting

### Terminal doesn't support colors
Set `FORCE_COLOR=0` to disable colors

### Arrow keys not working
Ensure your terminal supports ANSI escape codes

### UI looks broken
Try resizing terminal or use a modern terminal emulator (iTerm2, Alacritty, Windows Terminal)

### React errors
Check that all components have proper key props in lists

## 🎯 Future Enhancements

Potential additions for even more modern UX:

- [ ] Fuzzy search with live filtering
- [ ] Image previews in terminal (iTerm2/Kitty)
- [ ] Mouse support for click navigation
- [ ] Split-pane layout (list + details)
- [ ] Vim-style keybindings (j/k navigation)
- [ ] Custom color themes
- [ ] Configurable keyboard shortcuts
- [ ] Multi-selection for batch operations
- [ ] Search history with autocomplete
- [ ] Filter/sort options for lists
- [ ] Full-screen mode toggle

## 📚 Resources

- [Ink Documentation](https://github.com/vadimdemedes/ink)
- [React Documentation](https://react.dev/)
- [Terminal UI Design Patterns](https://github.com/rothgar/awesome-tuis)

---

**Welcome to the future of CLI streaming! 🚀**
