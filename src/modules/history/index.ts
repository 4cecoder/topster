// History module - Watch history tracking with SQLite backend
import { existsSync, readFileSync, unlinkSync, renameSync } from 'fs';
import type { HistoryEntry } from '../../core/types';
import { getConfig } from '../../core/config';
import { getHistory as getSQLiteHistory, resetHistoryInstance } from './sqlite';

// Export the SQLite manager
export { SQLiteHistoryManager } from './sqlite';
export { getHistory, resetHistoryInstance } from './sqlite';

// Migration utility: converts old JSON history to SQLite
export async function migrateFromJSON(): Promise<void> {
  const config = getConfig();
  const jsonPath = config.getHistoryPath();
  const sqlitePath = `${config.getDataDir()}/history.db`;

  // Check if JSON file exists and SQLite doesn't have data yet
  if (!existsSync(jsonPath)) {
    return; // Nothing to migrate
  }

  try {
    // Read old JSON data
    const jsonData = readFileSync(jsonPath, 'utf-8');
    const entries: HistoryEntry[] = JSON.parse(jsonData);

    if (entries.length === 0) {
      console.log('No history entries to migrate');
      return;
    }

    console.log(`Migrating ${entries.length} history entries from JSON to SQLite...`);

    // Get SQLite manager
    const history = getSQLiteHistory();

    // Import each entry
    for (const entry of entries) {
      await history.add({
        id: entry.id,
        title: entry.title,
        type: entry.type,
        url: entry.url,
        episodeId: entry.episodeId,
        episodeTitle: entry.episodeTitle,
        seasonNumber: entry.seasonNumber,
        episodeNumber: entry.episodeNumber,
        position: entry.position,
        duration: entry.duration,
        percentWatched: entry.percentWatched,
        completed: entry.completed,
      });
    }

    console.log('Migration completed successfully!');

    // Backup old JSON file
    const backupPath = `${jsonPath}.backup`;
    renameSync(jsonPath, backupPath);
    console.log(`Old history backed up to: ${backupPath}`);
  } catch (error) {
    console.error('Failed to migrate history:', error);
    throw error;
  }
}

// Auto-migrate on first import if needed
if (existsSync(getConfig().getHistoryPath())) {
  const sqlitePath = `${getConfig().getDataDir()}/history.db`;
  if (!existsSync(sqlitePath)) {
    console.log('Detected old JSON history, migrating to SQLite...');
    migrateFromJSON().catch(err => {
      console.error('Auto-migration failed:', err);
    });
  }
}
