# AGENTS.md - Coding Guidelines for Topster

This document provides coding guidelines, build commands, and conventions for the Topster codebase. Follow these guidelines when making changes to maintain code quality and consistency.

## Build, Lint, and Test Commands

### Core Application (CLI)
```bash
# Development server with hot reload
bun run dev

# Production build
bun run build

# Type checking
bun run typecheck

# Run all tests
bun test

# Run specific test file
bun test src/modules/scraper/http.test.ts

# Run tests with coverage (if configured)
bun test --coverage

# Database operations
bun run db:generate  # Generate migration files
bun run db:migrate   # Run migrations
```

### Web Application (Next.js)
```bash
cd site

# Development server
bun run dev

# Production build
bun run build

# Start production server
bun run start

# Lint code
bun run lint

# Preview production build
bun run preview
```

### General Commands
```bash
# Install dependencies
bun install

# Clean build artifacts
rm -rf dist drizzle

# Full test suite with type checking
bun test && bun run typecheck
```

## Code Style Guidelines

### TypeScript Configuration
- **Strict mode enabled**: All TypeScript strict flags are active
- **No `any` types**: Use proper types, `unknown`, or generics
- **Explicit imports**: Import types explicitly with `import type`
- **File extensions**: Use `.js` extensions in import statements for TypeScript files

### Runtime Environment
- **Bun runtime**: Use Bun APIs instead of Node.js equivalents:
  - `Bun.serve()` instead of Express
  - `bun:sqlite` instead of `better-sqlite3`
  - `Bun.redis` instead of `ioredis`
  - `Bun.sql` instead of `pg` or `postgres.js`
  - `WebSocket` (built-in) instead of `ws`
  - `Bun.file` instead of `node:fs`

### Error Handling
- **Custom error classes**: Extend `TopsterError` from `src/core/errors.ts`
- **Specific error types**: Use appropriate error classes (NetworkError, ScrapingError, etc.)
- **Error propagation**: Let errors bubble up, catch and handle at appropriate levels
- **Error formatting**: Use `formatError()` helper for consistent error messages

```typescript
import { NetworkError, formatError } from '../core/errors';

try {
  await fetchData();
} catch (error) {
  if (error instanceof NetworkError) {
    console.error(formatError(error));
    return;
  }
  throw error;
}
```

### Naming Conventions

#### Variables and Functions
- `camelCase` for variables, functions, and methods
- Descriptive names that indicate purpose
- Boolean variables prefixed with `is`, `has`, `can`, `should`

```typescript
const isValidUrl: boolean;
const hasResults: boolean;
function fetchHtml(url: string): Promise<string>
```

#### Types and Interfaces
- `PascalCase` for interfaces, types, classes, and enums
- Suffix interfaces with descriptive names when needed

```typescript
interface MediaItem {
  id: string;
  title: string;
}

type MediaType = 'movie' | 'tv';

class NetworkError extends TopsterError {
  // ...
}
```

#### Files and Directories
- `kebab-case` for file names (except components)
- `PascalCase` for React component files
- Group related files in directories

### Import Organization
- **Relative imports**: Use relative paths with `./` or `../`
- **Type imports**: Separate type-only imports
- **Group imports**: Group by external libraries, then internal modules

```typescript
import type { MediaItem, MediaType } from '../../core/types';
import { NetworkError } from '../../core/errors';
import React from 'react';
import { Box, Text } from 'ink';
```

### React Components (CLI with Ink)
- **Functional components**: Use function declarations
- **Props interface**: Define props types explicitly
- **Hooks**: Use React hooks for state management
- **Memoization**: Use `React.useMemo` for expensive computations

```tsx
interface MediaListProps {
  items: MediaItem[];
  onSelect: (item: MediaItem) => void;
}

export const MediaList: React.FC<MediaListProps> = ({ items, onSelect }) => {
  const [selectedIndex, setSelectedIndex] = React.useState(0);

  const filteredItems = React.useMemo(() => {
    return items.filter(item => item.type === 'movie');
  }, [items]);

  return (
    <Box>
      {/* Component JSX */}
    </Box>
  );
};
```

### Database (Drizzle ORM)
- **Schema definitions**: Define in `src/modules/*/schema.ts`
- **Migrations**: Generate and run migrations for schema changes
- **Queries**: Use Drizzle's query builder for type-safe operations

### Security Practices
- **Input validation**: Validate all user inputs and API responses
- **Header sanitization**: Clean HTTP headers to prevent injection
- **SSRF protection**: Block localhost and private IP ranges
- **JSON safety**: Parse JSON securely with size limits and prototype pollution checks

### Testing
- **Bun test runner**: Use `bun test` with `describe` and `test` blocks
- **Test file naming**: `*.test.ts` or `*.spec.ts`
- **Test organization**: Group related tests in describe blocks
- **Mocking**: Use Bun's built-in mocking capabilities

```typescript
import { describe, test, expect } from 'bun:test';

describe('HTTP Client', () => {
  test('should validate HTTPS URLs', () => {
    // Test implementation
  });
});
```

### Code Comments
- **JSDoc**: Use for public APIs and complex functions
- **Inline comments**: Explain complex logic, not obvious code
- **TODO comments**: Mark areas needing improvement

### Commit Messages
- **Format**: `type(scope): description`
- **Types**: `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`
- **Description**: Start with lowercase, be descriptive

### File Structure
```
src/
├── cli/           # Command-line interface components
├── core/          # Core types, errors, config
├── modules/       # Feature modules (scraper, player, etc.)
├── mcp/           # Model Context Protocol integration
└── tv/            # Android TV application

site/              # Next.js web application
drizzle/           # Database migrations
```

### Performance Considerations
- **Bundle optimization**: Use Bun's built-in bundling
- **Lazy loading**: Load modules only when needed
- **Caching**: Implement appropriate caching strategies
- **Memory management**: Clean up resources and event listeners

### Development Workflow
1. **Branch**: Create feature branches from `main`
2. **Test**: Write tests for new functionality
3. **Lint**: Run linting before committing
4. **Type check**: Ensure TypeScript compilation passes
5. **Commit**: Use descriptive commit messages
6. **PR**: Create pull requests for review

Remember to run `bun test && bun run typecheck` before pushing changes to ensure code quality.</content>
<parameter name="filePath">AGENTS.md