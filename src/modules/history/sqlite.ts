// SQLite-based history storage with Drizzle ORM for blazing fast performance
import { Database } from 'bun:sqlite';
import { drizzle, BunSQLiteDatabase } from 'drizzle-orm/bun-sqlite';
import { eq, and, desc, gt, sql } from 'drizzle-orm';
import { migrate } from 'drizzle-orm/bun-sqlite/migrator';
import { existsSync, mkdirSync } from 'fs';
import { dirname, join } from 'path';
import type { HistoryEntry, MediaItem, Episode } from '../../core/types';
import { HistoryError } from '../../core/errors';
import { getConfig } from '../../core/config';
import { history, type HistoryRecord } from './schema';

const MAX_HISTORY_ENTRIES = 100;

export class SQLiteHistoryManager {
  private db: BunSQLiteDatabase;
  private sqlite: Database;
  private dbPath: string;

  constructor() {
    const dataDir = getConfig().getDataDir();
    this.dbPath = `${dataDir}/history.db`;

    // Ensure directory exists
    const dir = dirname(this.dbPath);
    if (!existsSync(dir)) {
      mkdirSync(dir, { recursive: true });
    }

    // Open database with WAL mode for better concurrency and performance
    this.sqlite = new Database(this.dbPath);
    this.sqlite.exec('PRAGMA journal_mode = WAL');
    this.sqlite.exec('PRAGMA synchronous = NORMAL');
    this.sqlite.exec('PRAGMA cache_size = -64000'); // 64MB cache
    this.sqlite.exec('PRAGMA temp_store = MEMORY');
    this.sqlite.exec('PRAGMA mmap_size = 268435456'); // 256MB mmap

    this.db = drizzle(this.sqlite);
    this.initDatabase();
  }

  private initDatabase(): void {
    try {
      // Check if this is a legacy database (has history table but no migrations table)
      const tables = this.sqlite
        .query("SELECT name FROM sqlite_master WHERE type='table' AND name='history'")
        .all() as { name: string }[];

      const migrationsTable = this.sqlite
        .query("SELECT name FROM sqlite_master WHERE type='table' AND name='__drizzle_migrations'")
        .all() as { name: string }[];

      const hasLegacySchema = tables.length > 0 && migrationsTable.length === 0;

      if (hasLegacySchema) {
        // Mark the initial migration as applied for existing databases
        this.sqlite.exec(`
          CREATE TABLE IF NOT EXISTS __drizzle_migrations (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            hash TEXT NOT NULL,
            created_at INTEGER
          )
        `);

        // Insert the initial migration record (hash from 0000_bright_blue_marvel.sql)
        this.sqlite.exec(`
          INSERT INTO __drizzle_migrations (hash, created_at)
          VALUES ('0000_bright_blue_marvel', ${Date.now()})
        `);
      }

      // Run Drizzle migrations for type-safe schema management
      // Migrations are stored in the drizzle/ directory at project root
      // Use import.meta.dir to find the source file location, then go to project root
      const currentDir = import.meta.dir; // Points to src/modules/history/
      const migrationsFolder = join(currentDir, '..', '..', '..', 'drizzle');

      if (existsSync(migrationsFolder)) {
        migrate(this.db, { migrationsFolder });
      } else {
        // Fallback: if migrations folder not found, we assume schema is already created
        console.warn('Drizzle migrations folder not found at:', migrationsFolder);
        console.warn('Assuming schema is up to date');
      }

      // Clean up any duplicate entries (keep most recent)
      this.cleanupDuplicates();
    } catch (error) {
      throw new HistoryError(`Failed to run migrations: ${error}`);
    }
  }

  private cleanupDuplicates(): void {
    try {
      // Remove duplicate entries, keeping only the most recent for each (id, episodeId) pair
      // This handles the case where NULL episodeId created duplicates
      this.sqlite.exec(`
        DELETE FROM history
        WHERE rowid NOT IN (
          SELECT MAX(rowid)
          FROM history
          GROUP BY id, episodeId
        )
      `);
    } catch (error) {
      console.error('Failed to cleanup duplicates:', error);
    }
  }

