// Discord Rich Presence module

import { Socket } from 'net';
import { existsSync } from 'fs';
import type { MediaItem, Episode } from '../../core/types';
import { getConfig } from '../../core/config';

const DISCORD_CLIENT_ID = '1239340948048187472';

interface DiscordActivity {
  state?: string;
  details?: string;
  timestamps?: {
    start?: number;
    end?: number;
  };
  assets?: {
    large_image?: string;
    large_text?: string;
    small_image?: string;
    small_text?: string;
  };
  buttons?: Array<{ label: string; url: string }>;
}

interface DiscordPayload {
  cmd: string;
  args?: Record<string, unknown>;
  nonce?: string;
}

export class DiscordRPC {
  private socket: Socket | null = null;
  private connected = false;

  async connect(): Promise<boolean> {
    if (!getConfig().get('discordPresence')) {
      return false;
    }

    const socketPaths = this.getSocketPaths();

    for (const socketPath of socketPaths) {
      if (!existsSync(socketPath)) continue;

      try {
        await this.connectToSocket(socketPath);
        await this.handshake();
        this.connected = true;
        return true;
      } catch {
        this.disconnect();
        continue;
      }
    }

    return false;
  }

  private getSocketPaths(): string[] {
    const uid = process.getuid?.() || 1000;
    return [
      `/run/user/${uid}/discord-ipc-0`,
      '/tmp/discord-ipc-0',
      `/run/user/${uid}/app/com.discordapp.Discord/discord-ipc-0`,
    ];
  }

  private connectToSocket(path: string): Promise<void> {
    return new Promise((resolve, reject) => {
      this.socket = new Socket();

      this.socket.on('connect', () => resolve());
      this.socket.on('error', (err) => reject(err));

      this.socket.connect(path);
    });
  }

  private async handshake(): Promise<void> {
    const payload = JSON.stringify({
      v: 1,
      client_id: DISCORD_CLIENT_ID,
    });

    await this.send(0, payload);
    await this.receive();
  }

  private send(opcode: number, data: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (!this.socket) {
        reject(new Error('Not connected'));
        return;
      }

      const payload = Buffer.from(data, 'utf-8');
      const header = Buffer.alloc(8);
      header.writeUInt32LE(opcode, 0);
      header.writeUInt32LE(payload.length, 4);

      const packet = Buffer.concat([header, payload]);

      this.socket.write(packet, (err) => {
        if (err) reject(err);
        else resolve();
      });
    });
  }

  private receive(): Promise<string> {
    return new Promise((resolve, reject) => {
      if (!this.socket) {
        reject(new Error('Not connected'));
        return;
      }

      const handler = (data: Buffer) => {
        this.socket?.off('data', handler);
        const payload = data.subarray(8).toString('utf-8');
        resolve(payload);
      };

      this.socket.on('data', handler);
      this.socket.once('error', reject);
    });
  }

  async setActivity(activity: DiscordActivity): Promise<void> {
    if (!this.connected || !this.socket) return;

    const payload: DiscordPayload = {
      cmd: 'SET_ACTIVITY',
      args: {
        pid: process.pid,
        activity,
      },
      nonce: Math.random().toString(36).substring(2),
    };

    await this.send(1, JSON.stringify(payload));
  }

  async updatePresence(
    media: MediaItem,
    episode?: Episode,
    startTime?: number
  ): Promise<void> {
    let details = media.title;
    let state = media.type === 'movie' ? 'Watching a movie' : 'Watching TV';

    if (episode) {
      state = `S${episode.number.toString().padStart(2, '0')} - ${episode.title || `Episode ${episode.number}`}`;
    }

    const activity: DiscordActivity = {
      details,
      state,
      timestamps: startTime ? { start: startTime } : undefined,
      assets: {
        large_image: 'topster_logo',
        large_text: 'Topster',
      },
    };

    await this.setActivity(activity);
  }

  async clearPresence(): Promise<void> {
    if (!this.connected || !this.socket) return;

    const payload: DiscordPayload = {
      cmd: 'SET_ACTIVITY',
      args: {
        pid: process.pid,
        activity: null,
      },
      nonce: Math.random().toString(36).substring(2),
    };

    await this.send(1, JSON.stringify(payload));
  }

  disconnect(): void {
    if (this.socket) {
      this.socket.destroy();
      this.socket = null;
    }
    this.connected = false;
  }

  isConnected(): boolean {
    return this.connected;
  }
}

// Singleton instance
let discordInstance: DiscordRPC | null = null;

export function getDiscord(): DiscordRPC {
  if (!discordInstance) {
    discordInstance = new DiscordRPC();
  }
  return discordInstance;
}
