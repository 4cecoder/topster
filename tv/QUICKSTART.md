# Topster Android TV - Quick Start Guide

## 🚀 Get Started in 5 Minutes

### Prerequisites

- Android Studio (latest version)
- Android TV device or emulator
- Java 17+
- Android SDK 34

### 1. Build the App

```bash
cd tv/android

# Build debug APK
./gradlew assembleStableDebug

# Or build all variants
./gradlew assemble
```

**Output**: `app/build/outputs/apk/stable/debug/app-stable-arm64-v8a-debug.apk`

### 2. Install on TV

**Via ADB**:
```bash
# Connect to TV
adb connect <tv-ip-address>:5555

# Install APK
adb install app/build/outputs/apk/stable/debug/*.apk

# Launch app
adb shell am start -n com.topster.tv.stable/.MainActivity
```

**Via Android Studio**:
1. Open project in Android Studio
2. Select TV device from device dropdown
3. Click Run (▶️)

### 3. First Launch

When you first launch the app:

1. **Device Registration** - App registers with web server (if running)
2. **Content Prefetch** - Trending content loads in background
3. **Cache Setup** - Image and media caches initialized

Check logcat for initialization:
```
I/TopsterApplication: Topster TV Application starting...
I/TopsterApplication: Application initialized successfully
I/TopsterApplication: OTA Server started on port 8765
I/TopsterApplication: Device registered with web app
D/TopsterApplication: Trending content prefetched
```

### 4. Test Features

**Search & Browse**:
- Navigate with D-pad
- Search for "Breaking Bad" or any show
- Press select to view details

**Video Playback**:
- Select an episode
- Video loads with resume position
- Use media keys for playback control

**Remote Controls**:
- Play/Pause: Media play/pause button
- Jump Forward: Media fast-forward (+10s)
- Jump Backward: Media rewind (-10s)
- Speed Up: R1 button (or configure)
- Speed Down: L1 button (or configure)

**Picture-in-Picture**:
- Press Home button during playback
- Video continues in PiP window
- Return to app to exit PiP

## 🧪 Testing

### Run Unit Tests

```bash
./gradlew test

# With coverage
./gradlew jacocoTestReport
```

### Run Integration Tests

```bash
./gradlew connectedAndroidTest
```

### Manual Testing Checklist

- [ ] App launches without crash
- [ ] Trending content loads
- [ ] Search returns results
- [ ] Video plays smoothly
- [ ] Subtitles work
- [ ] Quality selection works
- [ ] Resume position restores
- [ ] PiP mode works (Android 8+)
- [ ] Remote keys respond
- [ ] Memory usage is stable

## 🔧 Configuration

### Update Web App URL

Edit `TopsterApplication.kt`:
```kotlin
private const val WEB_APP_URL = "http://192.168.1.100:3000"
```

### Adjust Buffer Settings

Edit `ExoPlayerManager.kt`:
```kotlin
private const val MIN_BUFFER_MS = 15000  // Increase for slower connections
private const val MAX_BUFFER_MS = 50000
```

### Change Cache Sizes

Edit `ImageCache.kt` and `ExoPlayerManager.kt`:
```kotlin
private const val MEMORY_CACHE_SIZE_MB = 10L  // Increase for high-end devices
private const val DISK_CACHE_SIZE_MB = 50L
private const val CACHE_SIZE = 100L * 1024 * 1024  // Media cache
```

## 📊 Performance Monitoring

### Enable Debug Logging

In `TopsterApplication.kt`, debug builds automatically enable:
- Memory logging (every 30s)
- Performance metrics
- Detailed logs

Check logcat:
```bash
adb logcat -s TopsterApplication:D PerformanceMonitor:D ExoPlayerManager:D
```

### Check Memory Usage

```
D/PerformanceMonitor: Memory: 150MB / 512MB (free: 362MB, native: 45MB)
```

### View Performance Metrics

```
D/PerformanceMonitor: load_trending: 342ms (avg: 315ms, min: 280ms, max: 420ms)
```

## 🐛 Troubleshooting

### App Crashes on Startup

1. Check logcat for stack trace
2. Verify all dependencies are installed
3. Clean and rebuild:
   ```bash
   ./gradlew clean
   ./gradlew assembleStableDebug
   ```

### Video Won't Play

1. Check MCP server is running: `topster --mcp`
2. Verify network connectivity
3. Check video URL in logs
4. Try different quality/source