  async add(entry: Omit<HistoryEntry, 'lastWatched'>): Promise<void> {
    if (!getConfig().get('historyEnabled')) return;

    try {
      // Delete existing entry first to handle NULL episodeId properly
      // (onConflictDoUpdate doesn't work with NULL values in composite keys)
      const condition = entry.episodeId
        ? and(eq(history.id, entry.id), eq(history.episodeId, entry.episodeId))
        : and(eq(history.id, entry.id), sql`${history.episodeId} IS NULL`);

      await this.db.delete(history).where(condition);

      // Insert new entry
      await this.db
        .insert(history)
        .values({
          id: entry.id,
          title: entry.title,
          type: entry.type,
          url: entry.url,
          episodeId: entry.episodeId || null,
          episodeTitle: entry.episodeTitle || null,
          seasonNumber: entry.seasonNumber || null,
          episodeNumber: entry.episodeNumber || null,
          position: entry.position,
          duration: entry.duration,
          percentWatched: entry.percentWatched,
          lastWatched: new Date(),
          completed: entry.completed,
        });

      // Clean up old entries if we exceed max
      const count = await this.db
        .select({ count: sql<number>`count(*)` })
        .from(history);

      if (count[0]?.count && count[0].count > MAX_HISTORY_ENTRIES) {
        // Delete oldest entries beyond limit
        const oldestToKeep = await this.db
          .select({ lastWatched: history.lastWatched })
          .from(history)
          .orderBy(desc(history.lastWatched))
          .limit(1)
          .offset(MAX_HISTORY_ENTRIES - 1);

        if (oldestToKeep[0]) {
          await this.db
            .delete(history)
            .where(sql`${history.lastWatched} < ${oldestToKeep[0].lastWatched}`);
        }
      }
    } catch (error) {
      throw new HistoryError(`Failed to add history entry: ${error}`);
    }
  }

  async update(
    id: string,
    episodeId: string | undefined,
    position: string,
    duration: string
  ): Promise<void> {
    try {
      // Calculate percent watched
      const posSeconds = this.parseTime(position);
      const durSeconds = this.parseTime(duration);
      const percentWatched = durSeconds > 0 ? (posSeconds / durSeconds) * 100 : 0;
      const completed = percentWatched >= 90;

      const condition = episodeId
        ? and(eq(history.id, id), eq(history.episodeId, episodeId))
        : and(eq(history.id, id), sql`${history.episodeId} IS NULL`);

      await this.db
        .update(history)
        .set({
          position,
          duration,
          percentWatched,
          completed,
          lastWatched: new Date(),
        })
        .where(condition);
    } catch (error) {
      throw new HistoryError(`Failed to update history entry: ${error}`);
    }
  }

  async get(id: string, episodeId?: string): Promise<HistoryEntry | undefined> {
    try {
      const condition = episodeId
        ? and(eq(history.id, id), eq(history.episodeId, episodeId))
        : and(eq(history.id, id), sql`${history.episodeId} IS NULL`);

      const rows = await this.db.select().from(history).where(condition).limit(1);

      return rows[0] ? this.recordToEntry(rows[0]) : undefined;
    } catch (error) {
      console.error('Failed to get history entry:', error);
      return undefined;
    }
  }

  async getAll(): Promise<HistoryEntry[]> {
    try {
      const rows = await this.db
        .select()
        .from(history)
        .orderBy(desc(history.lastWatched));

      return rows.map(row => this.recordToEntry(row));
    } catch (error) {
      console.error('Failed to get all history:', error);
      return [];
    }
  }

