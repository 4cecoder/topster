import React, { useState, useEffect } from 'react';
import { render } from 'ink';
import { Box, Text, useApp } from 'ink';
import { Header } from './components/Header.js';
import { Menu } from './components/Menu.js';
import { SearchInput } from './components/SearchInput.js';
import { MediaList } from './components/MediaList.js';
import { SeasonList, type Season } from './components/SeasonList.js';
import { EpisodeList, type Episode } from './components/EpisodeList.js';
import { HistoryList } from './components/HistoryList.js';
import { LoadingSpinner } from './components/LoadingSpinner.js';
import { StatusMessage } from './components/StatusMessage.js';
import { Settings } from './components/Settings.js';
import type { MediaItem } from '../core/types.js';
import type { HistoryEntry } from '../modules/history/types.js';
import { getDefaultProvider } from '../modules/scraper/index.js';
import { getHistory } from '../modules/history/index.js';
import { play } from '../modules/player/index.js';
import { downloadMedia } from '../modules/download/index.js';
import { getDiscord } from '../modules/discord/index.js';
import { getConfig } from '../core/config.js';
import type { CommandContext } from './commands.js';
import { writeFileSync, appendFileSync } from 'fs';
import { homedir } from 'os';
import { join } from 'path';

const provider = getDefaultProvider();
const history = getHistory();
const config = getConfig();

// Debug logging
const LOG_FILE = join(homedir(), '.topster', 'debug.log');

function debugLog(message: string, data?: any) {
  const timestamp = new Date().toISOString();
  const logMessage = `[${timestamp}] ${message}${data ? '\n' + JSON.stringify(data, null, 2) : ''}\n`;
  try {
    appendFileSync(LOG_FILE, logMessage);
  } catch {
    // Ignore write errors
  }
}

type Screen =
  | 'main-menu'
  | 'search'
  | 'search-results'
  | 'trending'
  | 'recent'
  | 'history'
  | 'settings'
  | 'season-select'
  | 'episode-select'
  | 'loading'
  | 'error';

interface AppState {
  screen: Screen;
  searchQuery?: string;
  searchResults?: MediaItem[];
  searchPage?: number;
  searchTotalPages?: number;
  trendingItems?: MediaItem[];
  trendingPage?: number;
  trendingTotalPages?: number;
  recentItems?: MediaItem[];
  recentPage?: number;
  recentTotalPages?: number;
  historyEntries?: HistoryEntry[];
  selectedMedia?: MediaItem;
  seasons?: Season[];
  selectedSeason?: Season;
  episodes?: Episode[];
  loadingMessage?: string;
  errorMessage?: string;
  breadcrumbs: string[];
}

