// Player module - External player integration

import { spawn, type ChildProcess } from 'child_process';
import { existsSync } from 'fs';
import type { PlayerType, PlayerOptions, Subtitle } from '../../core/types';
import { PlayerError } from '../../core/errors';
import { getConfig } from '../../core/config';

export interface PlaybackResult {
  exitCode: number | null;
  position?: string;
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
  subtitles?: Subtitle[]
): string[] {
  const args: string[] = [url];

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
  subtitles?: Subtitle[]
): string[] {
  const args: string[] = [];

  // IINA CLI requires --keep-running to wait for playback to finish
  args.push('--keep-running');

  // Add the URL/file
  args.push(url);

  // Use -- to pass raw mpv options
  args.push('--');

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

  return args;
}

export async function play(
  url: string,
  options: PlayerOptions = {},
  subtitles?: Subtitle[]
): Promise<PlaybackResult> {
  const playerType = getConfig().get('player');
  const playerBinary = await findPlayer(playerType);

  if (!playerBinary) {
    throw new PlayerError(`Player not found: ${playerType}`, playerType);
  }

  let args: string[];
  switch (playerType) {
    case 'mpv':
      args = buildMpvArgs(url, options, subtitles);
      break;
    case 'vlc':
      args = buildVlcArgs(url, options, subtitles);
      break;
    case 'iina':
      args = buildIinaArgs(url, options, subtitles);
      break;
    default:
      throw new PlayerError(`Unsupported player: ${playerType}`, playerType);
  }

  return new Promise((resolve, reject) => {
    const proc = spawn(playerBinary, args, {
      stdio: 'inherit',
      detached: false,
    });

    proc.on('error', (error) => {
      reject(new PlayerError(`Failed to start player: ${error.message}`, playerType));
    });

    proc.on('close', (code) => {
      resolve({
        exitCode: code,
        completed: code === 0,
      });
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
