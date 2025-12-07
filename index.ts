#!/usr/bin/env bun
// Topster - TypeScript streaming CLI with MCP support
// Main entry point

import { runCli } from './src/cli';
import { autoUpdate } from './src/core/updater';

// Check for updates in the background (non-blocking)
autoUpdate(true).catch(() => {
  // Silently fail - don't interrupt startup
});

runCli().catch((error) => {
  console.error('Fatal error:', error);
  process.exit(1);
});
