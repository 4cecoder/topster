// Player module - External player integration

import { spawn, type ChildProcess } from 'child_process';
import { existsSync } from 'fs';
import type { PlayerType, PlayerOptions, Subtitle } from '../../core/types';
import { PlayerError } from '../../core/errors';
import { getConfig } from '../../core/config';

export interface PlaybackResult {
  exitCode: number | null;
  position?: string;
  duration?: string;
  completed: boolean;
}

const PLAYER_BINARIES: Record<PlayerType, string[]> = {
  mpv: ['mpv'],
  vlc: ['vlc', 'cvlc'],
  iina: ['/Applications/IINA.app/Contents/MacOS/iina-cli'],
  internal: [],
};

export async function findPlayer(type: PlayerType): Promise<string | null> {
  const binaries = PLAYER_BINARIES[type];

  for (const binary of binaries) {
    try {
      const proc = spawn('which', [binary]);
      const code = await new Promise<number>((resolve) => {
        proc.on('close', resolve);
      });
      if (code === 0) return binary;
    } catch {
      continue;
    }
  }

  return null;
}

export function buildMpvArgs(
  url: string,
  options: PlayerOptions,
  subtitles?: Subtitle[],
  referer?: string
): string[] {
  const args: string[] = [url];

  // Add HTTP headers to bypass 403 errors
  const userAgent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';
  args.push(`--user-agent=${userAgent}`);

  // Build HTTP header fields
  const headers: string[] = [];
  if (referer) {
    headers.push(`Referer: ${referer}`);
  }
  headers.push(`User-Agent: ${userAgent}`);
  headers.push('Accept: */*');
  headers.push('Accept-Language: en-US,en;q=0.9');
  headers.push('Connection: keep-alive');

  if (headers.length > 0) {
    args.push(`--http-header-fields=${headers.join(',')}`);
  }

  // Disable ytdl_hook for direct streaming URLs (m3u8, mp4, etc)
  // This prevents the 403 error from ytdl trying to fetch the URL
  if (url.includes('.m3u8') || url.includes('.mp4') || url.includes('.mkv')) {
    args.push('--ytdl=no');
  }

  if (options.startTime && options.startTime > 0) {
    args.push(`--start=${options.startTime}`);
  }

  if (options.fullscreen) {
    args.push('--fullscreen');
  }

  if (options.subtitleFile) {
    args.push(`--sub-file=${options.subtitleFile}`);
  } else if (subtitles && subtitles.length > 0 && subtitles[0]) {
    // Add first subtitle as default
    args.push(`--sub-file=${subtitles[0].url}`);
  }

  // Additional useful mpv options
  args.push('--force-window=immediate');
  args.push('--keep-open=no');

  // Network options for better streaming
  args.push('--cache=yes');
  args.push('--demuxer-max-bytes=50M');
  args.push('--demuxer-max-back-bytes=20M');

  return args;
}

export function buildVlcArgs(
  url: string,
  options: PlayerOptions,
  subtitles?: Subtitle[]
): string[] {
  const args: string[] = [url];

  if (options.startTime && options.startTime > 0) {
    args.push(`--start-time=${options.startTime}`);
  }

  if (options.fullscreen) {
    args.push('--fullscreen');
  }

  if (options.subtitleFile) {
    args.push(`--sub-file=${options.subtitleFile}`);
  } else if (subtitles && subtitles.length > 0 && subtitles[0]) {
    args.push(`--sub-file=${subtitles[0].url}`);
  }

  args.push('--play-and-exit');

  return args;
}

