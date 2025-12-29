'use client';

import { useState, useEffect } from 'react';
import { getWatchHistory } from '@/lib/actions/history';
import type { HistoryItem } from '@/lib/types';
import { ArrowLeft, Play, Clock } from 'lucide-react';
import Link from 'next/link';
import Image from 'next/image';

export default function HistoryPage() {
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadHistory();
  }, []);

  const loadHistory = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await getWatchHistory();
      setHistory(data);
    } catch (err) {
      setError('Failed to load watch history');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const formatProgress = (position: number, duration: number) => {
    return Math.floor((position / duration) * 100);
  };

  const formatTime = (seconds: number) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = Math.floor(seconds % 60);

    if (h > 0) {
      return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    }
    return `${m}:${s.toString().padStart(2, '0')}`;
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const now = new Date();
    const diffMs = now.getTime() - date.getTime();
    const diffMins = Math.floor(diffMs / 60000);
    const diffHours = Math.floor(diffMs / 3600000);
    const diffDays = Math.floor(diffMs / 86400000);

    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffHours < 24) return `${diffHours}h ago`;
    if (diffDays < 7) return `${diffDays}d ago`;
    return date.toLocaleDateString();
  };

  return (
    <div className="min-h-screen bg-black text-white">
      {/* Header */}
      <header className="border-b border-zinc-800 bg-zinc-900/50 backdrop-blur-sm">
        <div className="container mx-auto px-6 py-4">
          <Link
            href="/"
            className="inline-flex items-center gap-2 text-sm text-zinc-400 transition-colors hover:text-white"
          >
            <ArrowLeft className="h-4 w-4" />
            Back to Browse
          </Link>
        </div>
      </header>

      <main className="container mx-auto px-6 py-8">
        <div className="mb-8 flex items-center gap-3">
          <Clock className="h-8 w-8" />
          <h1 className="text-3xl font-bold">Watch History</h1>
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-6 rounded-lg border border-red-500/20 bg-red-500/10 p-4 text-red-400">
            <p className="text-sm">{error}</p>
          </div>
        )}

        {/* Loading State */}
        {isLoading && (
          <div className="flex justify-center py-12">
            <div className="h-12 w-12 animate-spin rounded-full border-4 border-zinc-700 border-t-white" />
          </div>
        )}

        {/* History List */}
        {!isLoading && history.length > 0 && (
          <div className="space-y-4">
            {history.map((item) => (
              <Link
                key={item.id}
                href={`/watch/${item.mediaId}`}
                className="group flex gap-4 rounded-lg bg-zinc-900 p-4 transition-colors hover:bg-zinc-800"
              >
                {/* Thumbnail */}
                {item.image && (
                  <div className="relative h-24 w-16 flex-shrink-0 overflow-hidden rounded">
                    <Image
                      src={item.image}
                      alt={item.title}
                      fill
                      className="object-cover"
                    />
                    <div className="absolute inset-0 flex items-center justify-center bg-black/50 opacity-0 transition-opacity group-hover:opacity-100">
                      <Play className="h-6 w-6" />
                    </div>
                  </div>
                )}

                <div className="flex-1">
                  <h3 className="font-medium">{item.title}</h3>

                  <div className="mt-1 flex items-center gap-2 text-sm text-zinc-400">
                    <span>{item.type === 'tv' ? 'TV Series' : 'Movie'}</span>
                    {item.season && item.episode && (
                      <>
                        <span>•</span>
                        <span>S{item.season} E{item.episode}</span>
                      </>
                    )}
                    <span>•</span>
                    <span>{formatDate(item.lastWatched)}</span>
                  </div>

                  {/* Progress Bar */}
                  <div className="mt-3">
                    <div className="mb-1 flex items-center justify-between text-xs text-zinc-500">
                      <span>{formatTime(item.position)}</span>
                      <span>{formatProgress(item.position, item.duration)}%</span>
                      <span>{formatTime(item.duration)}</span>
                    </div>
                    <div className="h-1 w-full overflow-hidden rounded-full bg-zinc-700">
                      <div
                        className="h-full bg-white transition-all"
                        style={{ width: `${formatProgress(item.position, item.duration)}%` }}
                      />
                    </div>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        )}

        {/* Empty State */}
        {!isLoading && history.length === 0 && (
          <div className="flex flex-col items-center justify-center py-24 text-center">
            <Clock className="mb-4 h-16 w-16 text-zinc-700" />
            <p className="text-lg text-zinc-400">No watch history yet</p>
            <p className="mt-2 text-sm text-zinc-500">
              Start watching some content to see it here
            </p>
            <Link
              href="/"
              className="mt-6 rounded-full bg-white px-6 py-2 text-sm font-medium text-black transition-colors hover:bg-zinc-200"
            >
              Browse Content
            </Link>
          </div>
        )}
      </main>
    </div>
  );
}
