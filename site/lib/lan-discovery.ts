/**
 * LAN device discovery for TV app detection
 * Uses a combination of HTTP beacon and network scanning
 */

import type { TVDevice } from './types';

const TV_DISCOVERY_PORT = 8765;
const DISCOVERY_TIMEOUT = 5000;

interface DiscoveryMessage {
  type: 'discover' | 'announce';
  deviceId: string;
  deviceName: string;
  port: number;
  timestamp: number;
}

/**
 * Discovers TV devices on the local network
 * This uses a simple HTTP-based discovery protocol
 */
export async function discoverTVDevices(): Promise<TVDevice[]> {
  const devices: TVDevice[] = [];

  try {
    // Try to fetch from known devices endpoint
    const response = await fetch('/api/tv/devices', {
      method: 'GET',
      signal: AbortSignal.timeout(DISCOVERY_TIMEOUT),
    });

    if (response.ok) {
      const data = await response.json();
      return data.devices || [];
    }
  } catch (error) {
    console.error('Discovery error:', error);
  }

  return devices;
}

/**
 * Checks if a specific TV device is online
 */
export async function checkTVDeviceStatus(ip: string, port: number): Promise<boolean> {
  try {
    const response = await fetch(`http://${ip}:${port}/ping`, {
      method: 'GET',
      signal: AbortSignal.timeout(2000),
    });
    return response.ok;
  } catch {
    return false;
  }
}

/**
 * Sends a command to a TV device
 */
export async function sendTVCommand(
  device: TVDevice,
  command: string,
  data?: any
): Promise<any> {
  try {
    const response = await fetch(`http://${device.ip}:${device.port}/command`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ command, data }),
      signal: AbortSignal.timeout(5000),
    });

    if (!response.ok) {
      throw new Error(`Command failed: ${response.statusText}`);
    }

    return await response.json();
  } catch (error) {
    console.error('Send command error:', error);
    throw error;
  }
}

/**
 * Streams media to a TV device
 */
export async function streamToTV(
  device: TVDevice,
  mediaId: string,
  title: string,
  videoUrl: string,
  subtitles?: Array<{ url: string; lang: string }>
): Promise<void> {
  await sendTVCommand(device, 'play', {
    mediaId,
    title,
    videoUrl,
    subtitles,
  });
}
