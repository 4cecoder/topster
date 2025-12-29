# Screen Casting Guide for Gentoo

Cast your Gentoo desktop to DLNA devices, projectors, or TVs.

## Quick Start

```bash
# Discover DLNA devices on your network
bun cast-screen.ts --discover

# Cast to a specific device
bun cast-screen.ts -d http://192.168.1.100:8080

# Cast with high quality
bun cast-screen.ts -d http://192.168.1.100:8080 -q high -f 60
```

## Installation

### 1. Install Dependencies

```bash
# On Gentoo
sudo emerge -av ffmpeg v4l2loopback gstreamer gst-plugins-good gst-plugins-bad
```

### 2. Optional: Install DLNA/UPnP Discovery Tools

```bash
sudo emerge -av gssdp
```

## Methods

### Method 1: FFmpeg (Recommended)

Best for low latency and direct streaming to DLNA devices.

```bash
bun cast-screen.ts -d http://192.168.1.100:8080
```

**Features:**
- Low latency
- Customizable quality
- Audio support
- Works with most DLNA receivers

### Method 2: Simple HTTP Stream

Create an HTTP stream that can be accessed from any browser or media player:

```bash
# Start streaming server on port 8080
ffmpeg -f x11grab -r 30 -s 1920x1080 -i :0 \
  -c:v libx264 -preset ultrafast -tune zerolatency \
  -f mpegts http://0.0.0.0:8080/stream.ts

# Access from any device at:
# http://your-gentoo-ip:8080/stream.ts
```

### Method 3: GStreamer Pipeline

For advanced users who need more control:

```bash
gst-launch-1.0 ximagesrc use-damage=0 ! \
  videoconvert ! \
  x264enc tune=zerolatency bitrate=2000 speed-preset=superfast ! \
  mpegtsmux ! \
  tcpserversink host=0.0.0.0 port=8080
```

### Method 4: VNC-Based Screen Sharing

For screen sharing to any VNC client:

```bash
# Install x11vnc
sudo emerge -av x11vnc

# Start VNC server
x11vnc -display :0 -rfbport 5900

# Connect from any device with a VNC client to:
# vnc://your-gentoo-ip:5900
```

### Method 5: Miracast (WiFi Display)

For casting to Miracast-enabled displays:

```bash
# Install gnome-network-displays (supports Miracast)
sudo emerge -av gnome-network-displays

# Or use command-line tool
sudo emerge -av miracle-wifictl

# Start Miracast
miracle-wifictl
```

## Using with Topster Web App

You can also cast Topster content to DLNA devices:

```bash
# 1. Start the web app
./start-web.sh

# 2. Open in browser and find content

# 3. Get the video URL from the player

# 4. Cast it to DLNA device
ffmpeg -i "VIDEO_URL" -c copy -f mpegts http://192.168.1.100:8080/
```

## DLNA Server Setup

To make your Gentoo machine a DLNA server (so other devices can pull content):

```bash
# Install MiniDLNA
sudo emerge -av minidlna

# Configure /etc/minidlna.conf
sudo nano /etc/minidlna.conf
```

Add:
```ini
media_dir=V,/home/your-user/Videos
friendly_name=Gentoo DLNA Server
network_interface=eth0
```

Start service:
```bash
sudo rc-service minidlna start
sudo rc-update add minidlna default
```

## Projector Connection

### Wired (HDMI/DisplayPort)

```bash
# Detect displays
xrandr

# Extend to projector
xrandr --output HDMI-1 --auto --right-of eDP-1

# Mirror to projector
xrandr --output HDMI-1 --same-as eDP-1

# Projector only
xrandr --output eDP-1 --off --output HDMI-1 --auto
```

### Wireless Projectors

Many wireless projectors support:

1. **Miracast** - Use `gnome-network-displays`
2. **AirPlay** - Use `uxplay` (for Apple devices)
3. **Chromecast** - Use `catt` or Chrome browser
4. **DLNA** - Use the casting script above

## Performance Tuning

### For Low Latency

```bash
bun cast-screen.ts -d http://device:8080 \
  -q low \
  -f 30 \
  -r 1280x720
```

### For High Quality

```bash
bun cast-screen.ts -d http://device:8080 \
  -q high \
  -f 60 \
  -r 1920x1080
```

### Custom FFmpeg

For full control:

```bash
ffmpeg -f x11grab -r 60 -s 1920x1080 -i :0 \
  -f pulse -i default \
  -c:v libx264 \
  -preset ultrafast \
  -tune zerolatency \
  -b:v 5000k \
  -maxrate 5000k \
  -bufsize 2M \
  -c:a aac \
  -b:a 128k \
  -f mpegts \
  http://192.168.1.100:8080/stream
```

## Troubleshooting

### No Audio

```bash
# List audio devices
pactl list sources short

# Use specific audio source
ffmpeg -f x11grab -i :0 \
  -f pulse -i alsa_output.pci-0000_00_1f.3.analog-stereo.monitor \
  ...
```

### Screen Capture Not Working

```bash
# Check X11 permissions
xhost +local:

# Or use specific display
export DISPLAY=:0
```

### High CPU Usage

- Lower FPS: `-f 24`
- Lower resolution: `-r 1280x720`
- Use faster preset: `-preset ultrafast`

### DLNA Device Not Found

```bash
# Check network connectivity
ping 192.168.1.100

# Scan for UPnP devices
gssdp-discover --target upnp:rootdevice

# Check firewall
sudo iptables -L
```

## Systemd Service

Create `/etc/systemd/system/screen-cast.service`:

```ini
[Unit]
Description=Screen Casting Service
After=network.target

[Service]
Type=simple
User=your-user
Environment="DISPLAY=:0"
ExecStart=/usr/bin/bun /path/to/cast-screen.ts -d http://192.168.1.100:8080
Restart=always

[Install]
WantedBy=multi-user.target
```

Enable:
```bash
sudo systemctl enable screen-cast
sudo systemctl start screen-cast
```

## Integration with Topster

Add casting directly from the web app by updating the TV device integration:

1. Register your DLNA device with the web app
2. Use the "Cast to TV" button
3. Topster sends the video URL to the device
4. Device plays the content

## Additional Tools

### Chromecast

```bash
# Install catt
emerge -av catt

# Cast to Chromecast
catt cast http://your-gentoo-ip:8080/stream.ts
```

### AirPlay

```bash
# Install uxplay for AirPlay server
emerge -av uxplay

# Start AirPlay server
uxplay
```

### Screen Recording

Record your screen while casting:

```bash
ffmpeg -f x11grab -i :0 \
  -c:v libx264 -preset fast -crf 22 \
  output.mp4
```

## License

Same as parent Topster project.
