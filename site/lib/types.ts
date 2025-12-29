/**
 * Shared types between CLI and web site
 */

export interface MediaItem {
  id: string;
  title: string;
  url: string;
  image: string;
  releaseDate?: string;
  seasons?: number;
  type?: 'Movie' | 'TV Series';
}

export interface Episode {
  id: string;
  title: string;
  number: number;
  season: number;
  url: string;
}

export interface Season {
  season: number;
  episodes: Episode[];
}

export interface VideoSource {
  url: string;
  quality: string;
  isM3U8: boolean;
}

export interface Subtitle {
  url: string;
  lang: string;
}

export interface StreamingData {
  sources: VideoSource[];
  subtitles: Subtitle[];
  headers?: Record<string, string>;
}

export interface HistoryItem {
  id: number;
  mediaId: string;
  title: string;
  type: 'movie' | 'tv';
  season?: number;
  episode?: number;
  position: number;
  duration: number;
  lastWatched: string;
  image?: string;
}

export interface IMDbData {
  title: string;
  year: string;
  rated: string;
  released: string;
  runtime: string;
  genre: string;
  director: string;
  writer: string;
  actors: string;
  plot: string;
  language: string;
  country: string;
  awards: string;
  poster: string;
  ratings: Array<{
    source: string;
    value: string;
  }>;
  imdbRating: string;
  imdbVotes: string;
  imdbID: string;
  type: string;
  totalSeasons?: string;
}

export interface TopsterConfig {
  baseUrl: string;
  provider: 'Vidcloud' | 'UpCloud';
  player: 'mpv' | 'vlc' | 'iina' | 'internal';
  quality: '480' | '720' | '1080' | 'auto';
  subsLanguage: string;
  subtitlesEnabled: boolean;
  downloadDir: string;
  historyEnabled: boolean;
  imagePreview: boolean;
  discordPresence: boolean;
  debug: boolean;
  useExternalMenu: boolean;
  previewWindowSize: string;
  mcpPort: number;
  mcpHost: string;
  omdbApiKey?: string;
}

export interface TVDevice {
  id: string;
  name: string;
  ip: string;
  port: number;
  online: boolean;
  lastSeen?: Date;
}
