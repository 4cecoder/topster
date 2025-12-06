// History module - Watch history tracking

import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'fs';
import { dirname } from 'path';
import type { HistoryEntry, MediaItem, Episode } from '../../core/types';
import { HistoryError } from '../../core/errors';
import { getConfig } from '../../core/config';

const MAX_HISTORY_ENTRIES = 100;

export class HistoryManager {
  private historyPath: string;
  private entries: HistoryEntry[] = [];

  constructor() {
    this.historyPath = getConfig().getHistoryPath();
    this.load();
  }

  private load(): void {
    try {
      if (existsSync(this.historyPath)) {
        const data = readFileSync(this.historyPath, 'utf-8');
        this.entries = JSON.parse(data);
      }
    } catch (error) {
      console.error('Failed to load history:', error);
      this.entries = [];
    }
  }

  private save(): void {
    try {
      const dir = dirname(this.historyPath);
      if (!existsSync(dir)) {
        mkdirSync(dir, { recursive: true });
      }
      writeFileSync(this.historyPath, JSON.stringify(this.entries, null, 2));
    } catch (error) {
      throw new HistoryError(`Failed to save history: ${error}`);
    }
  }

  add(entry: Omit<HistoryEntry, 'lastWatched'>): void {
    if (!getConfig().get('historyEnabled')) return;

    const newEntry: HistoryEntry = {
      ...entry,
      lastWatched: new Date().toISOString(),
    };

    // Remove existing entry for same media
    this.entries = this.entries.filter(e => {
      if (entry.type === 'movie') {
        return e.id !== entry.id;
      }
      // For TV shows, match by episode
      return !(e.id === entry.id && e.episodeId === entry.episodeId);
    });

    // Add new entry at the beginning
    this.entries.unshift(newEntry);

    // Limit history size
    if (this.entries.length > MAX_HISTORY_ENTRIES) {
      this.entries = this.entries.slice(0, MAX_HISTORY_ENTRIES);
    }

    this.save();
  }

  update(id: string, episodeId: string | undefined, position: string, duration: string): void {
    const entry = this.entries.find(e =>
      e.id === id && e.episodeId === episodeId
    );

    if (entry) {
      entry.position = position;
      entry.duration = duration;
      entry.lastWatched = new Date().toISOString();

      // Calculate percent watched
      const posSeconds = this.parseTime(position);
      const durSeconds = this.parseTime(duration);
      entry.percentWatched = durSeconds > 0 ? (posSeconds / durSeconds) * 100 : 0;
      entry.completed = entry.percentWatched >= 90;

      this.save();
    }
  }

  get(id: string, episodeId?: string): HistoryEntry | undefined {
    return this.entries.find(e =>
      e.id === id && e.episodeId === episodeId
    );
  }

  getAll(): HistoryEntry[] {
    return [...this.entries];
  }

  getRecent(limit: number = 10): HistoryEntry[] {
    return this.entries.slice(0, limit);
  }

  getIncomplete(): HistoryEntry[] {
    return this.entries.filter(e => !e.completed && e.percentWatched > 5);
  }

  remove(id: string, episodeId?: string): void {
    this.entries = this.entries.filter(e =>
      !(e.id === id && e.episodeId === episodeId)
    );
    this.save();
  }

  clear(): void {
    this.entries = [];
    this.save();
  }

  createEntry(
    media: MediaItem,
    episode?: Episode,
    seasonNumber?: number
  ): Omit<HistoryEntry, 'lastWatched'> {
    return {
      id: media.id,
      title: media.title,
      type: media.type,
      url: media.url,
      episodeId: episode?.id,
      episodeTitle: episode?.title,
      seasonNumber,
      episodeNumber: episode?.number,
      position: '00:00:00',
      duration: '00:00:00',
      percentWatched: 0,
      completed: false,
    };
  }

  private parseTime(time: string): number {
    const parts = time.split(':').map(Number);
    if (parts.length === 3) {
      return (parts[0] ?? 0) * 3600 + (parts[1] ?? 0) * 60 + (parts[2] ?? 0);
    } else if (parts.length === 2) {
      return (parts[0] ?? 0) * 60 + (parts[1] ?? 0);
    }
    return parts[0] ?? 0;
  }
}

// Singleton instance
let historyInstance: HistoryManager | null = null;

export function getHistory(): HistoryManager {
  if (!historyInstance) {
    historyInstance = new HistoryManager();
  }
  return historyInstance;
}

export function resetHistoryInstance(): void {
  historyInstance = null;
}
