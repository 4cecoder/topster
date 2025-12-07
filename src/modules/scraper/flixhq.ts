// FlixHQ provider implementation

import * as cheerio from 'cheerio';
import { fetchHtml, fetchJson } from './http';
import { ScrapingError, NoResultsError, DecryptionError } from '../../core/errors';
import type {
  Provider,
  MediaItem,
  Season,
  Episode,
  VideoSource,
  VideoInfo,
  SearchResult,
  MediaType,
  Subtitle,
} from '../../core/types';
import { getConfig } from '../../core/config';
import { searchCache, mediaCache, episodeCache } from '../cache';

interface SourceResponse {
  type: string;
  link: string;
  sources?: Array<{ file: string; type: string }>;
  tracks?: Array<{ file: string; kind: string; label: string }>;
}

interface ServerInfo {
  id: string;
  name: string;
}

interface DecryptedResponse {
  sources?: Array<{ file: string; type?: string }>;
  tracks?: Array<{ file: string; kind: string; label: string }>;
}

const DECRYPT_API_URL = 'https://dec.eatmynerds.live';

export class FlixHQProvider implements Provider {
  name = 'FlixHQ';
  baseUrl: string;

  constructor(baseUrl?: string) {
    this.baseUrl = baseUrl || getConfig().get('baseUrl');
  }

  async search(query: string, page: number = 1): Promise<SearchResult> {
    const cacheKey = `search:${query}:${page}`;
    const cached = searchCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const searchUrl = `${this.baseUrl}/search/${encodeURIComponent(query.replace(/\s+/g, '-'))}?page=${page}`;
    const html = await fetchHtml(searchUrl);
    const $ = cheerio.load(html);

    const items: MediaItem[] = [];

    $('.flw-item').each((_, element) => {
      const $el = $(element);
      const $link = $el.find('.film-poster-ahref');
      const $detail = $el.find('.film-detail');

      const url = $link.attr('href') || '';
      const title = $link.attr('title') || $detail.find('.film-name a').text().trim();
      const image = $el.find('.film-poster-img').attr('data-src') || $el.find('.film-poster-img').attr('src');

      const id = this.extractId(url);
      const type = url.includes('/tv/') ? 'tv' : 'movie';

      const fdiItems = $el.find('.fdi-item');
      let quality: string | undefined;
      let duration: string | undefined;

      fdiItems.each((_, fdi) => {
        const text = $(fdi).text().trim();
        if (text.match(/\d+m/)) duration = text;
        else if (text.match(/HD|SD|CAM/)) quality = text;
      });

      const year = $el.find('.fdi-item').first().text().trim();

      if (id && title) {
        items.push({
          id,
          title,
          type: type as MediaType,
          year,
          quality,
          duration,
          image,
          url: `${this.baseUrl}${url}`,
        });
      }
    });

    if (items.length === 0 && page === 1) {
      throw new NoResultsError(query);
    }

    // Parse pagination info
    const paginationNav = $('.pagination');
    const totalPages = this.parseTotalPages($, paginationNav);

    const result: SearchResult = {
      items,
      currentPage: page,
      totalPages,
    };

    searchCache.set(cacheKey, result);
    return result;
  }

  async getTrending(page: number = 1): Promise<SearchResult> {
    const cacheKey = `trending:${page}`;
    const cached = mediaCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const url = page === 1 ? `${this.baseUrl}/home` : `${this.baseUrl}/home?page=${page}`;
    const html = await fetchHtml(url);
    const $ = cheerio.load(html);

    const items: MediaItem[] = [];

    $('#trending-movies .flw-item, #trending-tv .flw-item').each((_, element) => {
      const item = this.parseMediaItem($, element);
      if (item) items.push(item);
    });

    const paginationNav = $('.pagination');
    const totalPages = this.parseTotalPages($, paginationNav);

    const result: SearchResult = {
      items,
      currentPage: page,
      totalPages,
    };

    mediaCache.set(cacheKey, result);
    return result;
  }

  async getRecent(type: MediaType, page: number = 1): Promise<SearchResult> {
    const cacheKey = `recent:${type}:${page}`;
    const cached = mediaCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const path = type === 'movie' ? '/movie' : '/tv-show';
    const url = `${this.baseUrl}${path}${page > 1 ? `?page=${page}` : ''}`;
    const html = await fetchHtml(url);
    const $ = cheerio.load(html);

    const items: MediaItem[] = [];

    $('.flw-item').each((_, element) => {
      const item = this.parseMediaItem($, element);
      if (item) items.push(item);
    });

    const paginationNav = $('.pagination');
    const totalPages = this.parseTotalPages($, paginationNav);

    const result: SearchResult = {
      items,
      currentPage: page,
      totalPages,
    };

    mediaCache.set(cacheKey, result);
    return result;
  }

