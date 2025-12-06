// Download module - Video downloading using ffmpeg

import { spawn } from 'child_process';
import { join } from 'path';
import { existsSync } from 'fs';
import type { VideoInfo, MediaItem, Episode } from '../../core/types';
import { DownloadError } from '../../core/errors';
import { getConfig } from '../../core/config';

export interface DownloadOptions {
  outputDir?: string;
  filename?: string;
  quality?: string;
  subtitleUrl?: string;
  onProgress?: (percent: number) => void;
}

export interface DownloadResult {
  success: boolean;
  filePath: string;
  error?: string;
}

export async function checkFfmpeg(): Promise<boolean> {
  return new Promise((resolve) => {
    const proc = spawn('which', ['ffmpeg']);
    proc.on('close', (code) => resolve(code === 0));
  });
}

export function generateFilename(
  media: MediaItem,
  episode?: Episode,
  seasonNumber?: number
): string {
  const sanitize = (str: string) =>
    str.replace(/[<>:"/\\|?*]/g, '').replace(/\s+/g, '_');

  const title = sanitize(media.title);

  if (media.type === 'tv' && episode) {
    const season = seasonNumber?.toString().padStart(2, '0') || '01';
    const ep = episode.number.toString().padStart(2, '0');
    const epTitle = episode.title ? `_${sanitize(episode.title)}` : '';
    return `${title}_S${season}E${ep}${epTitle}.mkv`;
  }

  const year = media.year ? `_(${media.year})` : '';
  return `${title}${year}.mkv`;
}

export async function download(
  video: VideoInfo,
  options: DownloadOptions = {}
): Promise<DownloadResult> {
  const hasFfmpeg = await checkFfmpeg();
  if (!hasFfmpeg) {
    throw new DownloadError('ffmpeg is not installed');
  }

  const outputDir = options.outputDir || getConfig().get('downloadDir');
  const filename = options.filename || `download_${Date.now()}.mkv`;
  const filePath = join(outputDir, filename);

  const args: string[] = [
    '-y', // Overwrite output
    '-i', video.url,
  ];

  // Add referer if present
  if (video.referer) {
    args.unshift('-headers', `Referer: ${video.referer}\r\n`);
  }

  // Add subtitle if available
  if (options.subtitleUrl || video.subtitles.length > 0) {
    const subUrl = options.subtitleUrl || video.subtitles[0]?.url;
    if (subUrl) {
      args.push('-i', subUrl);
      args.push('-c:s', 'srt'); // Convert subtitles to SRT format
    }
  }

  // Output options
  args.push(
    '-c:v', 'copy',    // Copy video codec
    '-c:a', 'copy',    // Copy audio codec
    '-progress', 'pipe:1', // Output progress to stdout
    filePath
  );

  return new Promise((resolve, reject) => {
    const proc = spawn('ffmpeg', args, { stdio: ['pipe', 'pipe', 'pipe'] });

    let duration = 0;
    let lastProgress = 0;

    proc.stderr?.on('data', (data) => {
      const str = data.toString();

      // Parse duration
      const durMatch = str.match(/Duration: (\d{2}):(\d{2}):(\d{2})/);
      if (durMatch) {
        duration = parseInt(durMatch[1]) * 3600 +
                   parseInt(durMatch[2]) * 60 +
                   parseInt(durMatch[3]);
      }
    });

    proc.stdout?.on('data', (data) => {
      const str = data.toString();

      // Parse progress
      const timeMatch = str.match(/out_time_ms=(\d+)/);
      if (timeMatch && duration > 0) {
        const currentMs = parseInt(timeMatch[1]);
        const percent = Math.min(100, (currentMs / (duration * 1000000)) * 100);

        if (percent - lastProgress >= 1) {
          lastProgress = percent;
          options.onProgress?.(Math.round(percent));
        }
      }
    });

    proc.on('error', (error) => {
      reject(new DownloadError(`Download failed: ${error.message}`, filePath));
    });

    proc.on('close', (code) => {
      if (code === 0) {
        resolve({
          success: true,
          filePath,
        });
      } else {
        resolve({
          success: false,
          filePath,
          error: `ffmpeg exited with code ${code}`,
        });
      }
    });
  });
}

export async function downloadMedia(
  video: VideoInfo,
  media: MediaItem,
  episode?: Episode,
  seasonNumber?: number,
  options: Omit<DownloadOptions, 'filename'> = {}
): Promise<DownloadResult> {
  const filename = generateFilename(media, episode, seasonNumber);
  return download(video, { ...options, filename });
}
