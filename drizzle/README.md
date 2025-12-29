# Database Migrations

This directory contains Drizzle ORM migrations for the Topster application.

## Overview

Topster uses Drizzle ORM for type-safe database operations with SQLite. All database schema changes are managed through migrations stored in this directory.

## Schema Location

The database schema is defined in:
- `src/modules/history/schema.ts` - Drizzle schema definition

## Running Migrations

Migrations run automatically when the application starts. The `SQLiteHistoryManager` class handles migration execution.

## Creating New Migrations

When you modify the schema in `src/modules/history/schema.ts`:

1. Generate a new migration:
   ```bash
   bun run db:generate
   ```

2. The new migration will be created in this directory with a timestamp and description.

3. Migrations are applied automatically on next app start.

## Migration Files

- `0000_bright_blue_marvel.sql` - Initial schema (history table with indexes)
- `meta/_journal.json` - Migration history tracker

## Database Location

The SQLite database is stored at:
- `~/.local/share/topster/history.db`

## Type Safety

All database operations use Drizzle's type-safe query builder:
- `insert()` - Type-checked inserts
- `select()` - Type-checked queries
- `update()` - Type-checked updates
- `delete()` - Type-checked deletes

The schema exports TypeScript types:
- `HistoryRecord` - Full database record type
- `NewHistoryRecord` - Insert type (without auto-generated fields)