  async getRecent(limit: number = 10): Promise<HistoryEntry[]> {
    try {
      const rows = await this.db
        .select()
        .from(history)
        .orderBy(desc(history.lastWatched))
        .limit(limit);

      return rows.map(row => this.recordToEntry(row));
    } catch (error) {
      console.error('Failed to get recent history:', error);
      return [];
    }
  }

  async getIncomplete(): Promise<HistoryEntry[]> {
    try {
      const rows = await this.db
        .select()
        .from(history)
        .where(eq(history.completed, false))
        .orderBy(desc(history.lastWatched));

      return rows.map(row => this.recordToEntry(row));
    } catch (error) {
      console.error('Failed to get incomplete history:', error);
      return [];
    }
  }

  async getGroupedHistory(limit: number = 50, offset: number = 0): Promise<{
    entries: HistoryEntry[],
    total: number,
    hasMore: boolean
  }> {
    try {
      // Get total count for pagination (only incomplete entries)
      const countResult = await this.db
        .select({ count: sql<number>`count(*)` })
        .from(history)
        .where(eq(history.completed, false));

      const total = countResult[0]?.count ?? 0;

      // Get incomplete entries with grouping logic
      const rows = await this.db
        .select()
        .from(history)
        .where(eq(history.completed, false))
        .orderBy(desc(history.lastWatched))
        .limit(limit + 1) // +1 to check if there are more
        .offset(offset);

      const entries = rows.slice(0, limit).map(row => this.recordToEntry(row));

      // Group entries by unique media (show/movie), keeping only the most recent entry per media item
      const mediaMap = new Map<string, HistoryEntry>();

      for (const entry of entries) {
        const mediaKey = entry.id; // Use media ID as the key

        // Keep the most recent entry for each unique media item
        if (!mediaMap.has(mediaKey) || entry.lastWatched > mediaMap.get(mediaKey)!.lastWatched) {
          mediaMap.set(mediaKey, entry);
        }
      }

      // Convert to array and sort by last watched
      const groupedEntries = Array.from(mediaMap.values()).sort((a, b) =>
        new Date(b.lastWatched).getTime() - new Date(a.lastWatched).getTime()
      );

      return {
        entries: groupedEntries,
        total,
        hasMore: rows.length > limit
      };
    } catch (error) {
      console.error('Failed to get grouped history:', error);
      return { entries: [], total: 0, hasMore: false };
    }
  }

  async remove(id: string, episodeId?: string): Promise<void> {
    try {
      const condition = episodeId
        ? and(eq(history.id, id), eq(history.episodeId, episodeId))
        : and(eq(history.id, id), sql`${history.episodeId} IS NULL`);

      await this.db.delete(history).where(condition);
    } catch (error) {
      throw new HistoryError(`Failed to remove history entry: ${error}`);
    }
  }

  async clear(): Promise<void> {
    try {
      await this.db.delete(history);
    } catch (error) {
      throw new HistoryError(`Failed to clear history: ${error}`);
    }
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

  close(): void {
    this.sqlite.close();
  }

  private recordToEntry(record: HistoryRecord): HistoryEntry {
    return {
      id: record.id,
      title: record.title,
      type: record.type as 'movie' | 'tv',
      url: record.url,
      episodeId: record.episodeId || undefined,
      episodeTitle: record.episodeTitle || undefined,
      seasonNumber: record.seasonNumber || undefined,
      episodeNumber: record.episodeNumber || undefined,
      position: record.position,
      duration: record.duration,
      percentWatched: record.percentWatched,
      lastWatched: new Date(record.lastWatched).toISOString(),
      completed: record.completed,
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
let historyInstance: SQLiteHistoryManager | null = null;

export function getHistory(): SQLiteHistoryManager {
  if (!historyInstance) {
    historyInstance = new SQLiteHistoryManager();
  }
  return historyInstance;
}

export function resetHistoryInstance(): void {
  if (historyInstance) {
    historyInstance.close();
  }
  historyInstance = null;
}