const InkApp: React.FC<{ ctx: CommandContext }> = ({ ctx }) => {
  const { exit } = useApp();
  const [state, setState] = useState<AppState>({
    screen: 'main-menu',
    breadcrumbs: ['Main Menu'],
  });

  useEffect(() => {
    // Initialize log file
    debugLog('=== Topster TUI Started ===');
  }, []);

  const updateState = (updates: Partial<AppState>) => {
    setState(prev => ({ ...prev, ...updates }));
  };

  const handleBack = () => {
    const newBreadcrumbs = [...state.breadcrumbs];
    newBreadcrumbs.pop();

    if (state.screen === 'search-results' || state.screen === 'search') {
      updateState({
        screen: 'main-menu',
        breadcrumbs: ['Main Menu'],
      });
    } else if (state.screen === 'episode-select') {
      updateState({
        screen: 'season-select',
        breadcrumbs: newBreadcrumbs,
      });
    } else if (state.screen === 'season-select') {
      updateState({
        screen: 'main-menu',
        breadcrumbs: ['Main Menu'],
      });
    } else {
      updateState({
        screen: 'main-menu',
        breadcrumbs: ['Main Menu'],
      });
    }
  };

  const handleSearch = async (query: string, page: number = 1) => {
    try {
      debugLog(`Searching for: ${query} (page ${page})`);
      updateState({ screen: 'loading', loadingMessage: 'Searching...', breadcrumbs: state.breadcrumbs.includes('Results') ? state.breadcrumbs : [...state.breadcrumbs, 'Results'] });
      const result = await provider.search(query, page);
      debugLog(`Found ${result.items.length} results (page ${result.currentPage}/${result.totalPages || 1})`);
      updateState({
        screen: 'search-results',
        searchQuery: query,
        searchResults: result.items,
        searchPage: result.currentPage,
        searchTotalPages: result.totalPages,
      });
    } catch (error) {
      debugLog(`Search error: ${error}`);
      updateState({
        screen: 'error',
        errorMessage: error instanceof Error ? error.message : String(error),
      });
    }
  };

  const handleTrending = async (page: number = 1) => {
    try {
      updateState({ screen: 'loading', loadingMessage: 'Loading trending content...', breadcrumbs: state.breadcrumbs.includes('Trending') ? state.breadcrumbs : [...state.breadcrumbs, 'Trending'] });
      const result = await provider.getTrending(page);
      updateState({
        screen: 'trending',
        trendingItems: result.items,
        trendingPage: result.currentPage,
        trendingTotalPages: result.totalPages,
      });
    } catch (error) {
      updateState({
        screen: 'error',
        errorMessage: error instanceof Error ? error.message : String(error),
      });
    }
  };

  const handleRecent = async (page: number = 1) => {
    try {
      updateState({ screen: 'loading', loadingMessage: 'Loading recent content...', breadcrumbs: state.breadcrumbs.includes('Recent') ? state.breadcrumbs : [...state.breadcrumbs, 'Recent'] });
      const result = await provider.getRecent('movie', page);
      updateState({
        screen: 'recent',
        recentItems: result.items,
        recentPage: result.currentPage,
        recentTotalPages: result.totalPages,
      });
    } catch (error) {
      updateState({
        screen: 'error',
        errorMessage: error instanceof Error ? error.message : String(error),
      });
    }
  };

  const handleHistory = async () => {
    const entries = history.getIncomplete();
    updateState({
      screen: 'history',
      historyEntries: entries,
      breadcrumbs: [...state.breadcrumbs, 'Continue Watching'],
    });
  };

  const handleSelectMedia = async (media: MediaItem) => {
    try {
      debugLog(`Selected media: ${media.title} (${media.type})`);
      if (media.type === 'tv') {
        updateState({ screen: 'loading', loadingMessage: 'Loading seasons...', breadcrumbs: [...state.breadcrumbs, media.title] });
        const seasons = await provider.getSeasons(media.id);
        debugLog(`Loaded ${seasons.length} seasons`);
        updateState({
          screen: 'season-select',
          selectedMedia: media,
          seasons,
        });
      } else {
        updateState({ screen: 'loading', loadingMessage: 'Loading video...' });
        // Unmount Ink before playing
        debugLog(`Starting playback for: ${media.title}`);
        exit();
        await playMedia(media, undefined, undefined, ctx, exit);
      }
    } catch (error) {
      debugLog(`Select media error: ${error}`);
      updateState({
        screen: 'error',
        errorMessage: error instanceof Error ? error.message : String(error),
      });
    }
  };

  const handleSelectHistory = async (entry: HistoryEntry) => {
    try {
      debugLog(`Resume from history: ${entry.title}`);
      const media: MediaItem = {
        id: entry.id,
        title: entry.title,
        type: entry.type,
        url: entry.url,
      };

      updateState({ screen: 'loading', loadingMessage: 'Loading video...' });
      exit();

      if (entry.episodeId) {
        const episode: Episode = {
          id: entry.episodeId,
          number: entry.episodeNumber ?? 1,
          title: entry.episodeTitle ?? '',
        };
        await playMedia(media, episode, entry.seasonNumber, ctx, exit);
      } else {
        await playMedia(media, undefined, undefined, ctx, exit);
      }
    } catch (error) {
      debugLog(`History playback error: ${error}`);
      updateState({
        screen: 'error',
        errorMessage: error instanceof Error ? error.message : String(error),
      });
    }
  };

  const handleSelectSeason = async (season: Season) => {
    try {
      updateState({ screen: 'loading', loadingMessage: 'Loading episodes...', breadcrumbs: [...state.breadcrumbs, `Season ${season.number}`] });
      const episodes = await provider.getEpisodes(season.id);
      updateState({
        screen: 'episode-select',
        selectedSeason: season,
        episodes,
      });
    } catch (error) {
      updateState({
        screen: 'error',
        errorMessage: error instanceof Error ? error.message : String(error),
      });
    }
  };

  const handleSelectEpisode = async (episode: Episode) => {
    try {
      if (state.selectedMedia && state.selectedSeason) {
        debugLog(`Selected episode: S${state.selectedSeason.number}E${episode.number} - ${episode.title}`);
        updateState({ screen: 'loading', loadingMessage: 'Loading video...' });
        exit();
        await playMedia(state.selectedMedia, episode, state.selectedSeason.number, ctx, exit);
      }
    } catch (error) {
      debugLog(`Episode playback error: ${error}`);
      updateState({
        screen: 'error',
        errorMessage: error instanceof Error ? error.message : String(error),
      });
    }
  };

  const renderScreen = () => {
    switch (state.screen) {
      case 'main-menu':
        return (
          <>
            <Header showBigLogo />
            <Menu
              items={[
                { label: '🔍 Search', value: 'search', description: 'Search for movies and TV shows' },
                { label: '🔥 Trending', value: 'trending', description: 'Browse trending content' },
                { label: '🆕 Recent', value: 'recent', description: 'Browse recent releases' },
                { label: '📜 Continue Watching', value: 'history', description: 'Resume from watch history' },
                { label: '⚙️  Settings', value: 'settings', description: 'Configure Topster' },
                { label: '❌ Exit', value: 'exit', description: 'Quit the application' },
              ]}
              onSelect={async (value) => {
                if (value === 'search') {
                  updateState({ screen: 'search', breadcrumbs: [...state.breadcrumbs, 'Search'] });
                } else if (value === 'exit') {
                  process.exit(0);
                } else if (value === 'trending') {
                  await handleTrending();
                } else if (value === 'settings') {
                  updateState({ screen: 'settings', breadcrumbs: [...state.breadcrumbs, 'Settings'] });
                } else if (value === 'recent') {
                  await handleRecent();
                } else if (value === 'history') {
                  await handleHistory();
                }
              }}
            />
          </>
        );

      case 'search':
        return (
          <>
            <Header />
            <Breadcrumbs items={state.breadcrumbs} />
            <SearchInput
              placeholder="Enter movie or TV show name..."
              onSubmit={handleSearch}
              onCancel={handleBack}
            />
          </>
        );

      case 'search-results':
        return (
          <>
            <Header />
            <Breadcrumbs items={state.breadcrumbs} />
            {state.searchResults && (
              <MediaList
                title={`Search Results for "${state.searchQuery}"`}
                items={state.searchResults}
                onSelect={handleSelectMedia}
                onCancel={handleBack}
                currentPage={state.searchPage}
                totalPages={state.searchTotalPages}
                onNextPage={() => state.searchQuery && handleSearch(state.searchQuery, (state.searchPage || 1) + 1)}
                onPrevPage={() => state.searchQuery && handleSearch(state.searchQuery, (state.searchPage || 1) - 1)}
              />
            )}
          </>
        );

      case 'trending':
        return (
          <>
            <Header />
            <Breadcrumbs items={state.breadcrumbs} />
            {state.trendingItems && (
              <MediaList
                title="🔥 Trending Now"
                items={state.trendingItems}
                onSelect={handleSelectMedia}
                onCancel={handleBack}
                currentPage={state.trendingPage}
                totalPages={state.trendingTotalPages}
                onNextPage={() => handleTrending((state.trendingPage || 1) + 1)}
                onPrevPage={() => handleTrending((state.trendingPage || 1) - 1)}
              />
            )}
          </>
        );

      case 'recent':
        return (
          <>
            <Header />
            <Breadcrumbs items={state.breadcrumbs} />
            {state.recentItems && (
              <MediaList
                title="🆕 Recent Releases"
                items={state.recentItems}
                onSelect={handleSelectMedia}
                onCancel={handleBack}
                currentPage={state.recentPage}
                totalPages={state.recentTotalPages}
                onNextPage={() => handleRecent((state.recentPage || 1) + 1)}
                onPrevPage={() => handleRecent((state.recentPage || 1) - 1)}
              />
            )}
          </>
        );

      case 'history':
        return (
          <>
            <Header />
            <Breadcrumbs items={state.breadcrumbs} />
            {state.historyEntries && (
              <HistoryList
                entries={state.historyEntries}
                onSelect={handleSelectHistory}
                onCancel={handleBack}
              />
            )}
          </>
        );

      case 'settings':
        return (
          <>
            <Header />
            <Breadcrumbs items={state.breadcrumbs} />
            <Settings
              config={config.getAll()}
              onUpdate={(key, value) => {
                config.set(key, value);
              }}
              onBack={handleBack}
            />
          </>
        );

      case 'season-select':
        return (
          <>
            <Header />
            <Breadcrumbs items={state.breadcrumbs} />
            {state.seasons && state.selectedMedia && (
              <>
                <Box marginBottom={1}>
                  <Text bold>{state.selectedMedia.title}</Text>
                </Box>
                <SeasonList
                  seasons={state.seasons}
                  onSelect={handleSelectSeason}
                  onCancel={handleBack}
                />
              </>
            )}
          </>
        );

      case 'episode-select':
        return (
          <>
            <Header />
            <Breadcrumbs items={state.breadcrumbs} />
            {state.episodes && state.selectedSeason && state.selectedMedia && (
              <>
                <Box marginBottom={1}>
                  <Text bold>{state.selectedMedia.title}</Text>
                </Box>
                <EpisodeList
                  episodes={state.episodes}
                  seasonNumber={state.selectedSeason.number}
                  onSelect={handleSelectEpisode}
                  onCancel={handleBack}
                />
              </>
            )}
          </>
        );

      case 'loading':
        return (
          <>
            <Header />
            <Box marginTop={2}>
              <LoadingSpinner text={state.loadingMessage || 'Loading...'} />
            </Box>
          </>
        );

      case 'error':
        return (
          <>
            <Header />
            <Box marginTop={2}>
              <StatusMessage type="error" message={state.errorMessage || 'An error occurred'} />
            </Box>
            <Box marginTop={1}>
              <Text dimColor>Press ESC to go back</Text>
            </Box>
          </>
        );

      default:
        return <Text>Unknown screen</Text>;
    }
  };

  return (
    <Box flexDirection="column" padding={1}>
      {renderScreen()}
    </Box>
  );
};

