// MCP Tool definitions

import type { MediaItem, Season, Episode, HistoryEntry, VideoSource } from '../core/types';
import type { Provider } from '../modules/scraper';
import type { SQLiteHistoryManager } from '../modules/history';

// Lazy initialization - don't block server startup
let _provider: Provider | null = null;
let _history: SQLiteHistoryManager | null = null;

function getProvider(): Provider {
  if (!_provider) {
    const { getDefaultProvider } = require('../modules/scraper');
    _provider = getDefaultProvider();
  }
  return _provider;
}

function getHistoryManager(): SQLiteHistoryManager {
  if (!_history) {
    const { getHistory } = require('../modules/history');
    _history = getHistory();
  }
  return _history;
}

export interface ToolDefinition {
  name: string;
  description: string;
  inputSchema: {
    type: 'object';
    properties: Record<string, {
      type: string;
      description: string;
      enum?: string[];
    }>;
    required?: string[];
  };
}

export const tools: ToolDefinition[] = [
  {
    name: 'search',
    description: 'Search for movies and TV shows',
    inputSchema: {
      type: 'object',
      properties: {
        query: {
          type: 'string',
          description: 'Search query for movies or TV shows',
        },
      },
      required: ['query'],
    },
  },
  {
    name: 'get_trending',
    description: 'Get trending movies and TV shows',
    inputSchema: {
      type: 'object',
      properties: {},
    },
  },
  {
    name: 'get_recent',
    description: 'Get recently added content',
    inputSchema: {
      type: 'object',
      properties: {
        type: {
          type: 'string',
          description: 'Content type',
          enum: ['movie', 'tv'],
        },
      },
      required: ['type'],
    },
  },
  {
    name: 'get_seasons',
    description: 'Get seasons for a TV show',
    inputSchema: {
      type: 'object',
      properties: {
        media_id: {
          type: 'string',
          description: 'The media ID of the TV show',
        },
      },
      required: ['media_id'],
    },
  },
  {
    name: 'get_episodes',
    description: 'Get episodes for a season',
    inputSchema: {
      type: 'object',
      properties: {
        season_id: {
          type: 'string',
          description: 'The season ID',
        },
      },
      required: ['season_id'],
    },
  },
  {
    name: 'get_video_sources',
    description: 'Get video streaming sources for a movie or episode',
    inputSchema: {
      type: 'object',
      properties: {
        id: {
          type: 'string',
          description: 'The media ID or episode ID',
        },
        is_episode: {
          type: 'boolean',
          description: 'Whether the ID is an episode ID (true) or movie ID (false)',
        },
      },
      required: ['id'],
    },
  },
  {
    name: 'get_history',
    description: 'Get watch history',
    inputSchema: {
      type: 'object',
      properties: {
        limit: {
          type: 'number',
          description: 'Maximum number of entries to return',
        },
        incomplete_only: {
          type: 'boolean',
          description: 'Only return incomplete (unfinished) entries',
        },
      },
    },
  },
  {
    name: 'get_config',
    description: 'Get current configuration',
    inputSchema: {
      type: 'object',
      properties: {},
    },
  },
  {
    name: 'set_config',
    description: 'Update configuration settings',
    inputSchema: {
      type: 'object',
      properties: {
        key: {
          type: 'string',
          description: 'Configuration key to update',
        },
        value: {
          type: 'string',
          description: 'New value for the configuration key',
        },
      },
      required: ['key', 'value'],
    },
  },
  {
    name: 'update_history',
    description: 'Update watch history with playback position',
    inputSchema: {
      type: 'object',
      properties: {
        id: {
          type: 'string',
          description: 'Media ID',
        },
        episode_id: {
          type: 'string',
          description: 'Episode ID (for TV shows)',
        },
        position: {
          type: 'string',
          description: 'Current playback position (HH:MM:SS format)',
        },
        duration: {
          type: 'string',
          description: 'Total video duration (HH:MM:SS format)',
        },
      },
      required: ['id', 'position', 'duration'],
    },
  },
];

export type ToolResult = {
  content: Array<{ type: 'text'; text: string }>;
  isError?: boolean;
};

export async function executeTool(
  name: string,
  args: Record<string, unknown>
): Promise<ToolResult> {
  try {
    switch (name) {
      case 'search': {
        const query = args.query as string;
        const result = await getProvider().search(query);
        return success(result.items);
      }

      case 'get_trending': {
        const items = await getProvider().getTrending();
        return success(items);
      }

      case 'get_recent': {
        const type = args.type as 'movie' | 'tv';
        const items = await getProvider().getRecent(type);
        return success(items);
      }

      case 'get_seasons': {
        const mediaId = args.media_id as string;
        const seasons = await getProvider().getSeasons(mediaId);
        return success(seasons);
      }

      case 'get_episodes': {
        const seasonId = args.season_id as string;
        const episodes = await getProvider().getEpisodes(seasonId);
        return success(episodes);
      }

      case 'get_video_sources': {
        const id = args.id as string;
        const isEpisode = args.is_episode as boolean ?? false;
        const sources = await getProvider().getVideoSources(id, isEpisode);
        return success(sources);
      }

      case 'get_history': {
        const limit = args.limit as number | undefined;
        const incompleteOnly = args.incomplete_only as boolean | undefined;
        const history = getHistoryManager();

        let entries: HistoryEntry[];
        if (incompleteOnly) {
          entries = await history.getIncomplete();
        } else {
          entries = limit ? await history.getRecent(limit) : await history.getAll();
        }
        return success(entries);
      }

      case 'get_config': {
        const { getConfig } = require('../core/config');
        return success(getConfig().getAll());
      }

      case 'set_config': {
        const key = args.key as string;
        const value = args.value as string;
        const { getConfig } = require('../core/config');
        const config = getConfig();

        // Type-safe config update
        const current = config.getAll();
        if (!(key in current)) {
          return error(`Unknown configuration key: ${key}`);
        }

        // Convert value to appropriate type
        const currentValue = current[key as keyof typeof current];
        let typedValue: unknown = value;

        if (typeof currentValue === 'boolean') {
          typedValue = value === 'true';
        } else if (typeof currentValue === 'number') {
          typedValue = parseInt(value);
        }

        config.set(key as keyof typeof current, typedValue as never);
        return success({ key, value: typedValue, updated: true });
      }

      case 'update_history': {
        const id = args.id as string;
        const episodeId = args.episode_id as string | undefined;
        const position = args.position as string;
        const duration = args.duration as string;
        const history = getHistoryManager();

        await history.update(id, episodeId, position, duration);
        return success({ updated: true });
      }

      default:
        return error(`Unknown tool: ${name}`);
    }
  } catch (err) {
    return error(err instanceof Error ? err.message : String(err));
  }
}

function success(data: unknown): ToolResult {
  return {
    content: [{ type: 'text', text: JSON.stringify(data, null, 2) }],
  };
}

function error(message: string): ToolResult {
  return {
    content: [{ type: 'text', text: `Error: ${message}` }],
    isError: true,
  };
}
