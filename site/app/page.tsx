'use client';

import { useState, useEffect } from 'react';
import { SearchBar } from '@/components/search-bar';
import { MediaCard } from '@/components/media-card';
import { searchMedia, getTrendingMedia, getRecentMedia } from '@/lib/actions/media';
import { discoverTVDevices } from '@/lib/lan-discovery';
import type { MediaItem, TVDevice } from '@/lib/types';
import { Tv2, TrendingUp, Clock } from 'lucide-react';
import Link from 'next/link';

export default function Home() {
  const [searchResults, setSearchResults] = useState<MediaItem[]>([]);
  const [trending, setTrending] = useState<MediaItem[]>([]);
  const [recent, setRecent] = useState<MediaItem[]>([]);
  const [tvDevices, setTvDevices] = useState<TVDevice[]>([]);
  const [activeTab, setActiveTab] = useState<'search' | 'trending' | 'recent'>('trending');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Load trending content on mount
    loadTrending();
    loadRecent();
    loadTVDevices();
  }, []);

  const loadTrending = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await getTrendingMedia();
      setTrending(data);
    } catch (err) {
      setError('Failed to load trending content. Make sure the MCP server is running: topster --mcp');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const loadRecent = async () => {
    try {
      const data = await getRecentMedia();
      setRecent(data);
    } catch (err) {
      console.error('Failed to load recent:', err);
    }
  };

  const loadTVDevices = async () => {
    try {
      const devices = await discoverTVDevices();
      setTvDevices(devices.filter(d => d.online));
    } catch (err) {
      console.error('Failed to discover TV devices:', err);
    }
  };

  const handleSearch = async (query: string) => {
    if (!query.trim()) {
      setSearchResults([]);
      setActiveTab('trending');
      return;
    }

    try {
      setIsLoading(true);
      setError(null);
      setActiveTab('search');
      const results = await searchMedia(query);
      setSearchResults(results);
    } catch (err) {
      setError('Search failed. Make sure the MCP server is running: topster --mcp');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const displayedMedia =
    activeTab === 'search' ? searchResults :
    activeTab === 'trending' ? trending :
    recent;

  return (
    <div className="min-h-screen bg-black text-white">
      {/* Header */}
      <header className="border-b border-zinc-800 bg-zinc-900/50 backdrop-blur-sm">
        <div className="container mx-auto flex items-center justify-between px-6 py-4">
          <Link href="/" className="text-2xl font-bold">
            Topster
          </Link>

          <nav className="flex items-center gap-6">
            <Link
              href="/history"
              className="text-sm text-zinc-400 transition-colors hover:text-white"
            >
              History
            </Link>
            <Link
              href="/settings"
              className="text-sm text-zinc-400 transition-colors hover:text-white"
            >
              Settings
            </Link>
            {tvDevices.length > 0 && (
              <button
                onClick={loadTVDevices}
                className="flex items-center gap-2 rounded-full bg-green-600/20 px-3 py-1.5 text-sm text-green-400"
              >
                <Tv2 className="h-4 w-4" />
                {tvDevices.length} TV{tvDevices.length > 1 ? 's' : ''}
              </button>
            )}
          </nav>
        </div>
      </header>

      <main className="container mx-auto px-6 py-8">
        {/* Search */}
        <div className="mb-8 flex justify-center">
          <SearchBar onSearch={handleSearch} />
        </div>

        {/* Tabs */}
        <div className="mb-6 flex gap-4 border-b border-zinc-800">
          <button
            onClick={() => setActiveTab('trending')}
            className={`flex items-center gap-2 px-4 py-2 text-sm font-medium transition-colors ${
              activeTab === 'trending'
                ? 'border-b-2 border-white text-white'
                : 'text-zinc-400 hover:text-white'
            }`}
          >
            <TrendingUp className="h-4 w-4" />
            Trending
          </button>
          <button
            onClick={() => setActiveTab('recent')}
            className={`flex items-center gap-2 px-4 py-2 text-sm font-medium transition-colors ${
              activeTab === 'recent'
                ? 'border-b-2 border-white text-white'
                : 'text-zinc-400 hover:text-white'
            }`}
          >
            <Clock className="h-4 w-4" />
            Recent
          </button>
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-6 rounded-lg border border-red-500/20 bg-red-500/10 p-4 text-red-400">
            <p className="text-sm">{error}</p>
          </div>
        )}

        {/* Loading */}
        {isLoading && (
          <div className="flex justify-center py-12">
            <div className="h-12 w-12 animate-spin rounded-full border-4 border-zinc-700 border-t-white" />
          </div>
        )}

        {/* Media Grid */}
        {!isLoading && displayedMedia.length > 0 && (
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-5 xl:grid-cols-6">
            {displayedMedia.map((media) => (
              <MediaCard key={media.id} media={media} />
            ))}
          </div>
        )}

        {/* Empty State */}
        {!isLoading && !error && displayedMedia.length === 0 && (
          <div className="flex flex-col items-center justify-center py-24 text-center">
            <p className="text-lg text-zinc-400">
              {activeTab === 'search'
                ? 'No results found'
                : 'No content available'}
            </p>
            <p className="mt-2 text-sm text-zinc-500">
              Make sure the MCP server is running with <code className="rounded bg-zinc-800 px-2 py-1">topster --mcp</code>
            </p>
          </div>
        )}
      </main>
    </div>
  );
}
