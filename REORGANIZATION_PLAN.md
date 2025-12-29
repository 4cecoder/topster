# Repository Reorganization Plan

## Problems Identified

### 1. Documentation Issues
- **20+ scattered markdown files** across root level, docs/, tv/docs/
- **Duplicate READMEs**: Root README.md + tv/README.md + site/README.md
- **Fragmented feature docs**: IMDB feature split across 3 files
- **No clear hierarchy**: Users can't find information easily
- **Redundant content**: Similar info repeated across multiple docs

### 2. Code Organization Issues
- **No unified entry point**: Different entry points for CLI, Web, Android
- **Build artifacts**: node_modules/, dist/, build/ should be gitignored
- **Missing architecture doc**: No single source of truth for structure

## Proposed Solution

### Phase 1: Consolidate Documentation

**New Structure:**
```
docs/
├── README.md (main docs index)
├── architecture/
│   ├── overview.md
│   ├── cli.md
│   ├── web.md
│   └── android-tv.md
├── features/
│   ├── history.md
│   ├── filtering.md
│   ├── player.md
│   └── mcp.md
├── guides/
│   ├── getting-started.md
│   ├── android-development.md
│   └── deployment.md
└── api/
    └── database.md
```

**Files to Remove:**
- `docs/imdb-feature.md`
- `docs/IMDB_FEATURE_SUMMARY.md`
- `docs/imdb-onboarding-flow.md`
- `docs/filters-feature.md`
- `tv/README.md`
- `tv/docs/` (all files)
- `tv/IMPROVEMENTS.md`
- `tv/QUICKSTART.md`
- `tv/INSTALL_GUIDE.md`
- `tv/ARCHITECTURE.md`
- `ANDROID_PLAYER_UPGRADE.md`
- `TUI_UPGRADE.md`
- `WEB_APP.md`
- `SCREEN_CASTING.md`
- `FIXES.md`
- `TROUBLESHOOTING.md`
- `AUTO_UPDATE.md`
- `SUITE_OVERVIEW.md`
- `QUICK_START.md`
- `CLAUDE.md`

**Files to Create (consolidated):**
- `docs/README.md` - Main docs index with links
- `docs/architecture/overview.md` - High-level architecture
- `docs/features/history.md` - History feature (combine docs)
- `docs/features/filtering.md` - Filtering system
- `docs/features/player.md` - Player customization
- `docs/guides/getting-started.md` - Quick start guide
- `docs/guides/android-development.md` - Android TV dev guide

### Phase 2: Improve .gitignore

**Add to .gitignore:**
```
# Build artifacts
node_modules/
dist/
build/
.next/
tv/android/app/build/
tv/android/.gradle/
tv/android/app/.idea/

# IDE
.vscode/
.idea/
*.iml

# OS
.DS_Store
Thumbs.db
```

### Phase 3: Create Architecture Documentation

**Single source of truth:**
- `ARCHITECTURE.md` at root level
- Clear module boundaries
- Data flow diagrams
- Technology stack overview

### Phase 4: Code Duplication Review

**Check for duplication between:**
- CLI and Web sites (components, types)
- Shared utilities across modules
- Database access patterns

## Execution Order

1. **Create new documentation structure**
2. **Consolidate and remove old docs**
3. **Update .gitignore**
4. **Create ARCHITECTURE.md**
5. **Update main README.md** with clear structure
6. **Commit and verify** build still works
