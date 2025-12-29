# Topster Documentation

Welcome to the Topster documentation. This is your comprehensive guide to the project.

## Quick Links

### Platform-Specific Documentation
- **CLI App**: [Architecture Overview](architecture/overview.md) | [Getting Started](guides/getting-started.md)
- **Web App**: [Getting Started](guides/getting-started.md)
- **Android TV**: [Getting Started](tv/README.md) | [Architecture](architecture/android-tv.md)

### Feature Documentation
- [Watch History](features/history.md) - How history tracking works across all platforms
- [Filtering System](features/filtering.md) - Media type, year range, and quality filters
- [Player Configuration](features/player.md) - Customizable player controls and settings

### Development Guides
- [Getting Started](guides/getting-started.md) - Quick setup for all platforms
- [Android TV Development](guides/android-development.md) - Android TV specific dev guide

## Repository Structure

```
topster/
├── src/                    # Source code
│   ├── cli/             # CLI application
│   ├── core/            # Core types, errors, config
│   ├── modules/          # Feature modules (scraper, player, history, MCP)
│   ├── mcp/             # Model Context Protocol server
│   └── utils/           # Shared utilities
├── tv/                     # Android TV application
│   └── android/         # Android TV app code
├── site/                   # Next.js web application
├── drizzle/                # Database migrations
└── docs/                   # This directory (all documentation)
```

## Key Technologies

### Core Platform
- **Runtime**: Bun (JavaScript/TypeScript)
- **CLI Framework**: Ink (React for terminal UI)
- **Database**: SQLite (via bun:sqlite) with Drizzle ORM
- **MCP**: Model Context Protocol server integration

### Web Platform
- **Framework**: Next.js 14+ with App Router
- **UI**: React 18+ with shadcn/ui components
- **Styling**: Tailwind CSS
- **State**: Zustand (optional) or React Context

### Android TV Platform
- **Framework**: Jetpack Compose
- **Language**: Kotlin
- **Architecture**: MVVM with ViewModels
- **Player**: ExoPlayer (Media3)
- **Database**: Room (SQLite) with Drizzle ORM
- **Linting**: Android Lint + Detekt (strict mode)

### API Integration
- **IMDb**: OMDb API (ratings, cast, plot metadata)
- **Scrapers**: Multiple extractors for video sources
- **Caching**: Aggressive caching for fast lookups

## Development Workflow

### Setup
```bash
# Install dependencies
bun install

# Start CLI development server
bun run dev

# Start web development server
cd site && bun run dev

# Build all platforms
bun run build && bun test && bun run typecheck
```

### Testing
```bash
# Run all tests
bun test

# Run specific test file
bun test src/modules/scraper/http.test.ts

# Run type checking
bun run typecheck

# Android TV tests
cd tv/android
./gradlew testStableDebugUnitTest
./gradlew lintStableDebug
```

### Code Quality
- **Strict TypeScript**: No `any` types, full type safety
- **Linting**: Android Lint (fail on errors), Detekt (code quality)
- **Code Style**: Follow AGENTS.md guidelines for each platform
- **Documentation**: Keep docs in sync with code changes

## Common Issues

### CLI App
- **Issue**: History shows duplicates
- **Solution**: See [History Feature](features/history.md) for hierarchical navigation

### Web App
- **Issue**: Build fails with type errors
- **Solution**: Run `bun run typecheck` before committing
- **Issue**: Components don't render properly
- **Solution**: Ensure all required props are passed

### Android TV
- **Issue**: Lint fails with strict rules
- **Solution**: Fix Detekt issues before committing
- **Issue**: Detekt not configured correctly
- **Solution**: Use `./gradlew detektStableDebug` to run checks

## Resources

### Getting Help
- **Report Bugs**: [GitHub Issues](https://github.com/4cecoder/topster/issues)
- **Feature Requests**: Use GitHub issues with "enhancement" label
- **Discussions**: [GitHub Discussions](https://github.com/4cecoder/topster/discussions)

### Documentation Maintenance
When adding new features:
1. Update relevant feature documentation
2. Add examples to getting started guides
3. Update architecture docs if structure changes
4. Keep AGENTS.md up to date with new patterns

---

**Last Updated**: December 29, 2025
