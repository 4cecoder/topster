# Troubleshooting Playback Issues

## 403 Forbidden Errors

If you're seeing HTTP 403 errors when trying to play videos, follow these steps:

### Step 1: Use Dry Run Mode

Dry run mode will test the stream URL and show you the exact mpv command being generated:

```bash
topster "Your Movie Name" --dry-run
```

This will:
- ✅ Test the stream URL with curl (to verify headers are working)
- ✅ Show the exact mpv command that will be used
- ✅ Display all referer headers and video source info
- ✅ NOT actually play the video

### Step 2: Enable Debug Logging

If dry run shows the stream is accessible but playback still fails:

```bash
DEBUG=1 topster "Your Movie Name"
```

This will show:
- The exact mpv command being executed
- All headers being sent
- Player exit codes
- Detailed error information

### Step 3: Test the mpv Command Directly

Copy the mpv command from the dry run output and test it manually:

```bash
# Example from dry run output
mpv 'https://example.com/video.m3u8' \
  --user-agent='Mozilla/5.0...' \
  --http-header-fields='Referer: https://example.com,User-Agent: Mozilla/5.0...' \
  --ytdl=no \
  --fullscreen \
  --cache=yes
```

If this works manually but fails through topster, there's a configuration issue.

### Step 4: Check Video Source Quality

Some sources may have multiple quality options. Try different ones:

```bash
topster "Your Movie Name" --dry-run
```

Look at the available video sources and try selecting a different quality.

## Common Issues

### Issue: "ytdl_hook ERROR: HTTP Error 403"

**Solution**: This is fixed by using `--ytdl=no` flag, which is now automatically added for .m3u8, .mp4, and .mkv URLs.

If you're still seeing this, the URL might not be recognized. Check the dry run output.

### Issue: Stream plays but crashes immediately

**Possible causes**:
1. **Expired URL**: Many streaming URLs expire after a short time. Try searching again.
2. **Wrong referer**: The server is checking the referer header strictly.
3. **Rate limiting**: The server detected too many requests.

**Solution**:
```bash
# Test with curl first
curl -I -L -A "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" \
  -H "Referer: [referer-from-dry-run]" \
  "[url-from-dry-run]"
```

### Issue: "VO has bad performance" warning

This is a graphics driver warning and doesn't affect playback. To fix it:

1. Update your graphics drivers
2. Or force a different video output:
   ```bash
   # Add to your mpv config
   echo "vo=gpu" >> ~/.config/mpv/mpv.conf
   ```

## Advanced Debugging

### Test Individual Components

1. **Test curl access**:
   ```bash
   curl -I -L -A "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" "https://stream-url.m3u8"
   ```

2. **Test mpv with minimal flags**:
   ```bash
   mpv --ytdl=no "https://stream-url.m3u8"
   ```

3. **Test with maximum headers**:
   ```bash
   mpv --ytdl=no \
     --user-agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36" \
     --http-header-fields="Referer: https://example.com,Accept: */*" \
     "https://stream-url.m3u8"
   ```

## Getting Help

When reporting issues, always include:

1. Output from `--dry-run`
2. Output from `DEBUG=1` run
3. Your mpv version: `mpv --version`
4. The curl test results
5. Any error messages

## Quick Reference

| Command | Purpose |
|---------|---------|
| `topster "Movie" --dry-run` | Test stream without playing |
| `DEBUG=1 topster "Movie"` | Play with detailed logging |
| `topster --continue` | Resume watching |
| `topster --history` | View watch history |
| `curl -I -L "url"` | Test URL accessibility |
| `mpv --version` | Check mpv version |

## Performance Optimization

If playback is choppy or buffering:

```bash
# Increase cache (add to ~/.config/mpv/mpv.conf)
cache=yes
demuxer-max-bytes=150M
demuxer-max-back-bytes=50M
```

## Notes

- Stream URLs often expire after 1-24 hours
- Some providers block VPNs
- Geographic restrictions may apply
- Quality/availability varies by source
