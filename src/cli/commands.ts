// CLI command handlers

import type { MediaItem, Season, Episode, VideoSource } from '../core/types';
import { getConfig } from '../core/config';
import { getDefaultProvider } from '../modules/scraper';
import { getHistory } from '../modules/history';
import { play } from '../modules/player';
import { downloadMedia } from '../modules/download';
import { getDiscord } from '../modules/discord';
import * as ui from './ui';

export interface CommandContext {
  debug: boolean;
  download: boolean;
  downloadPath?: string;
  quality?: string;
  subsLanguage?: string;
  noSubs: boolean;
  dryRun?: boolean;
}

const provider = getDefaultProvider();
const history = getHistory();

export async function search(query: string, ctx: CommandContext): Promise<void> {
  ui.printHeader(`Searching: ${query}`);

  try {
    const result = await provider.search(query);
    const selected = await ui.selectMedia(result.items);

    if (selected) {
      await handleMedia(selected, ctx);
    }
  } catch (error) {
    ui.printError(`Search failed: ${error}`);
  }
}

export async function trending(ctx: CommandContext): Promise<void> {
  ui.printHeader('Trending');

  try {
    const items = await provider.getTrending();
    const selected = await ui.selectMedia(items);

    if (selected) {
      await handleMedia(selected, ctx);
    }
  } catch (error) {
    ui.printError(`Failed to get trending: ${error}`);
  }
}

export async function recent(type: 'movie' | 'tv', ctx: CommandContext): Promise<void> {
  ui.printHeader(`Recent ${type === 'movie' ? 'Movies' : 'TV Shows'}`);

  try {
    const items = await provider.getRecent(type);
    const selected = await ui.selectMedia(items);

    if (selected) {
      await handleMedia(selected, ctx);
    }
  } catch (error) {
    ui.printError(`Failed to get recent: ${error}`);
  }
}

export async function continueWatching(ctx: CommandContext): Promise<void> {
  ui.printHeader('Continue Watching');

  const entries = history.getIncomplete();
  ui.printHistoryList(entries);

  if (entries.length === 0) return;

  const selected = await ui.selectHistory(entries);
  if (!selected) return;

  // Reconstruct media item from history
  const media: MediaItem = {
    id: selected.id,
    title: selected.title,
    type: selected.type,
    url: selected.url,
  };

  if (selected.episodeId) {
    const episode: Episode = {
      id: selected.episodeId,
      number: selected.episodeNumber ?? 1,
      title: selected.episodeTitle ?? '',
    };
    await playMedia(media, episode, selected.seasonNumber, ctx);
  } else {
    await playMedia(media, undefined, undefined, ctx);
  }
}

export async function showHistory(): Promise<void> {
  ui.printHeader('Watch History');
  const entries = history.getAll();
  ui.printHistoryList(entries);
}

export async function clearHistory(): Promise<void> {
  const confirmed = await ui.confirm('Clear all watch history?');
  if (confirmed) {
    history.clear();
    ui.printSuccess('History cleared');
  }
}

async function handleMedia(media: MediaItem, ctx: CommandContext): Promise<void> {
  if (media.type === 'tv') {
    await handleTvShow(media, ctx);
  } else {
    await playMedia(media, undefined, undefined, ctx);
  }
}

async function handleTvShow(media: MediaItem, ctx: CommandContext): Promise<void> {
  ui.printHeader(`${media.title} - Select Season`);

  try {
    const seasons = await provider.getSeasons(media.id);

    if (seasons.length === 0) {
      ui.printError('No seasons found');
      return;
    }

    const season = await ui.selectSeason(seasons);
    if (!season) return;

    ui.printHeader(`${media.title} - Season ${season.number}`);

    const episodes = await provider.getEpisodes(season.id);

    if (episodes.length === 0) {
      ui.printError('No episodes found');
      return;
    }

    const episode = await ui.selectEpisode(episodes);
    if (!episode) return;

    await playMedia(media, episode, season.number, ctx);
  } catch (error) {
    ui.printError(`Failed to load TV show: ${error}`);
  }
}

