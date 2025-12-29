# AGENTS.md - Coding Guidelines for Topster

This document provides coding guidelines, build commands, and conventions for the Topster codebase.

## Build, Lint, and Test Commands

### Core Application (CLI)
**LSP & Linting**: TypeScript LSP (VS Code built-in), Bun type checker
```bash
bun run dev              # Development server with hot reload
bun run build            # Production build
bun run typecheck        # Type checking
bun test                 # Run all tests
bun test <path>          # Run specific test file (e.g., src/modules/scraper/http.test.ts)
bun run db:generate      # Generate migration files
bun run db:migrate       # Run migrations
```

### Web Application (Next.js)
**LSP & Linting**: TypeScript LSP, ESLint (Next.js config)
```bash
cd site
bun run dev              # Development server
bun run build            # Production build
bun run start            # Start production server
bun run lint             # Lint code
bun run preview          # Preview production build
```

### Android TV Application
```bash
cd tv/android

# Run unit tests (stable variant)
./gradlew testStableDebugUnitTest --stacktrace

# Run unit tests (beta variant)
./gradlew testBetaDebugUnitTest --stacktrace

# Run Android lint (stable variant)
./gradlew lintStableDebug --stacktrace

# Run Android lint (beta variant)
./gradlew lintBetaDebug --stacktrace

# Run Detekt (Kotlin code quality)
./gradlew detektStableDebug
./gradlew detektBetaDebug

# Build debug APK
./gradlew assembleStableDebug

# Build debug APK for testing
./gradlew assembleBetaDebug

# Build release APK
./gradlew assembleStableRelease

# Run instrumented tests (requires connected device/emulator)
./gradlew connectedStableDebugAndroidTest
```

**Android TV Features:**
- **Filtering**: Media filtering by type (Movies/TV Shows), year range, quality
- **History**: Hierarchical navigation with grouped shows, season/episode drill-down
- **Player**: Compose-based with customizable controls (See ComposePlayerScreen.kt)
- **Settings**: PlayerSettingsScreen.kt for button configuration

### General
```bash
bun install              # Install dependencies
rm -rf dist drizzle      # Clean build artifacts
bun test && bun run typecheck  # Full test suite
```

## Code Style Guidelines

### TypeScript Configuration
- **Strict mode**: All TypeScript strict flags active
- **No `any` types**: Use proper types, `unknown`, or generics
- **File extensions**: Use `.js` extensions in imports for TypeScript files
- **Type imports**: Import types explicitly with `import type`

### Runtime Environment (Bun)
Use Bun APIs instead of Node.js:
- `Bun.serve()` instead of Express
- `bun:sqlite` instead of `better-sqlite3`
- `WebSocket` (built-in) instead of `ws`
- `Bun.file()` instead of `node:fs`

### Error Handling
Extend `TopsterError` from `src/core/errors.ts`:
- `NetworkError` - Network-related failures
- `ScrapingError` - Web scraping failures
- `ConfigError` - Configuration issues
- `PlayerError` - Media player errors
- `NoResultsError` - Empty search results
- `DecryptionError` - Decryption failures
- `HLSError` - HLS streaming errors
- `HistoryError` - History database errors
- `DownloadError` - Download failures
- `MCPError` - MCP integration errors

Use `formatError()` helper for consistent error messages.

### Naming Conventions
- Variables/Functions: `camelCase` (prefix booleans with `is`, `has`, `can`, `should`)
- Types/Interfaces/Classes/Enums: `PascalCase`
- Files: `kebab-case` (except React components: `PascalCase`)
- Directories: `kebab-case`

### Import Organization
Use relative imports (`./` or `../`). Group imports:
```typescript
import type { MediaItem } from '../../core/types.js';
import { NetworkError } from '../../core/errors.js';
import React from 'react';
import { Box, Text } from 'ink';
```

### React Components (CLI with Ink)
```typescript
interface MediaListProps {
  items: MediaItem[];
  onSelect: (item: MediaItem) => void;
}

export const MediaList: React.FC<MediaListProps> = ({ items, onSelect }) => {
  const [selectedIndex, setSelectedIndex] = React.useState(0);
  const filteredItems = React.useMemo(() => items.filter(item => item.type === 'movie'), [items]);
  return <Box>{/* JSX */}</Box>;
};
```

### React Components (Web with Next.js)
```typescript
'use client';

interface MediaCardProps {
  media: MediaItem;
}

export function MediaCard({ media }: MediaCardProps) {
  return <Link href={`/watch/${media.id}`} className="...">{/* JSX */}</Link>;
}
```

### Database (Drizzle ORM)
- Schema definitions in `src/modules/*/schema.ts`
- Generate and run migrations for schema changes
- Use Drizzle query builder for type-safe operations

### Security Practices
- Validate all user inputs and API responses
- Sanitize HTTP headers (remove `\r\n` and `\0`)
- Block localhost and private IP ranges for SSRF protection
- Parse JSON with size limits and prototype pollution checks

### Testing (Bun)
- Use `bun test` with `describe` and `test` blocks
- Test files: `*.test.ts` or `*.spec.ts`
- Mocking: Use Bun's built-in capabilities
```typescript
import { describe, test, expect } from 'bun:test';

describe('HTTP Client', () => {
  test('should validate HTTPS URLs', () => {
    expect(isValidUrl('https://example.com')).toBe(true);
  });
});
```

### File Structure
```
src/
├── cli/           # Command-line interface (Ink components)
├── core/          # Core types, errors, config
├── modules/       # Feature modules (scraper, player, etc.)
└── mcp/           # Model Context Protocol integration

site/              # Next.js web application
drizzle/           # Database migrations
```

### Performance
- Use Bun's built-in bundling
- Lazy load modules when needed
- Implement caching strategies
- Clean up resources and event listeners

### Development Workflow
1. Create feature branches from `main`
2. Write tests for new functionality
3. Run `bun run typecheck` and linting
4. Commit with descriptive messages (`type(scope): description`)
5. Create pull requests for review

Always run `bun test && bun run typecheck` before pushing changes.
