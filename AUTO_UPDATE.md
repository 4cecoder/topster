# Auto-Update System

Topster includes a simple, native auto-update system that keeps your installation up-to-date with the latest changes from the main branch.

## How It Works

- **Automatic Background Updates**: On startup, Topster quietly checks for updates once every 24 hours
- **Git-Based**: Pulls latest changes from `origin/main`
- **Zero Configuration**: Works out of the box if installed via git clone
- **Non-Blocking**: Update checks happen in the background and never interrupt your workflow
- **Smart Stashing**: Automatically preserves your local changes during updates

## Manual Update

Force an update check at any time:

```bash
topster --update
# or
topster -u
```

## What Happens During Update

1. **Preflight Checks**:
   - Verifies `git` is installed and in PATH
   - Verifies `bun` is installed and in PATH
   - Confirms running from a git repository
2. **Update Check**:
   - Fetches latest from `origin/main`
   - Compares local vs remote commit hashes
3. **If update available**:
   - Saves current commit hash for rollback
   - Stashes local changes (if any)
   - Pulls latest code
   - Runs `bun install` (with error checking)
   - Runs `bun run build` (with error checking)
   - Restores stashed changes
4. **Rollback on Failure**:
   - If install/build fails, automatically rolls back to previous commit
   - Restores local changes
5. Reports update status

## Implementation

The updater is implemented in pure TypeScript using Bun's native APIs:
- `src/core/updater.ts` - Core update logic
- Integrated into `index.ts` for automatic checks
- Manual trigger via CLI command

No external dependencies, no GitHub Actions, no version numbers - just simple git operations.

## Disable Auto-Updates

Auto-updates are non-intrusive, but if you want to disable them:

1. Comment out the auto-update line in `index.ts`
2. Or remove the `.topster/last-update-check` file to reset the timer

## Requirements

- `git` - Must be installed and in PATH
- `bun` - Must be installed and in PATH
- Git repository - Must be cloned from GitHub (not npm/bun installs)

## Notes

- Updates require git repository (not available from npm/bun installs)
- Checks are rate-limited to once per 24 hours
- Failed updates are silently ignored to avoid disruption
- Local changes are preserved via git stash
- Automatic rollback on build failures ensures stability
