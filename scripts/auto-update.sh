#!/usr/bin/env bash
# Auto-update script for Topster
# Quietly pulls latest from main and rebuilds

set -e

REPO_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$REPO_DIR"

# Check if we're in a git repo
if [ ! -d .git ]; then
    echo "Error: Not a git repository"
    exit 1
fi

# Stash any local changes
if ! git diff-index --quiet HEAD --; then
    echo "Stashing local changes..."
    git stash push -m "auto-update-stash-$(date +%s)" >/dev/null 2>&1
    STASHED=1
fi

# Fetch and pull latest from main
echo "Checking for updates..."
git fetch origin main --quiet

# Check if we're behind
LOCAL=$(git rev-parse HEAD)
REMOTE=$(git rev-parse origin/main)

if [ "$LOCAL" = "$REMOTE" ]; then
    echo "Already up to date!"
    exit 0
fi

echo "Update available, pulling changes..."
git pull origin main --quiet

# Rebuild
echo "Rebuilding..."
bun install --quiet 2>/dev/null
bun run build >/dev/null 2>&1

echo "✓ Updated successfully to $(git rev-parse --short HEAD)"

# Restore stash if we created one
if [ -n "$STASHED" ]; then
    echo "Restoring local changes..."
    git stash pop >/dev/null 2>&1 || true
fi
