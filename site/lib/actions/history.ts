'use server';

import { mcpClient } from '../mcp-client';
import type { HistoryItem } from '../types';

export async function getWatchHistory(): Promise<HistoryItem[]> {
  try {
    const result = await mcpClient.getHistory();
    return result.content?.[0]?.text ? JSON.parse(result.content[0].text) : [];
  } catch (error) {
    console.error('Get history error:', error);
    throw new Error('Failed to get watch history');
  }
}

export async function updateWatchHistory(params: {
  mediaId: string;
  title: string;
  type: 'movie' | 'tv';
  position: number;
  duration: number;
  season?: number;
  episode?: number;
  image?: string;
}): Promise<void> {
  try {
    await mcpClient.updateHistory(
      params.mediaId,
      params.title,
      params.type,
      params.position,
      params.duration,
      params.season,
      params.episode,
      params.image
    );
  } catch (error) {
    console.error('Update history error:', error);
    throw new Error('Failed to update watch history');
  }
}