const Breadcrumbs: React.FC<{ items: string[] }> = ({ items }) => {
  if (items.length <= 1) return null;

  return (
    <Box marginBottom={1}>
      <Text dimColor>
        {items.join(' › ')}
      </Text>
    </Box>
  );
};

export async function runInkApp(ctx: CommandContext = {
  debug: false,
  download: false,
  noSubs: false,
}): Promise<void> {
  render(<InkApp ctx={ctx} />);
}

async function playMedia(
  media: MediaItem,
  episode?: Episode,
  seasonNumber?: number,
  ctx?: CommandContext,
  exitFn?: () => void
): Promise<void> {
  const isEpisode = !!episode;

  try {
    debugLog(`Getting video sources for: ${episode?.id ?? media.id}`);
    const sources = await provider.getVideoSources(
      episode?.id ?? media.id,
      isEpisode
    );

    const firstSource = sources[0];
    const firstVideoInfo = firstSource?.sources[0];

    if (!firstSource || !firstVideoInfo) {
      debugLog('ERROR: No video sources found');
      console.error('❌ No video sources found');
      process.exit(1);
    }

    debugLog(`Video URL: ${firstVideoInfo.url}`);
    debugLog(`Subtitles: ${firstVideoInfo.subtitles.length} available`);

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
      console.log(`\nSubtitles: ${firstVideoInfo.subtitles.length} available`);
      firstVideoInfo.subtitles.slice(0, 5).forEach((sub, i) => {
        console.log(`  [${i + 1}] ${sub.lang} - ${sub.url}`);
      });
      if (firstVideoInfo.subtitles.length > 5) {
        console.log(`  ... and ${firstVideoInfo.subtitles.length - 5} more`);
      }
      console.log(`\nResume Position: ${formatTime(startTime)}`);
      console.log(`History Entry: ${historyEntry ? 'Yes' : 'No'}`);
      console.log('\n=== END DRY RUN ===\n');
      debugLog('Dry run completed');
      process.exit(0);
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
    debugLog(`Resume position: ${startTime}s`);

    if (ctx?.download) {
      debugLog('Download mode enabled');
      const result = await downloadMedia(
        firstVideoInfo,
        media,
        episode,
        seasonNumber,
        {
          outputDir: ctx.downloadPath,
          onProgress: (percent) => {
            console.log(`Download progress: ${percent}%`);
          },
        }
      );

      if (!result.success) {
        debugLog(`Download failed: ${result.error}`);
        console.error(`❌ Download failed: ${result.error}`);
        process.exit(1);
      }
      console.log(`✓ Downloaded to: ${result.filePath}`);
      process.exit(0);
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

    console.log(`\n🎬 Playing: ${title}\n`);
    debugLog(`Starting player: ${title}`);

    const result = await play(
      firstVideoInfo.url,
      {
        startTime: startTime > 60 ? startTime - 10 : 0,
        fullscreen: true,
      },
      subtitles,
      firstVideoInfo.referer
    );

    debugLog(`Playback finished. Exit code: ${result.exitCode}`);

    // Update history with final position
    if (result.position) {
      history.update(media.id, episode?.id, result.position, '00:00:00');
      debugLog(`Updated history position: ${result.position}`);
    }

    // Clear Discord RPC
    await discord.clearPresence();
    discord.disconnect();

    console.log('\n✓ Playback finished\n');
    debugLog('Playback completed successfully');
    process.exit(0);
  } catch (error) {
    debugLog(`FATAL ERROR in playMedia: ${error instanceof Error ? error.stack : error}`);
    console.error(`\n❌ Error: ${error instanceof Error ? error.message : String(error)}\n`);
    console.error(`Check debug log: ${LOG_FILE}\n`);
    process.exit(1);
  }
}

function parseTime(time: string): number {
  const parts = time.split(':').map(Number);
  if (parts.length === 3) {
    return (parts[0] ?? 0) * 3600 + (parts[1] ?? 0) * 60 + (parts[2] ?? 0);
  }
  return 0;
}
