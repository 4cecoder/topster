#!/usr/bin/env bun
/**
 * Topster TV - OTA Deployment Tool
 *
 * Discover and deploy APK updates to Topster TV over LAN
 */

import { existsSync } from 'fs';
import { resolve } from 'path';

const DEFAULT_APK_PATH = './tv/android/app/build/outputs/apk/debug/app-debug.apk';
const OTA_PORT = 8888;

console.log('╔══════════════════════════════════════════════════════════════╗');
console.log('║         Topster TV - OTA Deployment Tool                     ║');
console.log('╚══════════════════════════════════════════════════════════════╝');
console.log();

/**
 * Discover Topster TV devices on the network using mDNS
 */
async function discoverDevices(): Promise<string[]> {
  console.log('🔍 Discovering Topster TV devices on your network...');
  console.log('   (Looking for _topster._tcp services)');
  console.log();

  // Try to use mdns-discovery if available
  try {
    const mdns = await import('multicast-dns');
    const dns = mdns.default();
    const devices: string[] = [];

    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        dns.destroy();
        if (devices.length === 0) {
          console.log('❌ No devices found via mDNS');
          console.log('   Falling back to network scan...');
          resolve([]);
        } else {
          resolve(devices);
        }
      }, 5000);

      dns.on('response', (response: any) => {
        const answers = response.answers || [];
        for (const answer of answers) {
          if (answer.name && answer.name.includes('topster')) {
            const ip = answer.data;
            if (ip && !devices.includes(ip)) {
              devices.push(ip);
              console.log(`✅ Found device: ${ip}:${OTA_PORT}`);
            }
          }
        }
      });

      dns.query({
        questions: [{ name: '_topster._tcp.local', type: 'PTR' }]
      });
    });
  } catch (e) {
    console.log('⚠️  mDNS discovery not available, using manual IP entry');
    return [];
  }
}

/**
 * Get all local network subnets
 */
function getLocalSubnets(): string[] {
  const { networkInterfaces } = require('os');
  const nets = networkInterfaces();
  const subnets: string[] = [];

  for (const name of Object.keys(nets)) {
    for (const net of nets[name]) {
      // Skip internal and IPv6
      if (net.family === 'IPv4' && !net.internal) {
        const parts = net.address.split('.');
        // Add first 3 octets for /24 scan
        const subnet = `${parts[0]}.${parts[1]}.${parts[2]}`;
        if (!subnets.includes(subnet)) {
          subnets.push(subnet);
        }
      }
    }
  }

  return subnets.length > 0 ? subnets : ['192.168.1']; // Fallback
}

/**
 * Scan local network for Topster TV devices
 */
async function scanNetwork(): Promise<string[]> {
  const subnets = getLocalSubnets();
  console.log(`🔍 Scanning local networks: ${subnets.map(s => s + '.0/24').join(', ')}...`);

  const devices: string[] = [];
  const promises: Promise<void>[] = [];

  for (const subnet of subnets) {
    for (let i = 1; i < 255; i++) {
      const ip = `${subnet}.${i}`;
      promises.push(
        (async () => {
          try {
            const controller = new AbortController();
            const timeout = setTimeout(() => controller.abort(), 500);

            const res = await fetch(`http://${ip}:${OTA_PORT}/status`, {
              signal: controller.signal,
            });

            clearTimeout(timeout);

            if (res.ok) {
              const data = await res.json();
              if (data.status === 'ok') {
                devices.push(ip);
                console.log(`✅ Found device: ${ip}:${OTA_PORT} (${data.device})`);
              }
            }
          } catch (e) {
            // Ignore - device not found
          }
        })()
      );
    }
  }

  await Promise.all(promises);
  return devices;
}

/**
 * Get device info
 */
async function getDeviceInfo(ip: string): Promise<any> {
  try {
    const res = await fetch(`http://${ip}:${OTA_PORT}/info`);
    return await res.json();
  } catch (e) {
    return null;
  }
}

/**
 * Upload APK to device
 */
async function uploadApk(ip: string, apkPath: string): Promise<boolean> {
  try {
    console.log();
    console.log(`📤 Uploading APK to ${ip}:${OTA_PORT}...`);
    console.log(`   File: ${apkPath}`);

    const file = Bun.file(apkPath);
    const size = file.size;

    console.log(`   Size: ${(size / 1024 / 1024).toFixed(2)} MB`);

    const formData = new FormData();
    formData.append('apk', file);

    const res = await fetch(`http://${ip}:${OTA_PORT}/update/apk`, {
      method: 'POST',
      body: formData,
    });

    const data = await res.json();

    if (data.success) {
      console.log('✅ Upload successful!');
      console.log('   Installation dialog should appear on TV');
      return true;
    } else {
      console.log('❌ Upload failed:', data.error || 'Unknown error');
      return false;
    }
  } catch (e) {
    console.log('❌ Upload failed:', (e as Error).message);
    return false;
  }
}

/**
 * Main function
 */
async function main() {
  // Get APK path from args or use default
  const apkPath = process.argv[2] || DEFAULT_APK_PATH;
  const resolvedPath = resolve(apkPath);

  if (!existsSync(resolvedPath)) {
    console.log('❌ APK file not found:', resolvedPath);
    console.log();
    console.log('Usage: bun deploy-to-tv.ts [path-to-apk]');
    console.log(`Default: ${DEFAULT_APK_PATH}`);
    process.exit(1);
  }

  // Discover devices
  let devices = await discoverDevices();

  // If mDNS didn't find anything, scan network
  if (devices.length === 0) {
    devices = await scanNetwork();
  }

  // If still no devices, ask for manual IP
  if (devices.length === 0) {
    console.log();
    console.log('❌ No devices found automatically');
    console.log();
    console.log('Please enter the IP address of your Google TV:');
    console.log('(You can find it in: Settings → Network & Internet → [Your Network])');
    console.log();

    process.stdout.write('IP Address: ');
    const line = await new Promise<string>((resolve) => {
      process.stdin.once('data', (data) => {
        resolve(data.toString().trim());
      });
    });

    if (line) {
      devices.push(line);
    } else {
      console.log('❌ No IP address provided');
      process.exit(1);
    }
  }

  console.log();
  console.log('━'.repeat(64));
  console.log();

  // Show device info
  for (const ip of devices) {
    const info = await getDeviceInfo(ip);
    if (info) {
      console.log(`📺 Device: ${info.device.manufacturer} ${info.device.model}`);
      console.log(`   IP: ${ip}:${OTA_PORT}`);
      console.log(`   App Version: ${info.app.version}`);
      console.log(`   Android: ${info.device.android} (SDK ${info.device.sdk})`);
      console.log();
    }
  }

  // Upload to first device (or all if multiple)
  if (devices.length === 1) {
    await uploadApk(devices[0], resolvedPath);
  } else {
    console.log(`Found ${devices.length} devices. Uploading to all...`);
    for (const ip of devices) {
      await uploadApk(ip, resolvedPath);
    }
  }

  console.log();
  console.log('━'.repeat(64));
  console.log();
  console.log('✅ Deployment complete!');
  console.log();
  console.log('Next steps:');
  console.log('1. Check your TV for the installation prompt');
  console.log('2. Click "Install" to update the app');
  console.log('3. Open the updated app');
  console.log();

  process.exit(0);
}

main();
