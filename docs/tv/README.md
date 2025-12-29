# Topster for Google TV

Android TV port of Topster - stream movies and TV shows on your Google TV from the comfort of your couch.

## Architecture

This is a native Android TV app that embeds a Bun server to reuse all the existing TypeScript scraping and video extraction logic:

```
┌─────────────────────────────────────┐
│     Android TV App (Kotlin)        │
│  ┌───────────────────────────────┐ │
│  │  Jetpack Compose for TV UI   │ │
│  │  ExoPlayer (Video Playback)  │ │
│  └──────────┬────────────────────┘ │
│             │ HTTP (localhost:3847) │
│  ┌──────────▼────────────────────┐ │
│  │   Embedded Bun Server        │ │
│  │   (TypeScript Scrapers)      │ │
│  └──────────────────────────────┘ │
└─────────────────────────────────────┘
```

**Benefits:**
- 100% code reuse of scraper/extractor logic from main Topster codebase
- No need to port TypeScript to Kotlin/Java
- Easy updates - just rebuild the server bundle
- TV-optimized UI with D-pad navigation

## Prerequisites

### On Gentoo (for building)

```bash
# Android SDK (for building APKs)
doas emerge dev-util/android-sdk
# OR
doas emerge dev-util/android-studio

# Bun (for bundling server code)
# Already installed if you're using Topster CLI

# Gradle wrapper is included, no need to install globally
```

### For Google TV (for installation)

- Google TV device with Android 5.0+ (API 21+)
- ADB debugging enabled on the TV

## Build Instructions

### Step 1: Bundle the Server Code

```bash
cd tv/server
./build.sh
```

This will:
- Bundle `bundle.ts` and all dependencies
- Output to `android/app/src/main/assets/server/`
- Copy required node_modules

### Step 2: Download Bun ARM64 Binary

The Android app needs the Bun runtime binary for ARM64 architecture:

```bash
cd ../android
mkdir -p app/src/main/assets/bun

# Download latest Bun for ARM64 Linux
wget https://github.com/oven-sh/bun/releases/latest/download/bun-linux-aarch64.zip

# Extract
unzip bun-linux-aarch64.zip

# Move binary to assets
mv bun-linux-aarch64/bun app/src/main/assets/bun/

# Cleanup
rm -rf bun-linux-aarch64*
```

### Step 3: Build the APK

```bash
# Build debug APK (unsigned)
./gradlew assembleDebug

# Output location:
# app/build/outputs/apk/debug/app-debug.apk
```

**Note:** On Gentoo, make sure `ANDROID_HOME` is set:
```bash
export ANDROID_HOME="/opt/android-sdk"
# Or wherever your Android SDK is installed
```

## Installation to Google TV

### Enable ADB on Google TV

1. Go to **Settings** → **System** → **About**
2. Click on **Build** 7 times to enable Developer Options
3. Go back to **Settings** → **System** → **Developer Options**
4. Enable **Network debugging** (note the IP address shown)

### Install via ADB

```bash
# Connect to your TV (replace with your TV's IP)
adb connect 192.168.1.100:5555

# Verify connection
adb devices

# Install the APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch the app
adb shell am start -n com.topster.tv/.MainActivity
```

### View Logs (for debugging)

```bash
# Watch app logs
adb logcat -s TopsterApplication BunServerManager TopsterApiClient

# Watch Bun server output
adb logcat -s BunServerManager:I | grep "\[Server\]"
```

## App Usage

### First Launch

When you first open the app, you'll see a loading screen while the Bun server starts. This takes about 5-10 seconds:

1. App extracts Bun binary from APK (~80MB)
2. App extracts server bundle
3. Bun server starts on localhost:3847
4. App verifies server health
5. Home screen appears

### Navigation

- **D-Pad**: Navigate between items
- **Center/OK**: Select item
- **Back**: Go back / Exit app

### Features

**Currently Implemented:**
- ✅ Browse trending movies and TV shows
- ✅ Browse recent movies and TV shows
- ✅ Embedded Bun server with auto-start
- ✅ TV-optimized card layout
- ✅ Focus management for D-pad navigation

**To Be Implemented:**
- ⏳ Search functionality
- ⏳ Details screen with season/episode selection
- ⏳ Video player with ExoPlayer
- ⏳ Watch history and resume playback
- ⏳ Subtitle support
- ⏳ Quality selection
- ⏳ Settings screen

## Development Roadmap

The core infrastructure is complete. Here's what needs to be built:

### 1. Search Screen
**Location:** `app/src/main/java/com/topster/tv/ui/screens/SearchScreen.kt`

- TV keyboard input
- Real-time search results
- Type filter (Movies/TV/All)

### 2. Details Screen
**Location:** `app/src/main/java/com/topster/tv/ui/screens/DetailsScreen.kt`

**For Movies:**
- Title, poster, rating, description
- Quality selector
- Play button → fetch video sources → navigate to player

