// HTTP client for scraping

import { NetworkError } from '../../core/errors';

export interface FetchOptions {
  headers?: Record<string, string>;
  timeout?: number;
  referer?: string;
}

const DEFAULT_HEADERS: Record<string, string> = {
  'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
  'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
  'Accept-Language': 'en-US,en;q=0.5',
  'Connection': 'keep-alive',
  'Upgrade-Insecure-Requests': '1',
};

export async function fetchHtml(url: string, options: FetchOptions = {}): Promise<string> {
  const headers: Record<string, string> = { ...DEFAULT_HEADERS, ...options.headers };

  if (options.referer) {
    headers['Referer'] = options.referer;
  }

  try {
    const controller = new AbortController();
    const timeout = options.timeout || 30000;
    const timeoutId = setTimeout(() => controller.abort(), timeout);

    const response = await fetch(url, {
      headers,
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new NetworkError(
        `HTTP ${response.status}: ${response.statusText}`,
        response.status,
        url
      );
    }

    return await response.text();
  } catch (error) {
    if (error instanceof NetworkError) throw error;
    if (error instanceof Error && error.name === 'AbortError') {
      throw new NetworkError('Request timeout', undefined, url);
    }
    throw new NetworkError(`Fetch failed: ${error}`, undefined, url);
  }
}

export async function fetchJson<T>(url: string, options: FetchOptions = {}): Promise<T> {
  const headers: Record<string, string> = {
    ...DEFAULT_HEADERS,
    ...options.headers,
    'Accept': 'application/json',
  };

  if (options.referer) {
    headers['Referer'] = options.referer;
  }

  try {
    const controller = new AbortController();
    const timeout = options.timeout || 30000;
    const timeoutId = setTimeout(() => controller.abort(), timeout);

    const response = await fetch(url, {
      headers,
      signal: controller.signal,
    });

    clearTimeout(timeoutId);

    if (!response.ok) {
      throw new NetworkError(
        `HTTP ${response.status}: ${response.statusText}`,
        response.status,
        url
      );
    }

    return await response.json() as T;
  } catch (error) {
    if (error instanceof NetworkError) throw error;
    if (error instanceof Error && error.name === 'AbortError') {
      throw new NetworkError('Request timeout', undefined, url);
    }
    throw new NetworkError(`Fetch JSON failed: ${error}`, undefined, url);
  }
}
