// VidCloud video extractor
import { fetchJson } from '../http';
import type { VideoInfo, Subtitle } from '../../../core/types';

interface DecryptedResponse {
  sources?: Array<{ file: string; type?: string }>;
  tracks?: Array<{ file: string; kind: string; label: string }>;
}

const DECRYPT_API_URL = 'https://dec.eatmynerds.live';
const MAX_RETRIES = 3;

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
 * Extract video sources from VidCloud/UpCloud embed URL
 * Uses the decryption API to get video sources
 */
export async function extractVidCloud(embedUrl: string, referer?: string): Promise<VideoInfo[]> {
  // Extract the correct referer from the embed URL
  const embedReferer = getRefererFromEmbed(embedUrl);
  let lastError: unknown;

  // Retry up to MAX_RETRIES times since the API can be flaky
  for (let attempt = 1; attempt <= MAX_RETRIES; attempt++) {
    try {
      const decryptUrl = `${DECRYPT_API_URL}/?url=${encodeURIComponent(embedUrl)}`;
      const response = await fetchJson<DecryptedResponse>(decryptUrl);

      if (response.sources && response.sources.length > 0) {
        // Filter for M3U8 sources
        const m3u8Sources = response.sources.filter(s => s.file && s.file.includes('.m3u8'));

        if (m3u8Sources.length > 0) {
          // Parse subtitles
          const subtitles: Subtitle[] = (response.tracks || [])
            .filter(t => t.kind === 'captions' || t.kind === 'subtitles')
            .map(t => ({
              url: t.file,
              lang: t.label?.toLowerCase() || 'unknown',
              label: t.label || 'Unknown',
            }));

          // Return video info for each source
          console.log(`VidCloud: Using referer ${embedReferer} for video URLs`);
          return m3u8Sources.map(s => ({
            url: s.file,
            subtitles,
            referer: embedReferer,
            quality: s.type || 'auto',
          }));
        }
      }

      // API returned empty sources, retry after a short delay
      if (attempt < MAX_RETRIES) {
        await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
      }
    } catch (error) {
      lastError = error;
      if (attempt < MAX_RETRIES) {
        await new Promise(resolve => setTimeout(resolve, 1000 * attempt));
      }
    }
  }

  // All retries failed
  console.error('VidCloud extraction failed after', MAX_RETRIES, 'attempts:', lastError || 'empty response');
  throw new Error(`Failed to extract video from VidCloud: ${embedUrl}`);
}
