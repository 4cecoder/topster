/**
 * MCP (Model Context Protocol) client for communicating with the Topster CLI
 * Optimized for Bun runtime with native fetch
 */

interface MCPRequest {
  jsonrpc: '2.0';
  id: string | number;
  method: string;
  params?: any;
}

interface MCPResponse<T = any> {
  jsonrpc: '2.0';
  id: string | number;
  result?: T;
  error?: {
    code: number;
    message: string;
    data?: any;
  };
}

interface MCPToolResponse {
  content: Array<{
    type: string;
    text: string;
  }>;
}

export class MCPClient {
  private baseUrl: string;
  private requestId = 0;

  constructor(host: string = 'localhost', port: number = 3847) {
    this.baseUrl = `http://${host}:${port}`;
  }

  private async request<T>(method: string, params?: any): Promise<T> {
    const id = ++this.requestId;
    const request: MCPRequest = {
      jsonrpc: '2.0',
      id,
      method,
      params,
    };

    try {
      const response = await fetch(`${this.baseUrl}/messages`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(request),
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const data: MCPResponse<T> = await response.json();

      if (data.error) {
        throw new Error(data.error.message);
      }

      if (!data.result) {
        throw new Error('No result in MCP response');
      }

      return data.result;
    } catch (error) {
      // If MCP server is not running, throw a helpful error
      if (error instanceof TypeError && error.message.includes('fetch')) {
        throw new Error(
          'MCP server is not running. Start it with: topster --mcp'
        );
      }
      throw error;
    }
  }

  async search(query: string): Promise<MCPToolResponse> {
    return this.request<MCPToolResponse>('tools/call', {
      name: 'search',
      arguments: { query },
    });
  }

  async getTrending(): Promise<MCPToolResponse> {
    return this.request<MCPToolResponse>('tools/call', {
      name: 'get_trending',
      arguments: {},
    });
  }

  async getRecent(): Promise<MCPToolResponse> {
    return this.request<MCPToolResponse>('tools/call', {
      name: 'get_recent',
      arguments: {},
    });
  }

  async getSeasons(mediaId: string): Promise<MCPToolResponse> {
    return this.request<MCPToolResponse>('tools/call', {
      name: 'get_seasons',
      arguments: { mediaId },
    });
  }

  async getEpisodes(mediaId: string, season: number): Promise<MCPToolResponse> {
    return this.request<MCPToolResponse>('tools/call', {
      name: 'get_episodes',
      arguments: { mediaId, season },
    });
  }

  async getVideoSources(episodeId: string, category?: string, server?: string): Promise<MCPToolResponse> {
    return this.request<MCPToolResponse>('tools/call', {
      name: 'get_video_sources',
      arguments: { episodeId, category, server },
    });
  }

  async getHistory(): Promise<MCPToolResponse> {
    return this.request<MCPToolResponse>('tools/call', {
      name: 'get_history',
      arguments: {},
    });
  }

  async updateHistory(
    mediaId: string,
    title: string,
    type: 'movie' | 'tv',
    position: number,
    duration: number,
    season?: number,
    episode?: number,
    image?: string
  ): Promise<MCPToolResponse> {
    return this.request<MCPToolResponse>('tools/call', {
      name: 'update_history',
      arguments: {
        mediaId,
        title,
        type,
        position,
        duration,
        season,
        episode,
        image,
      },
    });
  }

  async getConfig(): Promise<MCPToolResponse> {
    return this.request<MCPToolResponse>('tools/call', {
      name: 'get_config',
      arguments: {},
    });
  }

  async setConfig(config: Record<string, any>): Promise<MCPToolResponse> {
    return this.request<MCPToolResponse>('tools/call', {
      name: 'set_config',
      arguments: { config },
    });
  }
}

export const mcpClient = new MCPClient();