export function buildIinaArgs(
  url: string,
  options: PlayerOptions,
  subtitles?: Subtitle[],
  referer?: string
): string[] {
  const args: string[] = [];

  // IINA CLI requires --keep-running to wait for playback to finish
  args.push('--keep-running');

  // Add the URL/file
  args.push(url);

  // Use -- to pass raw mpv options
  args.push('--');

  // Add HTTP headers to bypass 403 errors
  const userAgent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36';
  args.push(`--user-agent=${userAgent}`);

  // Build HTTP header fields
  const headers: string[] = [];
  if (referer) {
    headers.push(`Referer: ${referer}`);
  }
  headers.push(`User-Agent: ${userAgent}`);
  headers.push('Accept: */*');
  headers.push('Accept-Language: en-US,en;q=0.9');
  headers.push('Connection: keep-alive');

  if (headers.length > 0) {
    args.push(`--http-header-fields=${headers.join(',')}`);
  }

  // Disable ytdl_hook for direct streaming URLs
  if (url.includes('.m3u8') || url.includes('.mp4') || url.includes('.mkv')) {
    args.push('--ytdl=no');
  }

  if (options.startTime && options.startTime > 0) {
    args.push(`--start=${options.startTime}`);
  }

  if (options.fullscreen) {
    args.push('--fullscreen');
  }

  if (options.subtitleFile) {
    args.push(`--sub-file=${options.subtitleFile}`);
  } else if (subtitles && subtitles.length > 0 && subtitles[0]) {
    args.push(`--sub-file=${subtitles[0].url}`);
  }

  // Additional mpv options for better playback
  args.push('--force-window=immediate');
  args.push('--keep-open=no');

  // Network options for better streaming
  args.push('--cache=yes');
  args.push('--demuxer-max-bytes=50M');
  args.push('--demuxer-max-back-bytes=20M');

  return args;
}

export async function play(
  url: string,
  options: PlayerOptions = {},
  subtitles?: Subtitle[],
  referer?: string
): Promise<PlaybackResult> {
  const playerType = getConfig().get('player');
  const playerBinary = await findPlayer(playerType);

  if (!playerBinary) {
    throw new PlayerError(`Player not found: ${playerType}`, playerType);
  }

  let args: string[];
  switch (playerType) {
    case 'mpv':
      args = buildMpvArgs(url, options, subtitles, referer);
      break;
    case 'vlc':
      args = buildVlcArgs(url, options, subtitles);
      break;
    case 'iina':
      args = buildIinaArgs(url, options, subtitles, referer);
      break;
    default:
      throw new PlayerError(`Unsupported player: ${playerType}`, playerType);
  }

  // Add IPC socket for position tracking (mpv and iina only)
  let ipcPath: string | null = null;
  if (playerType === 'mpv' || playerType === 'iina') {
    ipcPath = `/tmp/topster-mpv-${process.pid}-${Date.now()}.sock`;
    args.push(`--input-ipc-server=${ipcPath}`);
  }

  // Debug logging
  const debugEnabled = process.env.DEBUG === '1' || process.env.TOPSTER_DEBUG === '1';
  if (debugEnabled) {
    console.log('\n=== PLAYER DEBUG ===');
    console.log(`Player: ${playerBinary}`);
    console.log(`Command: ${playerBinary} ${args.join(' ')}`);
    console.log(`URL: ${url}`);
    console.log(`Referer: ${referer || 'none'}`);
    console.log(`Subtitles: ${subtitles?.length || 0}`);
    console.log(`IPC Socket: ${ipcPath || 'none'}`);
    console.log('===================\n');
  }

  return new Promise((resolve, reject) => {
    const proc = spawn(playerBinary, args, {
      stdio: 'inherit',
      detached: false,
    });

    let lastPosition = 0;
    let duration = 0;

    // Helper function to query IPC for position and duration
    const queryIPC = async (): Promise<void> => {
      try {
        const { existsSync } = await import('fs');
        if (!existsSync(ipcPath!)) return;

        const { connect } = await import('net');
        const socket = connect(ipcPath!);

        socket.on('connect', () => {
          // Use request IDs to identify responses
          socket.write(JSON.stringify({ command: ['get_property', 'time-pos'], request_id: 1 }) + '\n');
          socket.write(JSON.stringify({ command: ['get_property', 'duration'], request_id: 2 }) + '\n');
        });

        let buffer = '';
        socket.on('data', (data) => {
          buffer += data.toString();
          const lines = buffer.split('\n');
          buffer = lines.pop() || '';

          for (const line of lines) {
            try {
              const response = JSON.parse(line);
              if (response.data !== undefined && response.data !== null && typeof response.data === 'number') {
                if (response.request_id === 1) {
                  lastPosition = response.data;
                } else if (response.request_id === 2) {
                  duration = response.data;
                }
              }
            } catch {
              // Ignore parse errors
            }
          }
          socket.end();
        });

        socket.on('error', () => {
          socket.destroy();
        });

        // Set timeout to prevent hanging
        setTimeout(() => {
          if (!socket.destroyed) socket.destroy();
        }, 500);
      } catch {
        // Ignore errors
      }
    };

    // Poll for position/duration if IPC is available
    let pollInterval: Timer | null = null;
    if (ipcPath) {
      pollInterval = setInterval(() => queryIPC(), 1000); // Poll every second
    }

    proc.on('error', (error) => {
      if (pollInterval) clearInterval(pollInterval);
      reject(new PlayerError(`Failed to start player: ${error.message}`, playerType));
    });

    proc.on('close', async (code) => {
      if (pollInterval) clearInterval(pollInterval);

      // Do one final position check before cleanup
      if (ipcPath) {
        await queryIPC();
        // Give it a moment to complete
        await new Promise(resolve => setTimeout(resolve, 100));
      }

      // Clean up IPC socket
      if (ipcPath) {
        try {
          const { existsSync, unlinkSync } = await import('fs');
          if (existsSync(ipcPath)) {
            unlinkSync(ipcPath);
          }
        } catch {
          // Ignore cleanup errors
        }
      }

      if (debugEnabled && code !== 0) {
        console.log(`\n=== PLAYER EXIT CODE: ${code} ===\n`);
      }

      const result: PlaybackResult = {
        exitCode: code,
        completed: code === 0,
      };

      // Add position and duration if we got them
      if (lastPosition > 0) {
        result.position = formatTime(lastPosition);
      }
      if (duration > 0) {
        result.duration = formatTime(duration);
      }

      resolve(result);
    });
  });
}

