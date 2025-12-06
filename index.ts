#!/usr/bin/env bun
// Topster - TypeScript streaming CLI with MCP support
// Main entry point

import { runCli } from './src/cli';

runCli().catch((error) => {
  console.error('Fatal error:', error);
  process.exit(1);
});
