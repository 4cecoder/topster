// MegaCloud video extractor with local key extraction
import crypto from 'crypto';
import CryptoJS from 'crypto-js';
import { fetchJson, fetchHtml } from '../http';
import type { VideoInfo, Subtitle } from '../../../core/types';

interface MegaCloudSourceResponse {
  sources?: string | Array<{ file: string; type?: string }>;
  tracks?: Array<{ file: string; kind: string; label: string; default?: boolean }>;
  encrypted?: boolean;
  intro?: { start: number; end: number };
  outro?: { start: number; end: number };
}

const MEGACLOUD_API_BASE = 'https://megacloud.tv';
const PLAYER_SCRIPT_PATTERN = /https:\/\/megacloud\.tv\/js\/player\/a\/prod\/e\d+-player\.min\.js/;

/**
 * Extract video ID from MegaCloud embed URL
 */
function extractVideoId(url: string): string {
  // URL format: https://megacloud.tv/embed-2/e-1/xxxxx or similar
  const match = url.match(/\/e(?:-\d+)?\/([^?#/]+)/);
  if (!match) {
    throw new Error('Invalid MegaCloud URL format');
  }
  return match[1];
}

/**
 * Find a variable value in obfuscated JavaScript
 */
function matchingKey(script: string, value: string): string | null {
  const regex = new RegExp(`,${value}=((?:0x)?([0-9a-fA-F]+))`, 'g');
  const match = regex.exec(script);
  if (match && match[1]) {
    return match[1].replace('0x', '');
  }
  return null;
}

/**
 * Extract encryption variables from obfuscated player script
 */
function extractVariables(script: string): number[][] {
  // Pattern matches switch case statements with two variable assignments
  const regex = /case\s*0x[0-9a-f]+:(?![^;]*=partKey)\s*\w+\s*=\s*(\w+)\s*,\s*\w+\s*=\s*(\w+);/g;
  const matches = script.matchAll(regex);
  const variables: number[][] = [];

  for (const match of matches) {
    const firstVar = match[1];
    const secondVar = match[2];

    // Find hex values for these variables
    const firstValue = matchingKey(script, firstVar || '');
    const secondValue = matchingKey(script, secondVar || '');

    if (firstValue && secondValue) {
      variables.push([parseInt(firstValue, 16), parseInt(secondValue, 16)]);
    }
  }

  return variables;
}

/**
 * Extract secret key from encrypted string using index positions
 */
function getSecret(encryptedString: string, indices: number[][]): { secret: string; encryptedSource: string } {
  let secret = '';
  let currentIndex = 0;
  const sourceArray = encryptedString.split('');

  for (const [offset, length] of indices) {
    const start = offset + currentIndex;
    const end = start + length;

    for (let i = start; i < end; i++) {
      secret += sourceArray[i];
      sourceArray[i] = '';
    }
    currentIndex += length;
  }

  return {
    secret,
    encryptedSource: sourceArray.join(''),
  };
}

/**
 * Decrypt using AES-256-CBC with IV derivation (EVP_BytesToKey equivalent)
 */
function decrypt(encrypted: string, keyOrPassword: string, useMD5: boolean = true): string {
  if (!useMD5) {
    // Simple AES decryption with password
    return CryptoJS.AES.decrypt(encrypted, keyOrPassword).toString(CryptoJS.enc.Utf8);
  }

  // EVP_BytesToKey implementation using MD5
  const encryptedBytes = Buffer.from(encrypted, 'base64');

  // Check for Salted__ header
  if (encryptedBytes.toString('utf8', 0, 8) === 'Salted__') {
    const salt = encryptedBytes.subarray(8, 16);
    const ciphertext = encryptedBytes.subarray(16);

    // Derive key and IV using MD5 (OpenSSL's EVP_BytesToKey)
    const md5Hashes: Buffer[] = [];
    let hash = Buffer.alloc(0);

    for (let i = 0; i < 3; i++) {
      const data = Buffer.concat([hash, Buffer.from(keyOrPassword, 'utf8'), salt]);
      hash = crypto.createHash('md5').update(data).digest();
      md5Hashes.push(hash);
    }

    const key = Buffer.concat([md5Hashes[0]!, md5Hashes[1]!]);
    const iv = md5Hashes[2]!;

    // Decrypt using AES-256-CBC
    const decipher = crypto.createDecipheriv('aes-256-cbc', key, iv);
    const decrypted = Buffer.concat([decipher.update(ciphertext), decipher.final()]);

    return decrypted.toString('utf8');
  }

  // Fallback to CryptoJS
  return CryptoJS.AES.decrypt(encrypted, keyOrPassword).toString(CryptoJS.enc.Utf8);
}

/**
 * Extract decryption key from player script
 */
async function extractKeyFromScript(embedUrl: string): Promise<string> {
  try {
    // Fetch the embed page to find the player script URL
    const html = await fetchHtml(embedUrl);
    const scriptMatch = html.match(PLAYER_SCRIPT_PATTERN);

    if (!scriptMatch) {
      throw new Error('Could not find player script URL');
    }

    const scriptUrl = scriptMatch[0];

    // Add timestamp to bypass cache
    const scriptUrlWithTimestamp = `${scriptUrl}?t=${Date.now()}`;

    // Fetch the player script
    const script = await fetchHtml(scriptUrlWithTimestamp);

    // Extract variables using regex patterns
    const variables = extractVariables(script);

    if (variables.length === 0) {
      throw new Error('Could not extract encryption variables from player script');
    }

    return JSON.stringify(variables);
  } catch (error) {
    console.error('Failed to extract key from script:', error);
    throw error;
  }
}

/**
 * Extract video sources from MegaCloud embed URL with local key extraction
 */
export async function extractMegaCloud(embedUrl: string, referer?: string): Promise<VideoInfo[]> {
  try {
    const videoId = extractVideoId(embedUrl);

    // Fetch sources first to check if encryption is needed
    const apiUrl = `${MEGACLOUD_API_BASE}/embed-2/ajax/e-1/getSources?id=${videoId}`;

    const response = await fetchJson<MegaCloudSourceResponse>(apiUrl, {
      headers: {
        'X-Requested-With': 'XMLHttpRequest',
        'Referer': embedUrl,
      }
    });

    let sources: Array<{ file: string; type?: string }> = [];

    // Check if sources are encrypted
    if (response.encrypted && typeof response.sources === 'string') {
      console.log('MegaCloud sources are encrypted, extracting key from player script...');

      // Extract decryption variables from player script
      const variablesJson = await extractKeyFromScript(embedUrl);
      const variables: number[][] = JSON.parse(variablesJson);

      // Extract secret key and encrypted source
      const { secret, encryptedSource } = getSecret(response.sources, variables);

      // Decrypt using the extracted secret
      const decrypted = decrypt(encryptedSource, secret);
      sources = JSON.parse(decrypted);

      console.log('MegaCloud sources decrypted successfully');
    } else if (Array.isArray(response.sources)) {
      // Sources are already decrypted
      sources = response.sources;
      console.log('MegaCloud sources are not encrypted');
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

    // Return video info for each source
    return m3u8Sources.map(s => ({
      url: s.file,
      subtitles,
      referer: referer || MEGACLOUD_API_BASE,
      quality: s.type || 'auto',
    }));
  } catch (error) {
    console.error('MegaCloud extraction failed:', error);
    throw error;
  }
}