### Device Not Registering

1. Ensure web app is running on network
2. Check WEB_APP_URL in `TopsterApplication.kt`
3. Verify firewall allows port 3000
4. Check logcat for registration errors

### High Memory Usage

1. Clear image cache: Settings → Clear Cache
2. Reduce cache sizes in config
3. Check for memory leaks with profiler
4. Monitor with PerformanceMonitor

### PiP Not Working

1. Verify Android 8.0+
2. Check system supports PiP:
   ```kotlin
   packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
   ```
3. Grant PiP permission in system settings

## 📦 Build Variants

### Debug Build

```bash
./gradlew assembleStableDebug
```
- Debuggable
- Performance monitoring enabled
- No code shrinking
- Larger APK size

### Release Build

```bash
./gradlew assembleStableRelease
```
- Optimized
- Code shrinking enabled
- 25% smaller APK
- Production-ready

### Beta Build

```bash
./gradlew assembleBetaRelease
```
- Separate app ID (`.beta`)
- Test new features
- Can coexist with stable

## 🎬 Demo Flow

### 1. Home Screen
- Shows trending movies/TV shows
- Grid of 6 columns
- Focus on first item
- Smooth D-pad navigation

### 2. Search
- Type "Breaking Bad"
- Results appear instantly (prefetched)
- Select result to view details

### 3. Video Details
- Seasons and episodes listed
- Select episode to watch
- Auto-loads saved position

### 4. Playback
- Video starts within 1 second
- Smooth buffering
- Remote controls work
- Quality/subtitle selection

### 5. Background
- Press Home → Enters PiP
- Audio continues
- Return to exit PiP

## 🔗 Integration with Web App

### 1. Start MCP Server

```bash
cd ../..  # Back to topster root
bun run index.ts --mcp
```

### 2. Start Web App

```bash
cd site
bun run dev
```

### 3. Verify Device Shows Up

Open web app at `http://localhost:3000`

Check for TV device indicator in top-right (🎮 1 TV)

### 4. Cast from Web

1. Browse content on web
2. Click "Cast to TV" button
3. Select your TV device
4. Video plays on TV

## 📱 Emulator Setup

### Create Android TV Emulator

1. Open Android Studio
2. Tools → Device Manager
3. Create Device
4. Select "TV" category
5. Choose "1080p TV" profile
6. System Image: Android 12+ (API 31+)
7. Finish

### Launch Emulator

```bash
~/Android/Sdk/emulator/emulator -avd <emulator-name>
```

### Install App

Same as physical device:
```bash
adb install app/build/outputs/apk/stable/debug/*.apk
```

## 🎯 Next Steps

After successful installation:

1. **Customize UI** - Edit Compose screens in `ui/screens/`
2. **Add Features** - Create new controllers in `player/controllers/`
3. **Improve Performance** - Use PerformanceMonitor to identify bottlenecks
4. **Add Tests** - Write tests in `src/test/`
5. **Deploy** - Build release APK and distribute

## 📝 Useful Commands

```bash
# View logs
adb logcat

# Clear app data
adb shell pm clear com.topster.tv.stable

# Take screenshot
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png

# Record video
adb shell screenrecord /sdcard/demo.mp4
# Ctrl+C to stop
adb pull /sdcard/demo.mp4

# Check app info
adb shell dumpsys package com.topster.tv.stable

# Monitor memory
adb shell dumpsys meminfo com.topster.tv.stable
```

## 🎓 Learning Resources

- **Jetpack Compose for TV**: https://developer.android.com/jetpack/androidx/releases/tv
- **ExoPlayer Guide**: https://developer.android.com/guide/topics/media/exoplayer
- **Android TV Best Practices**: https://developer.android.com/design/ui/tv
- **SmartTube Source**: https://github.com/yuliskov/SmartTube

## 💡 Pro Tips

1. **Use ADB Wirelessly** - No USB cable needed:
   ```bash
   adb tcpip 5555
   adb connect <tv-ip>:5555
   ```

2. **Hot Reload** - Changes apply without full rebuild (Compose)

3. **Logcat Filtering** - Focus on important logs:
   ```bash
   adb logcat -s "Topster*:D" "*:E"
   ```

4. **Profile Memory** - Use Android Studio Profiler for deep analysis

5. **Test on Real Device** - Emulator doesn't show true performance

---

**You're all set! Build something amazing! 🚀**
