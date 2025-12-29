#!/usr/bin/env bun

/**
 * Bun-native server for Topster Web App
 * This uses Bun.serve() directly for optimal performance
 * Falls back to Next.js for routing
 */

import { $ } from "bun";

const port = process.env.PORT || 3000;
const dev = process.env.NODE_ENV !== "production";

console.log(`
🚀 Starting Topster Web App with Bun
   Mode: ${dev ? "development" : "production"}
   Port: ${port}
   Runtime: Bun ${Bun.version}
`);

if (dev) {
  // Development: Use Next.js dev server with Bun runtime
  await $`bun --bun next dev -p ${port}`;
} else {
  // Production: Use Next.js production server with Bun runtime
  console.log("Building Next.js app...");
  await $`bun --bun next build`;

  console.log("Starting production server...");
  await $`bun --bun next start -p ${port}`;
}
