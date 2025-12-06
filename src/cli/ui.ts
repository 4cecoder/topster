// CLI UI utilities

import * as readline from 'readline';
import type { MediaItem, Season, Episode, HistoryEntry } from '../core/types';

export function createReadline(): readline.Interface {
  return readline.createInterface({
    input: process.stdin,
    output: process.stdout,
  });
}

export async function prompt(question: string): Promise<string> {
  const rl = createReadline();
  return new Promise((resolve) => {
    rl.question(question, (answer) => {
      rl.close();
      resolve(answer.trim());
    });
  });
}

export async function confirm(question: string): Promise<boolean> {
  const answer = await prompt(`${question} (y/n): `);
  return answer.toLowerCase() === 'y' || answer.toLowerCase() === 'yes';
}

export function printMediaItem(item: MediaItem, index?: number): void {
  const prefix = index !== undefined ? `[${index + 1}] ` : '';
  const type = item.type === 'movie' ? '🎬' : '📺';
  const year = item.year ? ` (${item.year})` : '';
  const quality = item.quality ? ` [${item.quality}]` : '';

  console.log(`${prefix}${type} ${item.title}${year}${quality}`);
}

export function printMediaList(items: MediaItem[]): void {
  items.forEach((item, i) => printMediaItem(item, i));
}

export function printSeason(season: Season, index?: number): void {
  const prefix = index !== undefined ? `[${index + 1}] ` : '';
  console.log(`${prefix}${season.title}`);
}

export function printSeasonList(seasons: Season[]): void {
  seasons.forEach((season, i) => printSeason(season, i));
}

export function printEpisode(episode: Episode, index?: number): void {
  const prefix = index !== undefined ? `[${index + 1}] ` : '';
  const title = episode.title || `Episode ${episode.number}`;
  console.log(`${prefix}E${episode.number.toString().padStart(2, '0')}: ${title}`);
}

export function printEpisodeList(episodes: Episode[]): void {
  episodes.forEach((episode, i) => printEpisode(episode, i));
}

export function printHistoryEntry(entry: HistoryEntry, index?: number): void {
  const prefix = index !== undefined ? `[${index + 1}] ` : '';
  const type = entry.type === 'movie' ? '🎬' : '📺';
  const progress = `${Math.round(entry.percentWatched)}%`;
  const status = entry.completed ? '✓' : `⏸ ${entry.position}`;

  let title = entry.title;
  if (entry.episodeId) {
    const ep = entry.episodeNumber?.toString().padStart(2, '0');
    const season = entry.seasonNumber?.toString().padStart(2, '0') || '01';
    title += ` S${season}E${ep}`;
  }

  console.log(`${prefix}${type} ${title} [${progress}] ${status}`);
}

export function printHistoryList(entries: HistoryEntry[]): void {
  if (entries.length === 0) {
    console.log('No watch history found.');
    return;
  }
  entries.forEach((entry, i) => printHistoryEntry(entry, i));
}

export async function selectFromList<T>(
  items: T[],
  printer: (item: T, index: number) => void,
  question: string = 'Select an option: '
): Promise<T | null> {
  if (items.length === 0) return null;

  items.forEach((item, i) => printer(item, i));
  console.log('[0] Cancel');

  const answer = await prompt(question);
  const index = parseInt(answer) - 1;

  if (isNaN(index) || index < 0 || index >= items.length) {
    return null;
  }

  return items[index] ?? null;
}

export async function selectMedia(items: MediaItem[]): Promise<MediaItem | null> {
  return selectFromList(items, printMediaItem, 'Select media: ');
}

export async function selectSeason(seasons: Season[]): Promise<Season | null> {
  return selectFromList(seasons, printSeason, 'Select season: ');
}

export async function selectEpisode(episodes: Episode[]): Promise<Episode | null> {
  return selectFromList(episodes, printEpisode, 'Select episode: ');
}

export async function selectHistory(entries: HistoryEntry[]): Promise<HistoryEntry | null> {
  return selectFromList(entries, printHistoryEntry, 'Select to continue: ');
}

export function printHeader(text: string): void {
  console.log(`\n=== ${text} ===\n`);
}

export function printError(message: string): void {
  console.error(`❌ ${message}`);
}

export function printSuccess(message: string): void {
  console.log(`✅ ${message}`);
}

export function printInfo(message: string): void {
  console.log(`ℹ️  ${message}`);
}

export function printProgress(percent: number, label?: string): void {
  const width = 30;
  const filled = Math.round((percent / 100) * width);
  const empty = width - filled;
  const bar = '█'.repeat(filled) + '░'.repeat(empty);
  const text = label ? `${label}: ` : '';
  process.stdout.write(`\r${text}[${bar}] ${percent}%`);
  if (percent >= 100) console.log();
}

export function clearLine(): void {
  process.stdout.write('\r' + ' '.repeat(80) + '\r');
}
