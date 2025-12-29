// Drizzle schema for watch history
import { sqliteTable, text, integer, real, primaryKey, index } from 'drizzle-orm/sqlite-core';

export const history = sqliteTable(
  'history',
  {
    id: text('id').notNull(),
    title: text('title').notNull(),
    type: text('type', { enum: ['movie', 'tv'] }).notNull(),
    url: text('url').notNull(),
    episodeId: text('episodeId'),
    episodeTitle: text('episodeTitle'),
    seasonNumber: integer('seasonNumber'),
    episodeNumber: integer('episodeNumber'),
    position: text('position').notNull().default('00:00:00'),
    duration: text('duration').notNull().default('00:00:00'),
    percentWatched: real('percentWatched').notNull().default(0),
    lastWatched: integer('lastWatched', { mode: 'timestamp_ms' }).notNull(),
    completed: integer('completed', { mode: 'boolean' }).notNull().default(false),
  },
  (table) => ({
    pk: primaryKey({ columns: [table.id, table.episodeId] }),
    lastWatchedIdx: index('idx_lastWatched').on(table.lastWatched),
    incompleteIdx: index('idx_incomplete').on(table.completed, table.percentWatched),
    typeIdx: index('idx_type').on(table.type),
  })
);

export type HistoryRecord = typeof history.$inferSelect;
export type NewHistoryRecord = typeof history.$inferInsert;
