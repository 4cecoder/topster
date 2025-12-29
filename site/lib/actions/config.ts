'use server';

import { mcpClient } from '../mcp-client';
import type { TopsterConfig } from '../types';

export async function getConfig(): Promise<Partial<TopsterConfig>> {
  try {
    const result = await mcpClient.getConfig();
    if (result.content?.[0]?.text) {
      return JSON.parse(result.content[0].text);
    }
    return {};
  } catch (error) {
    console.error('Get config error:', error);
    throw new Error('Failed to get configuration');
  }
}

export async function updateConfig(
  config: Partial<TopsterConfig>
): Promise<void> {
  try {
    await mcpClient.setConfig(config);
  } catch (error) {
    console.error('Update config error:', error);
    throw new Error('Failed to update configuration');
  }
}