  async getSeasons(mediaId: string): Promise<Season[]> {
    const cacheKey = `seasons:${mediaId}`;
    const cached = episodeCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const html = await fetchHtml(`${this.baseUrl}/ajax/v2/tv/seasons/${mediaId}`, {
      headers: { 'X-Requested-With': 'XMLHttpRequest' },
    });
    const $ = cheerio.load(html);

    const seasons: Season[] = [];

    $('.dropdown-item').each((index, element) => {
      const $el = $(element);
      const id = $el.attr('data-id') || '';
      const title = $el.text().trim();
      const number = parseInt(title.replace(/\D/g, '')) || index + 1;

      if (id) {
        seasons.push({ id, number, title });
      }
    });

    episodeCache.set(cacheKey, seasons);
    return seasons;
  }

  async getEpisodes(seasonId: string): Promise<Episode[]> {
    const cacheKey = `episodes:${seasonId}`;
    const cached = episodeCache.get(cacheKey);
    if (cached) {
      return cached;
    }

    const html = await fetchHtml(`${this.baseUrl}/ajax/v2/season/episodes/${seasonId}`, {
      headers: { 'X-Requested-With': 'XMLHttpRequest' },
    });
    const $ = cheerio.load(html);

    const episodes: Episode[] = [];

    $('.eps-item').each((_, element) => {
      const $el = $(element);
      const id = $el.attr('data-id') || '';
      const title = $el.find('strong').text().trim() || $el.attr('title') || '';
      const epNum = $el.find('.episode-number').text().trim();
      const number = parseInt(epNum.replace(/\D/g, '')) || episodes.length + 1;

      if (id) {
        episodes.push({ id, number, title });
      }
    });

    episodeCache.set(cacheKey, episodes);
    return episodes;
  }

  async getVideoSources(id: string, isEpisode: boolean = false): Promise<VideoSource[]> {
    // For TV episodes, id is the episode data-id from the episode list
    // For movies, id is the media_id, and we need to get the episode_id first

    let servers: ServerInfo[] = [];

    if (isEpisode) {
      // TV show episode - get servers for this episode
      const html = await fetchHtml(`${this.baseUrl}/ajax/v2/episode/servers/${id}`, {
        headers: { 'X-Requested-With': 'XMLHttpRequest' },
      });
      const $ = cheerio.load(html);

      $('a.link-item').each((_, element) => {
        const $el = $(element);
        const serverId = $el.attr('data-id') || '';
        // title is "Server Vidcloud", extract just the server name
        const rawTitle = $el.attr('title') || '';
        const name = rawTitle.replace(/^Server\s*/i, '') || $el.find('span').text().trim() || '';

        if (serverId) {
          servers.push({ id: serverId, name });
        }
      });
    } else {
      // Movie - get servers from /ajax/movie/episodes/{media_id}
      const html = await fetchHtml(`${this.baseUrl}/ajax/movie/episodes/${id}`, {
        headers: { 'X-Requested-With': 'XMLHttpRequest' },
      });
      const $ = cheerio.load(html);

      // Parse server links - format: /watch-movie/watch-xxx-MEDIAID.EPISODEID
      $('a.link-item').each((_, element) => {
        const $el = $(element);
        const href = $el.attr('href') || '';
        const name = $el.attr('title') || $el.find('span').text().trim() || '';

        // Extract episode_id from href (the number after the dot)
        const match = href.match(/\.(\d+)$/);
        if (match) {
          servers.push({ id: match[1], name });
        }
      });
    }

    const sources: VideoSource[] = [];
    const preferredProvider = getConfig().get('provider');

    // Sort servers to prioritize preferred provider
    servers.sort((a, b) => {
      if (a.name.toLowerCase().includes(preferredProvider.toLowerCase())) return -1;
      if (b.name.toLowerCase().includes(preferredProvider.toLowerCase())) return 1;
      return 0;
    });

    for (const server of servers) {
      try {
        const sourceData = await this.getSourceFromServer(server.id);
        if (sourceData.length > 0) {
          sources.push({
            provider: server.name,
            sources: sourceData,
          });
          // Found working sources, no need to try more servers
          break;
        }
      } catch (error) {
        // Continue to next server on error
        console.error(`Failed to get sources from ${server.name}:`, error);
      }
    }

    return sources;
  }

