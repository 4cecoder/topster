'use client';

import { useState, useEffect } from 'react';
import { getConfig, updateConfig } from '@/lib/actions/config';
import type { TopsterConfig } from '@/lib/types';
import { ArrowLeft, Save, Settings as SettingsIcon } from 'lucide-react';
import Link from 'next/link';

export default function SettingsPage() {
  const [config, setConfig] = useState<Partial<TopsterConfig> | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  useEffect(() => {
    loadConfig();
  }, []);

  const loadConfig = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await getConfig();
      setConfig(data);
    } catch (err) {
      setError('Failed to load configuration');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleSave = async () => {
    if (!config) return;

    try {
      setIsSaving(true);
      setError(null);
      setSuccess(false);
      await updateConfig(config);
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (err) {
      setError('Failed to save configuration');
      console.error(err);
    } finally {
      setIsSaving(false);
    }
  };

  const updateField = <K extends keyof TopsterConfig>(
    field: K,
    value: TopsterConfig[K]
  ) => {
    if (!config) return;
    setConfig({ ...config, [field]: value });
  };

  if (isLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-black">
        <div className="h-12 w-12 animate-spin rounded-full border-4 border-zinc-700 border-t-white" />
      </div>
    );
  }

  if (!config) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-black text-white">
        <div className="text-center">
          <p className="text-lg text-zinc-400">Failed to load configuration</p>
          <Link href="/" className="mt-4 inline-block text-sm text-zinc-500 hover:text-white">
            Back to Browse
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-black text-white">
      {/* Header */}
      <header className="border-b border-zinc-800 bg-zinc-900/50 backdrop-blur-sm">
        <div className="container mx-auto flex items-center justify-between px-6 py-4">
          <Link
            href="/"
            className="inline-flex items-center gap-2 text-sm text-zinc-400 transition-colors hover:text-white"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Browse
          </Link>

          <button
            onClick={handleSave}
            disabled={isSaving}
            className="flex items-center gap-2 rounded-full bg-white px-4 py-2 text-sm font-medium text-black transition-colors hover:bg-zinc-200 disabled:opacity-50"
          >
            <Save className="h-4 w-4" />
            {isSaving ? 'Saving...' : 'Save Changes'}
          </button>
        </div>
      </header>

      <main className="container mx-auto max-w-2xl px-6 py-8">
        <div className="mb-8 flex items-center gap-3">
          <SettingsIcon className="h-8 w-8" />
          <h1 className="text-3xl font-bold">Settings</h1>
        </div>

        {/* Success Message */}
        {success && (
          <div className="mb-6 rounded-lg border border-green-500/20 bg-green-500/10 p-4 text-green-400">
            <p className="text-sm">Settings saved successfully!</p>
          </div>
        )}

        {/* Error Message */}
        {error && (
          <div className="mb-6 rounded-lg border border-red-500/20 bg-red-500/10 p-4 text-red-400">
            <p className="text-sm">{error}</p>
          </div>
        )}

        <div className="space-y-6">
          {/* Video Quality */}
          <div>
            <label className="mb-2 block text-sm font-medium">Video Quality</label>
            <select
              value={config.quality}
              onChange={(e) => updateField('quality', e.target.value as any)}
              className="w-full rounded-lg border border-zinc-800 bg-zinc-900 px-4 py-2 text-white focus:border-zinc-700 focus:outline-none focus:ring-2 focus:ring-zinc-700"
            >
              <option value="auto">Auto</option>
              <option value="1080">1080p</option>
              <option value="720">720p</option>
              <option value="480">480p</option>
            </select>
          </div>

          {/* Provider */}
          <div>
            <label className="mb-2 block text-sm font-medium">Streaming Provider</label>
            <select
              value={config.provider}
              onChange={(e) => updateField('provider', e.target.value as any)}
              className="w-full rounded-lg border border-zinc-800 bg-zinc-900 px-4 py-2 text-white focus:border-zinc-700 focus:outline-none focus:ring-2 focus:ring-zinc-700"
            >
              <option value="Vidcloud">Vidcloud</option>
              <option value="UpCloud">UpCloud</option>
            </select>
          </div>

          {/* Subtitles Language */}
          <div>
            <label className="mb-2 block text-sm font-medium">Subtitle Language</label>
            <input
              type="text"
              value={config.subsLanguage}
              onChange={(e) => updateField('subsLanguage', e.target.value)}
              placeholder="en"
              className="w-full rounded-lg border border-zinc-800 bg-zinc-900 px-4 py-2 text-white focus:border-zinc-700 focus:outline-none focus:ring-2 focus:ring-zinc-700"
            />
          </div>

          {/* Subtitles Enabled */}
          <div className="flex items-center justify-between">
            <label className="text-sm font-medium">Enable Subtitles</label>
            <button
              onClick={() => updateField('subtitlesEnabled', !config.subtitlesEnabled)}
              className={`relative h-6 w-11 rounded-full transition-colors ${
                config.subtitlesEnabled ? 'bg-white' : 'bg-zinc-700'
              }`}
            >
              <span
                className={`absolute top-0.5 h-5 w-5 rounded-full bg-black transition-transform ${
                  config.subtitlesEnabled ? 'translate-x-5' : 'translate-x-0.5'
                }`}
              />
            </button>
          </div>

          {/* History Enabled */}
          <div className="flex items-center justify-between">
            <label className="text-sm font-medium">Enable Watch History</label>
            <button
              onClick={() => updateField('historyEnabled', !config.historyEnabled)}
              className={`relative h-6 w-11 rounded-full transition-colors ${
                config.historyEnabled ? 'bg-white' : 'bg-zinc-700'
              }`}
            >
              <span
                className={`absolute top-0.5 h-5 w-5 rounded-full bg-black transition-transform ${
                  config.historyEnabled ? 'translate-x-5' : 'translate-x-0.5'
                }`}
              />
            </button>
          </div>

          {/* Discord Presence */}
          <div className="flex items-center justify-between">
            <label className="text-sm font-medium">Discord Rich Presence</label>
            <button
              onClick={() => updateField('discordPresence', !config.discordPresence)}
              className={`relative h-6 w-11 rounded-full transition-colors ${
                config.discordPresence ? 'bg-white' : 'bg-zinc-700'
              }`}
            >
              <span
                className={`absolute top-0.5 h-5 w-5 rounded-full bg-black transition-transform ${
                  config.discordPresence ? 'translate-x-5' : 'translate-x-0.5'
                }`}
              />
            </button>
          </div>

          {/* OMDb API Key */}
          <div>
            <label className="mb-2 block text-sm font-medium">
              OMDb API Key (for IMDb ratings)
            </label>
            <input
              type="text"
              value={config.omdbApiKey || ''}
              onChange={(e) => updateField('omdbApiKey', e.target.value)}
              placeholder="Enter your OMDb API key"
              className="w-full rounded-lg border border-zinc-800 bg-zinc-900 px-4 py-2 text-white focus:border-zinc-700 focus:outline-none focus:ring-2 focus:ring-zinc-700"
            />
            <p className="mt-1 text-xs text-zinc-500">
              Get your free API key at{' '}
              <a
                href="https://www.omdbapi.com/apikey.aspx"
                target="_blank"
                rel="noopener noreferrer"
                className="text-zinc-400 hover:text-white"
              >
                omdbapi.com
              </a>
            </p>
          </div>

          {/* MCP Server Settings */}
          <div className="border-t border-zinc-800 pt-6">
            <h2 className="mb-4 text-lg font-semibold">MCP Server</h2>

            <div className="space-y-4">
              <div>
                <label className="mb-2 block text-sm font-medium">MCP Host</label>
                <input
                  type="text"
                  value={config.mcpHost}
                  onChange={(e) => updateField('mcpHost', e.target.value)}
                  className="w-full rounded-lg border border-zinc-800 bg-zinc-900 px-4 py-2 text-white focus:border-zinc-700 focus:outline-none focus:ring-2 focus:ring-zinc-700"
                />
              </div>

              <div>
                <label className="mb-2 block text-sm font-medium">MCP Port</label>
                <input
                  type="number"
                  value={config.mcpPort}
                  onChange={(e) => updateField('mcpPort', parseInt(e.target.value))}
                  className="w-full rounded-lg border border-zinc-800 bg-zinc-900 px-4 py-2 text-white focus:border-zinc-700 focus:outline-none focus:ring-2 focus:ring-zinc-700"
                />
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
