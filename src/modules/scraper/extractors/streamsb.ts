// StreamSB video extractor
import { fetchHtml } from '../http';
import type { VideoInfo, Subtitle } from '../../../core/types';

const STREAMSB_HOSTS = [
  'https://streamsss.net/sources50',
  'https://watchsb.com/sources50',
  'https://sbplay2.com/sources48',
];

/**
 * Generate StreamSB payload with hex-encoded ID
 */
function generatePayload(hex: string): string {
  return `566d337678566f743674494a7c7c${hex}7c7c346b6767586d6934774855537c7c73747265616d7362/6565417268755339773461447c7c346133383438333436313335376136323337373433383634376337633465366534393338373136643732373736343735373237613763376334363733353737303533366236333463353333363534366137633763373337343732363536313664373336327c7c6b586c3163614468645a47617c7c73747265616d7362`;
}

/**
 * Convert string to hex
 */
function stringToHex(str: string): string {
  const encoder = new TextEncoder();
  const bytes = encoder.encode(str);
  return Array.from(bytes)
    .map(b => b.toString(16).padStart(2, '0'))
    .join('');
}

/**
 * Extract video ID from StreamSB URL
 */
function extractVideoId(url: string): string {
  // URL format: https://watchsb.com/e/xxxxx.html or https://streamsss.net/e/xxxxx
  const parts = url.split('/e/');
  if (parts.length < 2) {
    throw new Error('Invalid StreamSB URL format');
  }

  let id = parts[1];
  if (!id) {
    throw new Error('Invalid StreamSB URL format');
  }

  // Remove .html extension if present
  id = id.replace('.html', '').split('?')[0]!;

  return id;
}

/**
 * Parse M3U8 playlist to extract quality variants
 */
function parseM3U8(content: string, baseUrl: string): Array<{ file: string; quality?: string }> {
  const sources: Array<{ file: string; quality?: string }> = [];

  // Split by EXT-X-STREAM-INF
  const lines = content.split('\n');

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];

    if (line?.startsWith('#EXT-X-STREAM-INF:')) {
      // Extract resolution from RESOLUTION parameter
      const resolutionMatch = line.match(/RESOLUTION=(\d+)x(\d+)/);
      const quality = resolutionMatch ? `${resolutionMatch[2]}p` : undefined;

      // Next line should be the URL
      const urlLine = lines[i + 1];
      if (urlLine && urlLine.trim() && !urlLine.startsWith('#')) {
        let fileUrl = urlLine.trim();

        // Handle relative URLs
        if (!fileUrl.startsWith('http')) {
          const base = new URL(baseUrl);
          fileUrl = `${base.protocol}//${base.host}${fileUrl.startsWith('/') ? '' : '/'}${fileUrl}`;
        }

        sources.push({
          file: fileUrl,
          quality,
        });
      }
    }
  }

  // If no variants found, the whole content might be the master URL
  if (sources.length === 0 && content.includes('.m3u8')) {
    sources.push({
      file: baseUrl,
      quality: 'auto',
    });
  }

  return sources;
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
 * Extract video sources from StreamSB embed URL
 */
export async function extractStreamSB(embedUrl: string, referer?: string): Promise<VideoInfo[]> {
  try {
    const videoId = extractVideoId(embedUrl);
    const hexId = stringToHex(videoId);
    const payload = generatePayload(hexId);
    const embedReferer = getRefererFromEmbed(embedUrl);

    console.log(`StreamSB: Extracting video ID ${videoId}`);

    // Try each host until one works
    let lastError: Error | null = null;

    for (const host of STREAMSB_HOSTS) {
      try {
        const apiUrl = `${host}/${payload}`;

        // Fetch the sources
        const response = await fetchHtml(apiUrl, {
          headers: {
            'watchsb': 'sbstream',
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
            'Referer': embedUrl,
          }
        });

        // The response should be JSON with stream_data
        let data: any;
        try {
          data = JSON.parse(response);
        } catch {
          // If not JSON, try to find M3U8 URL in the response
          const m3u8Match = response.match(/(https?:\/\/[^\s"']+\.m3u8[^\s"']*)/);
          if (m3u8Match) {
            console.log(`StreamSB: Found M3U8 URL directly`);
            return [{
              url: m3u8Match[1]!,
              subtitles: [],
              referer: embedReferer,
              quality: 'auto',
            }];
          }
          throw new Error('Failed to parse StreamSB response');
        }

        // Check for stream_data in response
        if (data.stream_data) {
          const streamUrl = data.stream_data.file || data.stream_data;

          if (typeof streamUrl === 'string' && streamUrl.includes('.m3u8')) {
            console.log(`StreamSB: Using referer ${embedReferer} for video URL`);

            // Fetch the M3U8 playlist to get quality variants
            try {
              const playlistContent = await fetchHtml(streamUrl);
              const sources = parseM3U8(playlistContent, streamUrl);

              if (sources.length > 0) {
                return sources.map(s => ({
                  url: s.file,
                  subtitles: [],
                  referer: embedReferer,
                  quality: s.quality || 'auto',
                }));
              }
            } catch (playlistError) {
              console.warn('Failed to fetch M3U8 playlist, using master URL:', playlistError);
            }

            // Fallback to master URL
            return [{
              url: streamUrl,
              subtitles: [],
              referer: embedReferer,
              quality: 'auto',
            }];
          }
        }

        // If we found data but no stream_data, check for direct file property
        if (data.file && typeof data.file === 'string') {
          return [{
            url: data.file,
            subtitles: [],
            referer: embedReferer,
            quality: 'auto',
          }];
        }

        // If we got here, this host didn't work, try next one
        throw new Error('No valid stream data found');

      } catch (error) {
        console.warn(`StreamSB host ${host} failed:`, error);
        lastError = error as Error;
        continue;
      }
    }

    // All hosts failed
    throw new Error(`All StreamSB hosts failed. Last error: ${lastError?.message}`);

  } catch (error) {
    console.error('StreamSB extraction failed:', error);
    throw error;
  }
}
