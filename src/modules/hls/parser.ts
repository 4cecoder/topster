// HLS M3U8 playlist parser

import type {
  HLSMasterPlaylist,
  HLSMediaPlaylist,
  HLSVariant,
  HLSSegment,
  HLSEncryptionKey,
  MediaRendition,
} from '../../core/types';
import { HLSError } from '../../core/errors';

export function isMasterPlaylist(content: string): boolean {
  return content.includes('#EXT-X-STREAM-INF');
}

export function parseMasterPlaylist(content: string, baseUrl: string): HLSMasterPlaylist {
  const lines = content.split('\n').map(l => l.trim()).filter(l => l);

  if (!lines[0]?.startsWith('#EXTM3U')) {
    throw new HLSError('Invalid M3U8 playlist: missing #EXTM3U header');
  }

  const variants: HLSVariant[] = [];
  const audioRenditions: MediaRendition[] = [];
  const subtitleRenditions: MediaRendition[] = [];
  let version: number | undefined;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]!;

    if (line.startsWith('#EXT-X-VERSION:')) {
      version = parseInt(line.split(':')[1] ?? '0');
    }

    if (line.startsWith('#EXT-X-MEDIA:')) {
      const rendition = parseMediaRendition(line);
      if (rendition) {
        if (rendition.type === 'AUDIO') {
          audioRenditions.push(rendition);
        } else if (rendition.type === 'SUBTITLES') {
          subtitleRenditions.push(rendition);
        }
      }
    }

    if (line.startsWith('#EXT-X-STREAM-INF:')) {
      const nextLine = lines[i + 1];
      if (nextLine && !nextLine.startsWith('#')) {
        const variant = parseStreamInf(line, resolveUrl(nextLine, baseUrl));
        if (variant) variants.push(variant);
        i++;
      }
    }
  }

  return { version, variants, audioRenditions, subtitleRenditions };
}

export function parseMediaPlaylist(content: string, baseUrl: string): HLSMediaPlaylist {
  const lines = content.split('\n').map(l => l.trim()).filter(l => l);

  if (!lines[0]?.startsWith('#EXTM3U')) {
    throw new HLSError('Invalid M3U8 playlist: missing #EXTM3U header');
  }

  const segments: HLSSegment[] = [];
  let targetDuration = 0;
  let version: number | undefined;
  let endList = false;
  let playlistType: 'VOD' | 'EVENT' | undefined;
  let currentKey: HLSEncryptionKey | undefined;
  let currentDuration = 0;
  let currentTitle: string | undefined;
  let discontinuity = false;

  for (let i = 0; i < lines.length; i++) {
    const line = lines[i]!;

    if (line.startsWith('#EXT-X-VERSION:')) {
      version = parseInt(line.split(':')[1] ?? '0');
    }

    if (line.startsWith('#EXT-X-TARGETDURATION:')) {
      targetDuration = parseInt(line.split(':')[1] ?? '0');
    }

    if (line.startsWith('#EXT-X-PLAYLIST-TYPE:')) {
      const type = line.split(':')[1];
      if (type === 'VOD' || type === 'EVENT') {
        playlistType = type;
      }
    }

    if (line === '#EXT-X-ENDLIST') {
      endList = true;
    }

    if (line === '#EXT-X-DISCONTINUITY') {
      discontinuity = true;
    }

    if (line.startsWith('#EXT-X-KEY:')) {
      const key = parseKey(line, baseUrl);
      if (key) currentKey = key;
    }

    if (line.startsWith('#EXTINF:')) {
      const match = line.match(/#EXTINF:([\d.]+)(?:,(.*))?/);
      if (match) {
        currentDuration = parseFloat(match[1] ?? '0');
        currentTitle = match[2]?.trim();
      }
    }

    // Segment URL (non-comment line after EXTINF)
    if (!line.startsWith('#') && currentDuration > 0) {
      segments.push({
        url: resolveUrl(line, baseUrl),
        duration: currentDuration,
        title: currentTitle,
        key: currentKey,
        discontinuity,
      });
      currentDuration = 0;
      currentTitle = undefined;
      discontinuity = false;
    }
  }

  return { version, targetDuration, segments, endList, playlistType };
}

function parseStreamInf(line: string, url: string): HLSVariant | null {
  const attrs = parseAttributes(line.replace('#EXT-X-STREAM-INF:', ''));

  const bandwidth = parseInt(attrs['BANDWIDTH'] || '0');
  if (!bandwidth) return null;

  return {
    url,
    bandwidth,
    resolution: attrs['RESOLUTION'],
    codecs: attrs['CODECS'],
    name: attrs['NAME'],
  };
}

function parseMediaRendition(line: string): MediaRendition | null {
  const attrs = parseAttributes(line.replace('#EXT-X-MEDIA:', ''));

  const type = attrs['TYPE'] as 'AUDIO' | 'SUBTITLES' | 'VIDEO';
  const groupId = attrs['GROUP-ID'];
  const name = attrs['NAME'];

  if (!type || !groupId || !name) return null;

  return {
    type,
    groupId,
    name,
    language: attrs['LANGUAGE'],
    uri: attrs['URI'],
    default: attrs['DEFAULT'] === 'YES',
    autoselect: attrs['AUTOSELECT'] === 'YES',
  };
}

function parseKey(line: string, baseUrl: string): HLSEncryptionKey | null {
  const attrs = parseAttributes(line.replace('#EXT-X-KEY:', ''));

  const method = attrs['METHOD'];
  if (!method || method === 'NONE') {
    return { method: 'NONE' };
  }

  return {
    method: method as 'AES-128' | 'SAMPLE-AES',
    uri: attrs['URI'] ? resolveUrl(attrs['URI'], baseUrl) : undefined,
    iv: attrs['IV'],
  };
}

function parseAttributes(str: string): Record<string, string> {
  const attrs: Record<string, string> = {};
  const regex = /([A-Z-]+)=(?:"([^"]+)"|([^,]+))/g;
  let match;

  while ((match = regex.exec(str)) !== null) {
    const key = match[1];
    const value = match[2] || match[3];
    if (key && value) {
      attrs[key] = value;
    }
  }

  return attrs;
}

function resolveUrl(url: string, baseUrl: string): string {
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url;
  }

  if (url.startsWith('/')) {
    const base = new URL(baseUrl);
    return `${base.protocol}//${base.host}${url}`;
  }

  // Relative URL
  const base = baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1);
  return base + url;
}

export function selectBestVariant(
  variants: HLSVariant[],
  preferredQuality?: string
): HLSVariant | null {
  if (variants.length === 0) return null;

  // Sort by bandwidth descending
  const sorted = [...variants].sort((a, b) => b.bandwidth - a.bandwidth);

  if (!preferredQuality || preferredQuality === 'auto') {
    return sorted[0] ?? null;
  }

  const targetHeight = parseInt(preferredQuality);
  if (isNaN(targetHeight)) return sorted[0] ?? null;

  // Find the variant closest to preferred quality
  for (const variant of sorted) {
    if (variant.resolution) {
      const height = parseInt(variant.resolution.split('x')[1] || '0');
      if (height <= targetHeight) {
        return variant;
      }
    }
  }

  // Return lowest quality if none match
  return sorted[sorted.length - 1] ?? null;
}
