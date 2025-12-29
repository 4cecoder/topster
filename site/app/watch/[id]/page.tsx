'use client';

import { useState, useEffect } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { VideoPlayer } from '@/components/video-player';
import { getMediaSeasons, getVideoSources } from '@/lib/actions/media';
import { updateWatchHistory } from '@/lib/actions/history';
import type { Season, Episode, StreamingData } from '@/lib/types';
import { ArrowLeft, Play } from 'lucide-react';
import Link from 'next/link';

export default function WatchPage() {
  const params = useParams();
  const router = useRouter();
  const mediaId = params.id as string;

  const [seasons, setSeasons] = useState<Season[]>([]);
  const [selectedSeason, setSelectedSeason] = useState<number>(1);
  const [selectedEpisode, setSelectedEpisode] = useState<Episode | null>(null);
  const [streamingData, setStreamingData] = useState<StreamingData | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    loadSeasons();
  }, [mediaId]);

  const loadSeasons = async () => {
    try {
      setIsLoading(true);
      const data = await getMediaSeasons(mediaId);
      setSeasons(data);

      if (data.length > 0 && data[0].episodes.length > 0) {
        setSelectedSeason(1);
      }
    } catch (err) {
      setError('Failed to load seasons');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const playEpisode = async (episode: Episode) => {
    try {
      setIsLoading(true);
      setSelectedEpisode(episode);

      const data = await getVideoSources(episode.id);
      setStreamingData(data);
    } catch (err) {
      setError('Failed to load video sources');
      console.error(err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleTimeUpdate = async (currentTime: number, duration: number) => {
    if (!selectedEpisode) return;

    // Update watch history every 10 seconds
    if (Math.floor(currentTime) % 10 === 0) {
      try {
        await updateWatchHistory({
          mediaId,
          title: selectedEpisode.title,
          type: 'tv',
          position: currentTime,
          duration,
          season: selectedEpisode.season,
          episode: selectedEpisode.number,
        });
      } catch (err) {
        console.error('Failed to update history:', err);
      }
    }
  };

  const currentSeasonData = seasons.find(s => s.season === selectedSeason);

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
        {/* Video Player */}
        {streamingData && streamingData.sources.length > 0 && (
          <div className="mb-8">
            <VideoPlayer
              src={streamingData.sources[0].url}
              subtitles={streamingData.subtitles}
              onTimeUpdate={handleTimeUpdate}
            />
            {selectedEpisode && (
              <div className="mt-4">
                <h1 className="text-2xl font-bold">{selectedEpisode.title}</h1>
                <p className="text-zinc-400">
                  Season {selectedEpisode.season} • Episode {selectedEpisode.number}
                </p>
              </div>
            )}
          </div>
        )}

        {/* Error Message */}
        {error && (
          <div className="mb-6 rounded-lg border border-red-500/20 bg-red-500/10 p-4 text-red-400">
            <p className="text-sm">{error}</p>
          </div>
        )}

        {/* Season Selector */}
        {seasons.length > 0 && (
          <div className="mb-6">
            <h2 className="mb-4 text-xl font-semibold">Seasons</h2>
            <div className="flex gap-2 overflow-x-auto pb-2">
              {seasons.map((season) => (
                <button
                  key={season.season}
                  onClick={() => setSelectedSeason(season.season)}
                  className={`rounded-lg px-4 py-2 text-sm font-medium transition-colors ${
                    selectedSeason === season.season
                      ? 'bg-white text-black'
                      : 'bg-zinc-800 text-white hover:bg-zinc-700'
                  }`}
                >
                  Season {season.season}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Episodes List */}
        {currentSeasonData && (
          <div>
            <h2 className="mb-4 text-xl font-semibold">
              Episodes ({currentSeasonData.episodes.length})
            </h2>
            <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
              {currentSeasonData.episodes.map((episode) => (
                <button
                  key={episode.id}
                  onClick={() => playEpisode(episode)}
                  disabled={isLoading}
                  className="group flex items-start gap-4 rounded-lg bg-zinc-900 p-4 text-left transition-colors hover:bg-zinc-800 disabled:opacity-50"
                >
                  <div className="flex h-10 w-10 flex-shrink-0 items-center justify-center rounded-full bg-zinc-800 group-hover:bg-white/10">
                    <Play className="h-5 w-5" />
                  </div>
                  <div className="flex-1">
                    <div className="text-sm font-medium">{episode.title}</div>
                    <div className="mt-1 text-xs text-zinc-400">
                      Episode {episode.number}
                    </div>
                  </div>
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Loading State */}
        {isLoading && !streamingData && (
          <div className="flex justify-center py-12">
            <div className="h-12 w-12 animate-spin rounded-full border-4 border-zinc-700 border-t-white" />
          </div>
        )}
      </main>
    </div>
  );
}
