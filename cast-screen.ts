#!/usr/bin/env bun

/**
 * Screen Casting Utility for Gentoo -> DLNA/Projector
 * Captures screen and streams to DLNA devices using FFmpeg
 */

import { $ } from "bun";
import { parseArgs } from "util";

// Parse command line arguments
const { values, positionals } = parseArgs({
  args: Bun.argv.slice(2),
  options: {
    device: {
      type: "string",
      short: "d",
      default: "",
    },
    resolution: {
      type: "string",
      short: "r",
      default: "1920x1080",
    },
    fps: {
      type: "string",
      short: "f",
      default: "30",
    },
    quality: {
      type: "string",
      short: "q",
      default: "medium",
    },
    audio: {
      type: "boolean",
      short: "a",
      default: true,
    },
    discover: {
      type: "boolean",
      default: false,
    },
    help: {
      type: "boolean",
      short: "h",
      default: false,
    },
  },
  strict: true,
  allowPositionals: true,
});

if (values.help) {
  console.log(`
Screen Casting Utility for Gentoo

Usage: bun cast-screen.ts [options]

Options:
  -d, --device <url>      DLNA device URL or IP (e.g., http://192.168.1.100:8080)
  -r, --resolution <res>  Screen resolution (default: 1920x1080)
  -f, --fps <fps>         Frame rate (default: 30)
  -q, --quality <level>   Quality: low, medium, high (default: medium)
  -a, --audio             Include audio (default: true)
  --discover              Discover DLNA devices on network
  -h, --help              Show this help message

Examples:
  # Discover DLNA devices
  bun cast-screen.ts --discover

  # Cast to specific device
  bun cast-screen.ts -d http://192.168.1.100:8080

  # Cast with custom settings
  bun cast-screen.ts -d http://192.168.1.100:8080 -r 1920x1080 -f 60 -q high

  # Cast without audio
  bun cast-screen.ts -d http://192.168.1.100:8080 --audio false
`);
  process.exit(0);
}

// Check dependencies
async function checkDependencies() {
  const deps = ["ffmpeg", "v4l2loopback-utils"];
  const missing: string[] = [];

  for (const dep of deps) {
    try {
      await $`which ${dep}`.quiet();
    } catch {
      missing.push(dep);
    }
  }

  if (missing.length > 0) {
    console.error(`❌ Missing dependencies: ${missing.join(", ")}`);
    console.error(`   Install with: emerge -av ${missing.join(" ")}`);
    process.exit(1);
  }
}

// Discover DLNA devices using gssdp-discover
async function discoverDevices() {
  console.log("🔍 Discovering DLNA devices on network...\n");

  try {
    // Check if gssdp-discover is available
    await $`which gssdp-discover`.quiet();

    // Discover UPnP/DLNA devices
    const proc = Bun.spawn(
      ["gssdp-discover", "-t", "5", "--target", "upnp:rootdevice"],
      {
        stdout: "pipe",
      }
    );

    const text = await new Response(proc.stdout).text();
    console.log(text);
  } catch {
    console.log("Using fallback discovery method...\n");

    // Fallback: scan common DLNA ports
    const network = "192.168.1"; // Adjust for your network
    const ports = [8080, 8200, 49152, 49153];

    console.log("Scanning network for DLNA devices...");

    for (let i = 1; i <= 254; i++) {
      for (const port of ports) {
        const ip = `${network}.${i}`;
        const url = `http://${ip}:${port}`;

        try {
          const response = await fetch(url, {
            signal: AbortSignal.timeout(100),
          });

          if (response.ok) {
            console.log(`✅ Found device at: ${url}`);
          }
        } catch {
          // Ignore connection errors
        }
      }
    }
  }

  console.log("\nUse -d <device-url> to cast to a specific device");
}

// Start screen casting
async function startCasting(deviceUrl: string) {
  console.log("🚀 Starting screen cast to DLNA device...");
  console.log(`   Device: ${deviceUrl}`);
  console.log(`   Resolution: ${values.resolution}`);
  console.log(`   FPS: ${values.fps}`);
  console.log(`   Quality: ${values.quality}`);
  console.log("");

  // Quality presets
  const qualityPresets = {
    low: { bitrate: "1000k", preset: "ultrafast" },
    medium: { bitrate: "2500k", preset: "fast" },
    high: { bitrate: "5000k", preset: "medium" },
  };

  const preset =
    qualityPresets[values.quality as keyof typeof qualityPresets] ||
    qualityPresets.medium;

  // Get screen capture device (usually :0.0 for X11)
  const display = process.env.DISPLAY || ":0";

  // Build FFmpeg command
  const ffmpegArgs = [
    "-f",
    "x11grab",
    "-r",
    values.fps!,
    "-s",
    values.resolution!,
    "-i",
    display,
  ];

  // Add audio if enabled
  if (values.audio) {
    ffmpegArgs.push("-f", "pulse", "-i", "default");
  }

  // Encoding settings
  ffmpegArgs.push(
    "-c:v",
    "libx264",
    "-preset",
    preset.preset,
    "-b:v",
    preset.bitrate,
    "-maxrate",
    preset.bitrate,
    "-bufsize",
    "2M",
    "-pix_fmt",
    "yuv420p"
  );

  if (values.audio) {
    ffmpegArgs.push("-c:a", "aac", "-b:a", "128k");
  }

  // Output format
  ffmpegArgs.push(
    "-f",
    "mpegts",
    `${deviceUrl}/stream.ts`
  );

  console.log("Starting FFmpeg...");
  console.log("Press Ctrl+C to stop casting\n");

  try {
    // Start FFmpeg
    const proc = Bun.spawn(["ffmpeg", ...ffmpegArgs], {
      stdout: "inherit",
      stderr: "inherit",
    });

    await proc.exited;
  } catch (error) {
    console.error("\n❌ Error during casting:", error);
    process.exit(1);
  }
}

// Alternative: Use VLC for casting
async function castWithVLC(deviceUrl: string) {
  console.log("🚀 Starting screen cast with VLC...");

  const vlcArgs = [
    "screen://",
    `--screen-fps=${values.fps}`,
    `--screen-width=${values.resolution!.split("x")[0]}`,
    `--screen-height=${values.resolution!.split("x")[1]}`,
    `--sout=#transcode{vcodec=h264,vb=2500,acodec=mp3,ab=128}:std{access=http,mux=ts,dst=${deviceUrl}}`,
  ];

  const proc = Bun.spawn(["cvlc", ...vlcArgs], {
    stdout: "inherit",
    stderr: "inherit",
  });

  await proc.exited;
}

// Main
async function main() {
  console.log("📺 Screen Casting Utility for Gentoo\n");

  await checkDependencies();

  if (values.discover) {
    await discoverDevices();
    process.exit(0);
  }

  if (!values.device) {
    console.error("❌ No device specified!");
    console.error("   Use --discover to find devices");
    console.error("   Use -d <device-url> to cast");
    process.exit(1);
  }

  await startCasting(values.device);
}

main().catch(console.error);
