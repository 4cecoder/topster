// Auto-updater that checks for updates in the background

import { join } from 'path';
import { existsSync, mkdirSync } from 'fs';
import { homedir } from 'os';

const UPDATE_CHECK_INTERVAL = 24 * 60 * 60 * 1000; // 24 hours
const TOPSTER_DIR = join(homedir(), '.topster');
const LAST_CHECK_FILE = join(TOPSTER_DIR, 'last-update-check');

export interface UpdateResult {
  updated: boolean;
  currentVersion: string;
  message: string;
}

async function gitCommand(args: string[], cwd: string): Promise<string> {
  const proc = Bun.spawn(['git', ...args], {
    cwd,
    stdout: 'pipe',
    stderr: 'pipe',
  });

  const output = await new Response(proc.stdout).text();
  await proc.exited;
  return output.trim();
}

export async function checkForUpdates(silent: boolean = true): Promise<UpdateResult> {
  try {
    const repoDir = join(__dirname, '../..');

    // Check if we're in a git repo
    if (!existsSync(join(repoDir, '.git'))) {
      return {
        updated: false,
        currentVersion: 'unknown',
        message: 'Not a git repository',
      };
    }

    // Get current commit
    const localHash = await gitCommand(['rev-parse', 'HEAD'], repoDir);

    // Fetch latest from origin
    await gitCommand(['fetch', 'origin', 'main'], repoDir);

    // Get remote commit
    const remoteHash = await gitCommand(['rev-parse', 'origin/main'], repoDir);

    // Already up to date
    if (localHash === remoteHash) {
      return {
        updated: false,
        currentVersion: localHash.substring(0, 7),
        message: 'Already up to date',
      };
    }

    if (!silent) {
      console.log('Update available, pulling changes...');
    }

    // Stash any local changes
    const hasChanges = (await gitCommand(['status', '--porcelain'], repoDir)).length > 0;
    if (hasChanges) {
      await gitCommand(['stash', 'push', '-m', `auto-update-stash-${Date.now()}`], repoDir);
    }

    // Pull latest
    await gitCommand(['pull', 'origin', 'main'], repoDir);

    // Rebuild
    if (!silent) {
      console.log('Rebuilding...');
    }

    const installProc = Bun.spawn(['bun', 'install'], {
      cwd: repoDir,
      stdout: 'ignore',
      stderr: 'ignore',
    });
    await installProc.exited;

    const buildProc = Bun.spawn(['bun', 'run', 'build'], {
      cwd: repoDir,
      stdout: 'ignore',
      stderr: 'ignore',
    });
    await buildProc.exited;

    // Restore stash
    if (hasChanges) {
      await gitCommand(['stash', 'pop'], repoDir);
    }

    const newHash = await gitCommand(['rev-parse', '--short', 'HEAD'], repoDir);

    return {
      updated: true,
      currentVersion: newHash,
      message: `Updated to ${newHash}`,
    };
  } catch (error) {
    return {
      updated: false,
      currentVersion: 'unknown',
      message: error instanceof Error ? error.message : 'Update failed',
    };
  }
}

export async function shouldCheckForUpdates(): Promise<boolean> {
  try {
    if (!existsSync(LAST_CHECK_FILE)) {
      return true;
    }

    const content = await Bun.file(LAST_CHECK_FILE).text();
    const lastCheck = parseInt(content);
    return Date.now() - lastCheck > UPDATE_CHECK_INTERVAL;
  } catch {
    return true;
  }
}

export async function updateLastCheckTime(): Promise<void> {
  try {
    if (!existsSync(TOPSTER_DIR)) {
      mkdirSync(TOPSTER_DIR, { recursive: true });
    }
    await Bun.write(LAST_CHECK_FILE, Date.now().toString());
  } catch (err) {
    // Silently fail - not critical
  }
}

export async function autoUpdate(silent: boolean = true): Promise<UpdateResult | null> {
  if (!(await shouldCheckForUpdates())) {
    return null;
  }

  try {
    const result = await checkForUpdates(silent);
    await updateLastCheckTime();

    if (!silent && result.updated) {
      console.log(`✓ ${result.message}`);
    }

    return result;
  } catch (err) {
    // Silently fail - don't interrupt the user
    return null;
  }
}