export async function playWithTracking(
  url: string,
  options: PlayerOptions = {},
  subtitles?: Subtitle[],
  onProgress?: (position: number) => void
): Promise<PlaybackResult> {
  const playerType = getConfig().get('player');

  if (playerType !== 'mpv') {
    // For non-mpv players, just use regular play
    return play(url, options, subtitles);
  }

  const playerBinary = await findPlayer('mpv');
  if (!playerBinary) {
    throw new PlayerError('mpv not found', 'mpv');
  }

  const args = buildMpvArgs(url, options, subtitles);

  // Add IPC socket for position tracking
  const ipcPath = `/tmp/topster-mpv-${process.pid}.sock`;
  args.push(`--input-ipc-server=${ipcPath}`);

  return new Promise((resolve, reject) => {
    const proc = spawn(playerBinary, args, {
      stdio: 'inherit',
      detached: false,
    });

    let lastPosition = 0;

    proc.on('error', (error) => {
      reject(new PlayerError(`Failed to start player: ${error.message}`, 'mpv'));
    });

    proc.on('close', (code) => {
      resolve({
        exitCode: code,
        position: formatTime(lastPosition),
        completed: code === 0,
      });
    });
  });
}

function formatTime(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
}

export function parseTime(time: string): number {
  const parts = time.split(':').map(Number);
  if (parts.length === 3) {
    return (parts[0] ?? 0) * 3600 + (parts[1] ?? 0) * 60 + (parts[2] ?? 0);
  } else if (parts.length === 2) {
    return (parts[0] ?? 0) * 60 + (parts[1] ?? 0);
  }
  return parts[0] ?? 0;
}
