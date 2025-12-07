// Configuration management for Topster

import { homedir, platform } from 'os';
import { join } from 'path';
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'fs';
import type { PlayerType, MediaType } from './types';
import { ConfigError } from './errors';

export interface TopsterConfig {
  // Provider settings
  baseUrl: string;
  provider: 'Vidcloud' | 'UpCloud';

  // Player settings
  player: PlayerType;
  quality: '480' | '720' | '1080' | 'auto';

  // Subtitle settings
  subsLanguage: string;
  subtitlesEnabled: boolean;

  // Directory settings
  downloadDir: string;

  // Feature flags
  historyEnabled: boolean;
  imagePreview: boolean;
  discordPresence: boolean;
  debug: boolean;

  // UI settings
  useExternalMenu: boolean;
  previewWindowSize: string;

  // MCP settings
  mcpPort: number;
  mcpHost: string;
}

// Detect the best default player based on platform
function getDefaultPlayer(): PlayerType {
  const p = platform();
  if (p === 'darwin') {
    return 'iina'; // macOS: use IINA
  }
  return 'mpv'; // Linux/Windows: use mpv
}

const DEFAULT_CONFIG: TopsterConfig = {
  baseUrl: 'https://flixhq.to',
  provider: 'Vidcloud',
  player: getDefaultPlayer(),
  quality: '1080',
  subsLanguage: 'english',
  subtitlesEnabled: true,
  downloadDir: join(homedir(), 'Downloads'),
  historyEnabled: true,
  imagePreview: false,
  discordPresence: false,
  debug: false,
  useExternalMenu: false,
  previewWindowSize: 'right:60%:wrap',
  mcpPort: 3847,
  mcpHost: 'localhost',
};

export class ConfigManager {
  private config: TopsterConfig;
  private configDir: string;
  private configPath: string;
  private dataDir: string;

  constructor() {
    this.configDir = join(homedir(), '.config', 'topster');
    this.configPath = join(this.configDir, 'config.json');
    this.dataDir = join(homedir(), '.local', 'share', 'topster');
    this.config = { ...DEFAULT_CONFIG };
    this.ensureDirectories();
    this.load();
  }

  private ensureDirectories(): void {
    try {
      if (!existsSync(this.configDir)) {
        mkdirSync(this.configDir, { recursive: true });
      }
      if (!existsSync(this.dataDir)) {
        mkdirSync(this.dataDir, { recursive: true });
      }
    } catch (error) {
      throw new ConfigError(`Failed to create config directories: ${error}`);
    }
  }

  private load(): void {
    try {
      if (existsSync(this.configPath)) {
        const data = readFileSync(this.configPath, 'utf-8');
        const loaded = JSON.parse(data);
        this.config = { ...DEFAULT_CONFIG, ...loaded };
      } else {
        this.save();
      }
    } catch (error) {
      throw new ConfigError(`Failed to load config: ${error}`, this.configPath);
    }
  }

  save(): void {
    try {
      writeFileSync(this.configPath, JSON.stringify(this.config, null, 2));
    } catch (error) {
      throw new ConfigError(`Failed to save config: ${error}`, this.configPath);
    }
  }

  get<K extends keyof TopsterConfig>(key: K): TopsterConfig[K] {
    return this.config[key];
  }

  set<K extends keyof TopsterConfig>(key: K, value: TopsterConfig[K]): void {
    this.config[key] = value;
    this.save();
  }

  getAll(): TopsterConfig {
    return { ...this.config };
  }

  update(partial: Partial<TopsterConfig>): void {
    this.config = { ...this.config, ...partial };
    this.save();
  }

  reset(): void {
    this.config = { ...DEFAULT_CONFIG };
    this.save();
  }

  getConfigPath(): string {
    return this.configPath;
  }

  getDataDir(): string {
    return this.dataDir;
  }

  getHistoryPath(): string {
    return join(this.dataDir, 'history.json');
  }
}

// Singleton instance
let configInstance: ConfigManager | null = null;

export function getConfig(): ConfigManager {
  if (!configInstance) {
    configInstance = new ConfigManager();
  }
  return configInstance;
}

export function resetConfigInstance(): void {
  configInstance = null;
}
