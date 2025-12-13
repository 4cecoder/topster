// RapidCloud video extractor with AES decryption
import CryptoJS from 'crypto-js';
import { fetchJson, fetchHtml } from '../http';
import type { VideoInfo, Subtitle } from '../../../core/types';

interface RapidCloudSourceResponse {
  sources?: string | Array<{ file: string; type?: string }>;
  tracks?: Array<{ file: string; kind: string; label: string; default?: boolean }>;
  encrypted?: boolean;
  intro?: { start: number; end: number };
  outro?: { start: number; end: number };
  server?: number;
}

const RAPIDCLOUD_API_BASE = 'https://rapid-cloud.co';
const FALLBACK_KEY = 'c1d17096f2ca11b7';
const GITHUB_KEY_URL = 'https://raw.githubusercontent.com/enimax-anime/key/e4/key.txt';

/**
 * Extract video ID from RapidCloud embed URL
 */
function extractVideoId(url: string): string {
  // URL format: https://rapid-cloud.co/embed-6/xxxxx or https://rabbitstream.net/v2/embed-4/xxxxx
  const parts = url.split('/');
  const idWithQuery = parts[parts.length - 1];
  if (!idWithQuery) {
    throw new Error('Invalid RapidCloud URL format');
  }
  const id = idWithQuery.split('?')[0];
  return id;
}

/**
 * Fetch decryption key from GitHub
 */
async function getDecryptionKey(): Promise<string> {
  try {
    const response = await fetchHtml(GITHUB_KEY_URL);

    // Extract key from HTML response
    // The key is in a table cell like: <td class="blob-code blob-code-inner js-file-line">KEY_HERE</td>
    const match = response.match(/blob-code blob-code-inner js-file-line">([^<]+)</);
    if (match && match[1]) {
      return match[1].trim();
    }

    console.warn('Failed to extract key from GitHub, using fallback');
    return FALLBACK_KEY;
  } catch (error) {
    console.error('Failed to fetch decryption key from GitHub:', error);
    return FALLBACK_KEY;
  }
}

/**
 * Parse decryption key to get index pairs
 */
function parseDecryptionKey(key: string): number[][] {
  const pairs: number[][] = [];

  // The key format is like: [[0,1],[2,3],[4,5]]
  // We need to parse it as JSON
  try {
    const parsed = JSON.parse(key);
    if (Array.isArray(parsed)) {
      return parsed;
    }
  } catch {
    // If parsing fails, return empty array
  }

  return pairs;
}

/**
 * Extract secret key from encrypted string using index pairs
 */
function extractSecretKey(encryptedData: string, indices: number[][]): { key: string; encrypted: string } {
  let key = '';
  let currentIndex = 0;
  const dataArray = encryptedData.split('');

  for (const [offset, length] of indices) {
    const start = offset + currentIndex;
    const end = start + length;

    for (let i = start; i < end; i++) {
      if (i < dataArray.length) {
        key += dataArray[i];
        dataArray[i] = '';
      }
    }
    currentIndex += length;
  }

  return {
    key,
    encrypted: dataArray.join(''),
  };
}

/**
 * Decrypt RapidCloud sources using AES
 */
function decryptSources(encryptedData: string, key: string): string {
  try {
    const decrypted = CryptoJS.AES.decrypt(encryptedData, key).toString(CryptoJS.enc.Utf8);
    return decrypted;
  } catch (error) {
    console.error('RapidCloud decryption failed:', error);
    throw new Error('Failed to decrypt video sources');
  }
}

/**
 * Extract referer from embed URL
 */
function getRefererFromEmbed(embedUrl: string): string {
  try {
    const url = new URL(embedUrl);
    return `${url.protocol}//${url.host}/`;
  } catch {
    return embedUrl;
  }
}

/**
 * Extract video sources from RapidCloud embed URL
 */
export async function extractRapidCloud(embedUrl: string, referer?: string): Promise<VideoInfo[]> {
  try {
    const videoId = extractVideoId(embedUrl);
    const embedReferer = getRefererFromEmbed(embedUrl);

    // Determine the hostname from the embed URL
    const url = new URL(embedUrl);
    const hostname = url.hostname;

    // Build API URL - rapid-cloud uses /embed-2/ajax/e-1/getSources
    const apiUrl = `https://${hostname}/embed-2/ajax/e-1/getSources?id=${videoId}`;

    console.log(`RapidCloud: Fetching sources for video ID ${videoId}`);

    // Fetch sources
    const response = await fetchJson<RapidCloudSourceResponse>(apiUrl, {
      headers: {
        'X-Requested-With': 'XMLHttpRequest',
        'Referer': embedUrl,
      }
    });

    let sources: Array<{ file: string; type?: string }> = [];

    // Check if sources are encrypted
    if (response.encrypted && typeof response.sources === 'string') {
      console.log('RapidCloud sources are encrypted, fetching decryption key...');

      // Get decryption key from GitHub
      const keyString = await getDecryptionKey();
      const keyIndices = parseDecryptionKey(keyString);

      if (keyIndices.length > 0) {
        // Extract the actual decryption key from the encrypted data
        const { key, encrypted } = extractSecretKey(response.sources, keyIndices);

        console.log('RapidCloud: Decrypting sources with extracted key');

        // Decrypt the sources
        const decrypted = decryptSources(encrypted, key);
        sources = JSON.parse(decrypted);

        console.log('RapidCloud sources decrypted successfully');
      } else {
        // Try direct decryption with the key as-is
        console.log('RapidCloud: Using key directly for decryption');
        const decrypted = decryptSources(response.sources, keyString || FALLBACK_KEY);
        sources = JSON.parse(decrypted);
      }
    } else if (Array.isArray(response.sources)) {
      // Sources are already decrypted
      sources = response.sources;
      console.log('RapidCloud sources are not encrypted');
    } else if (typeof response.sources === 'string') {
      // Try to parse as JSON
      try {
        sources = JSON.parse(response.sources);
      } catch {
        throw new Error('Failed to parse sources as JSON');
      }
    }

    // Filter for M3U8 sources
    const m3u8Sources = sources.filter(s => s.file && s.file.includes('.m3u8'));

    if (m3u8Sources.length === 0) {
      throw new Error('No M3U8 sources found');
    }

    // Parse subtitles
    const subtitles: Subtitle[] = (response.tracks || [])
      .filter(t => t.kind === 'captions' || t.kind === 'subtitles')
      .map(t => ({
        url: t.file,
        lang: t.label?.toLowerCase() || 'unknown',
        label: t.label || 'Unknown',
      }));

    console.log(`RapidCloud: Using referer ${embedReferer} for video URLs`);

    // Return video info for each source
    return m3u8Sources.map(s => ({
      url: s.file,
      subtitles,
      referer: embedReferer,
      quality: s.type || 'auto',
    }));
  } catch (error) {
    console.error('RapidCloud extraction failed:', error);
    throw error;
  }
}
