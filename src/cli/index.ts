// CLI entry point

import { Command } from 'commander';
import { getConfig } from '../core/config';
import * as commands from './commands';

export function createCli(): Command {
  const program = new Command();

  program
    .name('topster')
    .description('Stream movies and TV shows from the command line')
    .version('1.0.0');

  program
    .argument('[query...]', 'Search query')
    .option('-t, --trending', 'Show trending content')
    .option('-r, --recent <type>', 'Show recent content (movie|tv)')
    .option('-c, --continue', 'Continue watching from history')
    .option('-H, --history', 'Show watch history')
    .option('--clear-history', 'Clear watch history')
    .option('-d, --download [path]', 'Download instead of streaming')
    .option('-q, --quality <quality>', 'Video quality (480|720|1080|auto)')
    .option('-l, --language <lang>', 'Subtitle language')
    .option('-n, --no-subs', 'Disable subtitles')
    .option('--discord', 'Enable Discord Rich Presence')
    .option('--config', 'Show configuration')
    .option('-e, --edit', 'Edit configuration file')
    .option('-x, --debug', 'Enable debug output')
    .option('--dry-run', 'Show details without playing video')
    .option('--mcp', 'Start MCP server mode')
    .option('--mcp-port <port>', 'MCP server port', '3847')
    .action(async (query: string[], options) => {
      const ctx: commands.CommandContext = {
        debug: options.debug || false,
        download: !!options.download,
        downloadPath: typeof options.download === 'string' ? options.download : undefined,
        quality: options.quality,
        subsLanguage: options.language,
        noSubs: !options.subs,
        dryRun: options.dryRun || false,
      };

      // Update config with CLI options
      const config = getConfig();
      if (options.discord) {
        config.set('discordPresence', true);
      }
      if (options.quality) {
        config.set('quality', options.quality);
      }
      if (options.language) {
        config.set('subsLanguage', options.language);
      }
      if (options.debug) {
        config.set('debug', true);
      }

      // Handle MCP mode
      if (options.mcp) {
        const { startMcpServer } = await import('../mcp');
        const port = parseInt(options.mcpPort) || config.get('mcpPort');
        await startMcpServer(port);
        return;
      }

      // Handle other commands
      if (options.config) {
        await commands.showConfig();
        return;
      }

      if (options.edit) {
        await commands.editConfig();
        return;
      }

      if (options.clearHistory) {
        await commands.clearHistory();
        return;
      }

      if (options.history) {
        await commands.showHistory();
        return;
      }

      if (options.continue) {
        await commands.continueWatching(ctx);
        return;
      }

      if (options.trending) {
        await commands.trending(ctx);
        return;
      }

      if (options.recent) {
        const type = options.recent === 'tv' ? 'tv' : 'movie';
        await commands.recent(type, ctx);
        return;
      }

      if (query.length > 0) {
        await commands.search(query.join(' '), ctx);
        return;
      }

      // Default: launch interactive TUI
      const { runInkApp } = await import('./ink-app.js');
      await runInkApp(ctx);
    });

  return program;
}

export async function runCli(): Promise<void> {
  const program = createCli();
  await program.parseAsync(process.argv);
}