  private async getSourceFromServer(serverId: string): Promise<VideoInfo[]> {
    // Get embed link from /ajax/episode/sources/{episode_id}
    const response = await fetchJson<SourceResponse>(
      `${this.baseUrl}/ajax/episode/sources/${serverId}`,
      { headers: { 'X-Requested-With': 'XMLHttpRequest' } }
    );

    if (response.type === 'iframe' && response.link) {
      return this.extractFromEmbed(response.link);
    }

    if (response.sources && response.sources.length > 0) {
      const subtitles: Subtitle[] = (response.tracks || [])
        .filter(t => t.kind === 'captions' || t.kind === 'subtitles')
        .map(t => ({
          url: t.file,
          lang: t.label?.toLowerCase() || 'unknown',
          label: t.label || 'Unknown',
        }));

      return response.sources.map(s => ({
        url: s.file,
        subtitles,
      }));
    }

    return [];
  }

  private async extractFromEmbed(embedUrl: string): Promise<VideoInfo[]> {
    // Use the decryption API to extract video sources from embed
    // Retry up to 3 times since the API can be flaky
    const maxRetries = 3;
    let lastError: unknown;

    for (let attempt = 1; attempt <= maxRetries; attempt++) {
      try {
        const decryptUrl = `${DECRYPT_API_URL}/?url=${encodeURIComponent(embedUrl)}`;
        const response = await fetchJson<DecryptedResponse>(decryptUrl);

        if (response.sources && response.sources.length > 0) {
          const m3u8Sources = response.sources.filter(s => s.file && s.file.includes('.m3u8'));

          if (m3u8Sources.length > 0) {
            const subtitles: Subtitle[] = (response.tracks || [])
              .filter(t => t.kind === 'captions' || t.kind === 'subtitles')
              .map(t => ({
                url: t.file,
                lang: t.label?.toLowerCase() || 'unknown',
                label: t.label || 'Unknown',
              }));

            return m3u8Sources.map(s => ({
              url: s.file,
              subtitles,
              referer: this.baseUrl,
            }));
          }
        }

        // API returned empty sources, retry after a short delay
        if (attempt < maxRetries) {
          await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
        }
      } catch (error) {
        lastError = error;
        if (attempt < maxRetries) {
          await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
        }
      }
    }

    // All retries failed
    console.error('Failed to decrypt embed after', maxRetries, 'attempts:', lastError || 'empty response');
    throw new DecryptionError(`Failed to extract video from embed: ${embedUrl}`);
  }

  private parseMediaItem($: cheerio.CheerioAPI, element: Parameters<typeof $>[0]): MediaItem | null {
    const $el = $(element);
    const $link = $el.find('.film-poster-ahref');
    const url = $link.attr('href') || '';
    const title = $link.attr('title') || $el.find('.film-name a').text().trim();
    const image = $el.find('.film-poster-img').attr('data-src') ||
                  $el.find('.film-poster-img').attr('src');

    const id = this.extractId(url);
    if (!id || !title) return null;

    const type: MediaType = url.includes('/tv/') ? 'tv' : 'movie';

    const fdiItems = $el.find('.fdi-item');
    let quality: string | undefined;
    let duration: string | undefined;
    let year: string | undefined;

    fdiItems.each((i, fdi) => {
      const text = $(fdi).text().trim();
      if (text.match(/^\d{4}$/)) year = text;
      else if (text.match(/\d+m/)) duration = text;
      else if (text.match(/HD|SD|CAM|4K/i)) quality = text;
    });

    return {
      id,
      title,
      type,
      year,
      quality,
      duration,
      image,
      url: url.startsWith('http') ? url : `${this.baseUrl}${url}`,
    };
  }

  private extractId(url: string): string {
    const match = url.match(/\-(\d+)$/);
    return match?.[1] ?? '';
  }

  private parseTotalPages($: cheerio.CheerioAPI, paginationNav: cheerio.Cheerio<any>): number | undefined {
    if (!paginationNav.length) return undefined;

    // Look for the last page number in pagination
    let maxPage = 1;
    paginationNav.find('a').each((_, element) => {
      const text = $(element).text().trim();
      const pageNum = parseInt(text);
      if (!isNaN(pageNum) && pageNum > maxPage) {
        maxPage = pageNum;
      }
    });

    return maxPage > 1 ? maxPage : undefined;
  }
}

export function createFlixHQProvider(baseUrl?: string): Provider {
  return new FlixHQProvider(baseUrl);
}
