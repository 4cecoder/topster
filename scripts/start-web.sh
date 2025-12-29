#!/usr/bin/env bash

# Start script for Topster Web App
# This script starts both the MCP server and the Next.js web app using Bun

set -e

echo "🚀 Starting Topster Web App with Bun"
echo "   Bun version: $(bun --version)"
echo

# Check if Bun is installed
if ! command -v bun &> /dev/null; then
    echo "❌ Bun is not installed!"
    echo "   Install it from: https://bun.sh"
    exit 1
fi

# Check if MCP server is already running
if lsof -Pi :3847 -sTCP:LISTEN -t >/dev/null 2>&1; then
    echo "✅ MCP server is already running on port 3847"
else
    echo "⚡ Starting MCP server with Bun..."
    bun run index.ts --mcp &
    MCP_PID=$!
    echo "✅ MCP server started (PID: $MCP_PID)"

    # Save PID for later cleanup
    echo $MCP_PID > .mcp.pid

    # Wait a bit for server to start
    sleep 2
fi

echo
echo "⚡ Starting Next.js web app with Bun runtime..."
cd site
bun run dev

# Cleanup on exit
cleanup() {
    echo
    echo "🛑 Shutting down..."
    if [ -f ../.mcp.pid ]; then
        kill $(cat ../.mcp.pid) 2>/dev/null || true
        rm ../.mcp.pid
        echo "✅ MCP server stopped"
    fi
}

trap cleanup EXIT
