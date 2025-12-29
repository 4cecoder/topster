# Topster TV - Installation Guide

## ⚠️ "App not installed" Error Fix

This error happens when there's a signature mismatch with a previously installed version.

### Solution: Complete Uninstall First

#### Method 1: Uninstall from TV Settings
1. On your TV: **Settings** → **Apps** → **See all apps**
2. Find **Topster**
3. Select **Uninstall**
4. Confirm uninstall
5. **Important:** Also clear app data if given the option

#### Method 2: Using ADB (if TV uninstall fails)
```bash
# Connect to your TV
adb connect <YOUR_TV_IP>:5555

# Uninstall completely
adb uninstall com.topster.tv

# Verify it's gone
adb shell pm list packages | grep topster
# Should return nothing
```

### Then Install Fresh

1. **Plug USB into TV**
2. **Use File Manager** to find `topster-tv.apk`
3. **Install** the APK
4. If asked about "Install unknown apps", allow it
5. **Launch Topster**

---

## 🔧 Alternative: Direct ADB Install

If you have ADB access, this is the cleanest method:

```bash
# 1. Enable ADB on TV
# Settings → System → About → Click "Build" 7 times
# Settings → System → Developer Options → Network debugging ON

# 2. Connect from your computer
adb connect <YOUR_TV_IP>:5555

# 3. Uninstall old version
adb uninstall com.topster.tv

# 4. Install new APK directly
adb install /home/fource/gobster/lobster/topster/tv/android/app/build/outputs/apk/debug/app-debug.apk

# 5. Launch the app
adb shell am start -n com.topster.tv/.MainActivity
```

---

## 📱 What to Expect After Install

1. **First launch**: Loading screen ~10 seconds
2. **Server starts**: Extracts Bun binary (93MB) from native libs
3. **Home screen appears**: Trending movies/TV shows
4. **Navigate**: Use D-pad on your remote

---

## 🐛 Troubleshooting

### Still getting "App not installed"?

Try this on TV:
1. Settings → Storage → Internal storage → Cached data → Clear
2. Restart your TV
3. Try installing again

### Via ADB - Check logs:
```bash
# See what's happening during install
adb logcat -c  # Clear logs
# Then try installing the APK
adb logcat | grep -i "package\|install\|topster"
```

### App crashes on launch?

```bash
# View crash logs
adb logcat | grep -E "BunServerManager|TopsterApplication|AndroidRuntime"
```

### Server won't start?

The app should now work because Bun is packaged as a native library at:
```
/data/app/~~<random>~~/com.topster.tv/lib/arm64-v8a/libbun.so
```

This location is **executable** on all Android versions.

---

## ✅ Success Checklist

- [ ] Old Topster version completely uninstalled
- [ ] TV allows "Install unknown apps" for your file manager
- [ ] APK installs without errors
- [ ] App launches and shows loading screen
- [ ] After ~10 seconds, home screen appears with content

If all checks pass, you're good to go! 🎬
