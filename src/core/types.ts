// Core types for Topster

export type MediaType = 'movie' | 'tv';

export interface MediaItem {
  id: string;
  title: string;
  type: MediaType;
  year?: string;
  rating?: string;
  duration?: string;
  quality?: string;
  image?: string;
  url: string;
}

export interface Season {
  id: string;
  number: number;
  title: string;
}

export interface Episode {
  id: string;
  number: number;
  title: string;
  url?: string;
}

export interface Subtitle {
  url: string;
  lang: string;
  label: string;
}

export interface VideoInfo {
  url: string;
  subtitles: Subtitle[];
  referer?: string;
  quality?: string;
}

export interface VideoSource {
  provider: string;
  sources: VideoInfo[];
}

export interface HistoryEntry {
  id: string;
  title: string;
  type: MediaType;
  url: string;
  episodeId?: string;
  episodeTitle?: string;
  seasonNumber?: number;
  episodeNumber?: number;
  position: string; // HH:MM:SS format
  duration: string;
  percentWatched: number;
  lastWatched: string; // ISO timestamp
  completed: boolean;
}

export interface SearchResult {
  items: MediaItem[];
  totalPages?: number;
  currentPage?: number;
}

// HLS Types
export interface HLSVariant {
  url: string;
  bandwidth: number;
  resolution?: string;
  codecs?: string;
  name?: string;
}

export interface HLSSegment {
  url: string;
  duration: number;
  title?: string;
  byteRange?: { start: number; end: number };
  key?: HLSEncryptionKey;
  discontinuity?: boolean;
}

export interface HLSEncryptionKey {
  method: 'AES-128' | 'SAMPLE-AES' | 'NONE';
  uri?: string;
  iv?: string;
}

export interface HLSMediaPlaylist {
  version?: number;
  targetDuration: number;
  segments: HLSSegment[];
  endList: boolean;
  playlistType?: 'VOD' | 'EVENT';
}

export interface HLSMasterPlaylist {
  version?: number;
  variants: HLSVariant[];
  audioRenditions?: MediaRendition[];
  subtitleRenditions?: MediaRendition[];
}

export interface MediaRendition {
  type: 'AUDIO' | 'SUBTITLES' | 'VIDEO';
  groupId: string;
  name: string;
  language?: string;
  uri?: string;
  default?: boolean;
  autoselect?: boolean;
}

// Player types
export type PlayerType = 'mpv' | 'vlc' | 'iina' | 'internal';

export interface PlayerOptions {
  startTime?: number;
  subtitleFile?: string;
  quality?: string;
  fullscreen?: boolean;
}

// Provider types
export interface Provider {
  name: string;
  baseUrl: string;
  search: (query: string, page?: number) => Promise<SearchResult>;
  getTrending: (page?: number) => Promise<SearchResult>;
  getRecent: (type: MediaType, page?: number) => Promise<SearchResult>;
  getSeasons: (mediaId: string) => Promise<Season[]>;
  getEpisodes: (seasonId: string) => Promise<Episode[]>;
  getVideoSources: (id: string, isEpisode?: boolean) => Promise<VideoSource[]>;
}
