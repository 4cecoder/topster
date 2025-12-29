import { defineConfig } from 'drizzle-kit';

export default defineConfig({
  schema: './src/modules/history/schema.ts',
  out: './drizzle',
  dialect: 'sqlite',
  dbCredentials: {
    url: 'file:local',
  },
});
