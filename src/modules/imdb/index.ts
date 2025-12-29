import type { IMDbInfo, IMDbSearchResult, IMDbError } from './types.js';
import { getConfig } from '../../core/config.js';
import { existsSync, readFileSync, writeFileSync, mkdirSync } from 'fs';
import { join } from 'path';

const OMDB_BASE_URL = 'https://www.omdbapi.com/';

// Cache for IMDb lookups - keyed by both IMDb ID and title+year
const imdbCache = new Map<string, { data: IMDbInfo; timestamp: number }>();
const CACHE_TTL = 1000 * 60 * 60 * 24 * 7; // 7 days - be very conservative with API calls

// Persistent cache file
let CACHE_FILE_PATH: string;

/**
 * Initialize cache from disk
 */
function loadCache(): void {
  try {
    const config = getConfig();
    CACHE_FILE_PATH = join(config.getDataDir(), 'imdb-cache.json');

    if (existsSync(CACHE_FILE_PATH)) {
      const data = readFileSync(CACHE_FILE_PATH, 'utf-8');
      const cached = JSON.parse(data);

      // Load into memory cache
      for (const [key, value] of Object.entries(cached)) {
        imdbCache.set(key, value as { data: IMDbInfo; timestamp: number });
      }
    }
  } catch (error) {
    // Ignore cache load errors - cache will rebuild
  }
}

/**
 * Save cache to disk
 */
function saveCache(): void {
  try {
    if (!CACHE_FILE_PATH) {
      const config = getConfig();
      CACHE_FILE_PATH = join(config.getDataDir(), 'imdb-cache.json');
    }

    // Convert Map to object for JSON serialization
    const cacheObj: Record<string, { data: IMDbInfo; timestamp: number }> = {};
    for (const [key, value] of imdbCache.entries()) {
      cacheObj[key] = value;
    }

    writeFileSync(CACHE_FILE_PATH, JSON.stringify(cacheObj, null, 2));
  } catch (error) {
    // Ignore cache save errors
  }
}

// Load cache on module initialization
loadCache();

/**
 * Search for a title on OMDb and get the best match
 */
export async function searchIMDb(title: string, year?: string): Promise<IMDbSearchResult[]> {
  const config = getConfig();
  const apiKey = config.get('omdbApiKey');

  if (!apiKey) {
    throw new Error('OMDb API key not configured. Set it in Settings > Features > OMDb API Key');
  }

  const params = new URLSearchParams({
    apikey: apiKey,
    s: title,
    type: 'movie,series',
  });

  if (year) {
    params.append('y', year);
  }

  const response = await fetch(`${OMDB_BASE_URL}?${params}`);
  const data = await response.json();

  if (data.Response === 'False') {
    throw new Error((data as IMDbError).Error || 'No results found');
  }

  return data.Search || [];
}

/**
 * Get detailed information about a title by IMDb ID
 */
export async function getIMDbInfo(imdbId: string): Promise<IMDbInfo> {
  const config = getConfig();
  const apiKey = config.get('omdbApiKey');

  if (!apiKey) {
    throw new Error('OMDb API key not configured. Set it in Settings > Features > OMDb API Key');
  }

  // Check cache first
  const cached = imdbCache.get(imdbId);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
    return cached.data;
  }

  const params = new URLSearchParams({
    apikey: apiKey,
    i: imdbId,
    plot: 'full',
  });

  const response = await fetch(`${OMDB_BASE_URL}?${params}`);
  const data = await response.json();

  if (data.Response === 'False') {
    throw new Error((data as IMDbError).Error || 'Title not found');
  }

  // Cache the result
  imdbCache.set(imdbId, { data, timestamp: Date.now() });
  saveCache(); // Persist to disk

  return data as IMDbInfo;
}

/**
 * Lookup IMDb info by title and optional year
 * IMPORTANT: Only call this when user explicitly presses 'i' - conserves API quota (1000/day limit)
 */
export async function lookupIMDbByTitle(title: string, year?: string): Promise<IMDbInfo> {
  const config = getConfig();
  const apiKey = config.get('omdbApiKey');

  if (!apiKey) {
    throw new Error('OMDb API key not configured. Set it in Settings > Features > OMDb API Key');
  }

  // Create cache key from title+year to avoid duplicate API calls
  const cacheKey = `title:${title.toLowerCase()}${year ? `:${year}` : ''}`;
  const cached = imdbCache.get(cacheKey);
  if (cached && Date.now() - cached.timestamp < CACHE_TTL) {
    return cached.data;
  }

  const params = new URLSearchParams({
    apikey: apiKey,
    t: title,
    plot: 'full',
  });

  if (year) {
    params.append('y', year);
  }

  const response = await fetch(`${OMDB_BASE_URL}?${params}`);
  const data = await response.json();

  if (data.Response === 'False') {
    // Don't fallback to search - it uses 2 API calls instead of 1
    // Just throw the error to conserve API quota
    throw new Error((data as IMDbError).Error || 'Title not found');
  }

  const info = data as IMDbInfo;

  // Cache by both title+year AND IMDb ID for maximum cache hits
  if (info.imdbID) {
    const timestamp = Date.now();
    imdbCache.set(info.imdbID, { data: info, timestamp });
    imdbCache.set(cacheKey, { data: info, timestamp });
    saveCache(); // Persist to disk
  }

  return info;
}