**For TV Shows:**
- Season selector
- Episode list with resume indicators
- Episode metadata (number, title)

### 3. Player Screen
**Location:** `app/src/main/java/com/topster/tv/ui/screens/PlayerScreen.kt`

**Critical Components:**
- ExoPlayer with HLS support
- **Must include HTTP headers** (User-Agent, Referer) to avoid 403 errors
- Subtitle integration from VideoInfo.subtitles
- Playback controls
- Progress tracking → call `updateHistory()` on exit

**Example ExoPlayer setup:**
```kotlin
val dataSourceFactory = DefaultHttpDataSource.Factory()
    .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
    .setDefaultRequestProperties(mapOf(
        "Referer" to (videoSource.referer ?: ""),
        "Accept" to "*/*"
    ))

val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
    .createMediaSource(MediaItem.fromUri(videoUrl))
```

### 4. History Screen
**Location:** `app/src/main/java/com/topster/tv/ui/screens/HistoryScreen.kt`

- List of watched content
- Resume from last position
- Clear individual entries

### 5. Settings Screen
**Location:** `app/src/main/java/com/topster/tv/ui/screens/SettingsScreen.kt`

- Base URL (FlixHQ mirror)
- Default quality preference
- Subtitle language
- Clear all history
- About/Version info

### 6. Navigation
**Location:** `app/src/main/java/com/topster/tv/navigation/NavGraph.kt`

Set up Jetpack Navigation for TV:
- Home → Details → Player
- Home → Search → Details
- Home → History → Player
- Home → Settings

## Troubleshooting

### Server Won't Start

**Check logs:**
```bash
adb logcat -s BunServerManager:* | grep -E "(Starting|Error|Failed)"
```

**Common issues:**
- Bun binary not executable: APK might be corrupt, rebuild
- Server bundle missing: Re-run `tv/server/build.sh`
- Port 3847 already in use: Restart the app

### "Failed to load content" Error

**Possible causes:**
1. Server not running yet (wait a few more seconds)
2. FlixHQ website down/blocked
3. Network connectivity issue

**Debug:**
```bash
# Test server health from ADB
adb shell "curl http://localhost:3847/health"

# Should return: {"status":"ok","server":{...}}
```

### Video Playback 403 Error

This means HTTP headers are not being sent correctly.

**Verify in PlayerScreen:**
- User-Agent must be set
- Referer header from VideoInfo.referer must be included
- Check ExoPlayer DataSource.Factory configuration

### APK Size Too Large

The APK will be ~100MB due to:
- Bun runtime: ~80MB
- Server bundle + node_modules: ~20MB

To reduce size:
- Use ProGuard in release build (already configured)
- Only bundle essential node_modules in build.sh

## File Structure

```
tv/
├── android/                      # Android TV app
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/topster/tv/
│   │   │   │   ├── MainActivity.kt               ✅
│   │   │   │   ├── TopsterApplication.kt         ✅
│   │   │   │   ├── BunServerManager.kt           ✅
│   │   │   │   ├── api/
│   │   │   │   │   ├── TopsterApiClient.kt       ✅
│   │   │   │   │   ├── models/                   ✅
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── HomeScreen.kt         ✅
│   │   │   │   │   │   ├── SearchScreen.kt       ⏳
│   │   │   │   │   │   ├── DetailsScreen.kt      ⏳
│   │   │   │   │   │   ├── PlayerScreen.kt       ⏳
│   │   │   │   │   │   ├── HistoryScreen.kt      ⏳
│   │   │   │   │   │   ├── SettingsScreen.kt     ⏳
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── MediaCard.kt          ✅
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   ├── NavGraph.kt           ⏳
│   │   │   ├── assets/
│   │   │   │   ├── bun/bun                       (download manually)
│   │   │   │   ├── server/                       (built by build.sh)
│   │   │   ├── AndroidManifest.xml               ✅
│   │   ├── build.gradle.kts                      ✅
│   ├── build.gradle.kts                          ✅
│   ├── settings.gradle.kts                       ✅
├── server/
│   ├── bundle.ts                                 ✅
│   ├── build.sh                                  ✅
│   ├── package.json                              ✅
└── README.md                                     ✅

✅ = Implemented
⏳ = To be implemented
```

## Contributing

To add new features:

1. Update server code in `tv/server/bundle.ts` if needed
2. Rebuild server bundle: `cd tv/server && ./build.sh`
3. Add Kotlin UI code in `app/src/main/java/com/topster/tv/ui/`
4. Rebuild APK: `cd tv/android && ./gradlew assembleDebug`
5. Test on TV: `adb install -r app/build/outputs/apk/debug/app-debug.apk`

## License

Same as main Topster project.

## Support

For issues specific to the TV app, check:
- ADB logs: `adb logcat`
- Server logs: Look for `[Server]` tags
- API errors: Look for `TopsterApiClient` tags

For scraper/extraction issues, these are shared with the main CLI - report in the main Topster repository.
