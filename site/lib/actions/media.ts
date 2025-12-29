'use server';

import { mcpClient } from '../mcp-client';
import type { MediaItem, Season, Episode, StreamingData } from '../types';

export async function searchMedia(query: string): Promise<MediaItem[]> {
  try {
    const result = await mcpClient.search(query);
    return result.content?.[0]?.text ? JSON.parse(result.content[0].text) : [];
  } catch (error) {
    console.error('Search error:', error);
    throw new Error('Failed to search media');
  }
}

export async function getTrendingMedia(): Promise<MediaItem[]> {
  try {
    const result = await mcpClient.getTrending();
    return result.content?.[0]?.text ? JSON.parse(result.content[0].text) : [];
  } catch (error) {
    console.error('Get trending error:', error);
    throw new Error('Failed to get trending media');
  }
}

export async function getRecentMedia(): Promise<MediaItem[]> {
  try {
    const result = await mcpClient.getRecent();
    return result.content?.[0]?.text ? JSON.parse(result.content[0].text) : [];
  } catch (error) {
    console.error('Get recent error:', error);
    throw new Error('Failed to get recent media');
  }
}

export async function getMediaSeasons(mediaId: string): Promise<Season[]> {
  try {
    const result = await mcpClient.getSeasons(mediaId);
    return result.content?.[0]?.text ? JSON.parse(result.content[0].text) : [];
  } catch (error) {
    console.error('Get seasons error:', error);
    throw new Error('Failed to get seasons');
  }
}

export async function getSeasonEpisodes(
  mediaId: string,
  season: number
): Promise<Episode[]> {
  try {
    const result = await mcpClient.getEpisodes(mediaId, season);
    return result.content?.[0]?.text ? JSON.parse(result.content[0].text) : [];
  } catch (error) {
    console.error('Get episodes error:', error);
    throw new Error('Failed to get episodes');
  }
}

export async function getVideoSources(
  episodeId: string,
  category?: string,
  server?: string
): Promise<StreamingData> {
  try {
    const result = await mcpClient.getVideoSources(episodeId, category, server);
    return result.content?.[0]?.text ? JSON.parse(result.content[0].text) : { sources: [], subtitles: [] };
  } catch (error) {
    console.error('Get video sources error:', error);
    throw new Error('Failed to get video sources');
  }
}
