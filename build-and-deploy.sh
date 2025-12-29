#!/bin/bash
set -e

echo "╔══════════════════════════════════════════════════════════════╗"
echo "║         Topster TV - Build & Deploy                          ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# Build APK
echo "🔨 Building APK..."
cd tv/android
./gradlew assembleDebug
cd ../..

# Deploy
echo ""
echo "🚀 Deploying to TV..."
bun deploy-to-tv.ts

echo ""
echo "✅ Done!"
