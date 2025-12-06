// Custom error types for Topster

export class TopsterError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'TopsterError';
  }
}

export class NetworkError extends TopsterError {
  constructor(
    message: string,
    public statusCode?: number,
    public url?: string
  ) {
    super(message);
    this.name = 'NetworkError';
  }
}

export class ScrapingError extends TopsterError {
  constructor(
    message: string,
    public selector?: string
  ) {
    super(message);
    this.name = 'ScrapingError';
  }
}

export class ConfigError extends TopsterError {
  constructor(
    message: string,
    public configPath?: string
  ) {
    super(message);
    this.name = 'ConfigError';
  }
}

export class PlayerError extends TopsterError {
  constructor(
    message: string,
    public player?: string
  ) {
    super(message);
    this.name = 'PlayerError';
  }
}

export class NoResultsError extends TopsterError {
  constructor(query?: string) {
    super(query ? `No results found for: ${query}` : 'No results found');
    this.name = 'NoResultsError';
  }
}

export class DecryptionError extends TopsterError {
  constructor(message: string) {
    super(message);
    this.name = 'DecryptionError';
  }
}

export class HLSError extends TopsterError {
  constructor(message: string) {
    super(message);
    this.name = 'HLSError';
  }
}

export class HistoryError extends TopsterError {
  constructor(message: string) {
    super(message);
    this.name = 'HistoryError';
  }
}

export class DownloadError extends TopsterError {
  constructor(
    message: string,
    public filePath?: string
  ) {
    super(message);
    this.name = 'DownloadError';
  }
}

export class MCPError extends TopsterError {
  constructor(
    message: string,
    public code?: string
  ) {
    super(message);
    this.name = 'MCPError';
  }
}

// Error helper
export function isTopsterError(error: unknown): error is TopsterError {
  return error instanceof TopsterError;
}

export function formatError(error: unknown): string {
  if (isTopsterError(error)) {
    return `[${error.name}] ${error.message}`;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return String(error);
}