async function playMedia(
  media: MediaItem,
  episode?: Episode,
  seasonNumber?: number,
  ctx?: CommandContext
): Promise<void> {
  const isEpisode = !!episode;

  ui.printInfo('Loading video sources...');

  try {
    const sources = await provider.getVideoSources(
      episode?.id ?? media.id,
      isEpisode
    );

    const firstSource = sources[0];
    const firstVideoInfo = firstSource?.sources[0];

    if (!firstSource || !firstVideoInfo) {
      ui.printError('No video sources found');
      return;
    }

    // Filter subtitles by language if specified
    let subtitles = firstVideoInfo.subtitles;
    if (ctx?.noSubs) {
      subtitles = [];
    } else if (ctx?.subsLanguage) {
      subtitles = subtitles.filter(s =>
        s.lang.toLowerCase().includes(ctx.subsLanguage!.toLowerCase())
      );
    }

    // Get resume position from history
    const historyEntry = history.get(media.id, episode?.id);
    const startTime = historyEntry ? parseTime(historyEntry.position) : 0;

    // Dry run mode - just show details
    if (ctx?.dryRun) {
      const seasonStr = seasonNumber?.toString().padStart(2, '0') ?? '01';
      const title = episode
        ? `${media.title} S${seasonStr}E${episode.number.toString().padStart(2, '0')}`
        : media.title;

      console.log('\n=== DRY RUN MODE ===');
      console.log(`\nTitle: ${title}`);
      console.log(`Media ID: ${media.id}`);
      console.log(`Episode ID: ${episode?.id || 'N/A'}`);
      console.log(`\nVideo Source:`);
      console.log(`  URL: ${firstVideoInfo.url}`);
      console.log(`  Quality: ${firstVideoInfo.quality || 'auto'}`);
      console.log(`  Type: ${isEpisode ? 'Episode' : 'Movie'}`);
      console.log(`\nSubtitles: ${subtitles.length} available`);
      subtitles.slice(0, 5).forEach((sub, i) => {
        console.log(`  [${i + 1}] ${sub.lang} - ${sub.url}`);
      });
      if (subtitles.length > 5) {
        console.log(`  ... and ${subtitles.length - 5} more`);
      }
      console.log(`\nResume Position: ${formatTime(startTime)}`);
      console.log(`History Entry: ${historyEntry ? 'Yes' : 'No'}`);
      console.log('\n=== END DRY RUN ===\n');
      return;
    }

    if (ctx?.download) {
      ui.printInfo('Downloading...');
      const result = await downloadMedia(
        firstVideoInfo,
        media,
        episode,
        seasonNumber,
        {
          outputDir: ctx.downloadPath,
          onProgress: (percent) => ui.printProgress(percent, 'Downloading'),
        }
      );

      if (result.success) {
        ui.printSuccess(`Downloaded to: ${result.filePath}`);
      } else {
        ui.printError(`Download failed: ${result.error}`);
      }
      return;
    }

    // Connect Discord RPC
    const discord = getDiscord();
    await discord.connect();
    await discord.updatePresence(media, episode, Date.now());

    // Create/update history entry
    const entry = history.createEntry(media, episode, seasonNumber);
    history.add(entry);

    // Play video
    const seasonStr = seasonNumber?.toString().padStart(2, '0') ?? '01';
    const title = episode
      ? `${media.title} S${seasonStr}E${episode.number.toString().padStart(2, '0')}`
      : media.title;

    ui.printInfo(`Playing: ${title}`);

    const result = await play(
      firstVideoInfo.url,
      {
        startTime: startTime > 60 ? startTime - 10 : 0,
        fullscreen: true,
      },
      subtitles,
      firstVideoInfo.referer
    );

    // Update history with final position
    if (result.position) {
      history.update(media.id, episode?.id, result.position, '00:00:00');
    }

    // Clear Discord RPC
    await discord.clearPresence();
    discord.disconnect();

    ui.printSuccess('Playback finished');
  } catch (error) {
    ui.printError(`Playback failed: ${error}`);
  }
}

function formatTime(seconds: number): string {
  const h = Math.floor(seconds / 3600);
  const m = Math.floor((seconds % 3600) / 60);
  const s = Math.floor(seconds % 60);
  return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
}

function parseTime(time: string): number {
  const parts = time.split(':').map(Number);
  if (parts.length === 3) {
    return (parts[0] ?? 0) * 3600 + (parts[1] ?? 0) * 60 + (parts[2] ?? 0);
  }
  return 0;
}

export async function showConfig(): Promise<void> {
  const config = getConfig();
  ui.printHeader('Configuration');
  console.log(JSON.stringify(config.getAll(), null, 2));
  console.log(`\nConfig file: ${config.getConfigPath()}`);
}

export async function editConfig(): Promise<void> {
  const config = getConfig();
  const configPath = config.getConfigPath();

  const editor = process.env.EDITOR || 'nano';

  const { spawn } = await import('child_process');
  const proc = spawn(editor, [configPath], { stdio: 'inherit' });

  await new Promise<void>((resolve) => {
    proc.on('close', () => resolve());
  });

  ui.printSuccess('Configuration updated');
}

export async function updateApp(): Promise<void> {
  const { checkForUpdates } = await import('../core/updater');

  ui.printHeader('Checking for updates...');

  try {
    const result = await checkForUpdates(false);

    if (result.updated) {
      ui.printSuccess(`✓ Updated to version ${result.currentVersion}`);
      ui.printInfo('Restart Topster to use the new version');
    } else if (result.message.includes('Already up to date')) {
      ui.printSuccess(`✓ Already on latest version (${result.currentVersion})`);
    } else {
      ui.printError(`Update failed: ${result.message}`);
    }
  } catch (error) {
    ui.printError(`Update error: ${error}`);
  }
}
