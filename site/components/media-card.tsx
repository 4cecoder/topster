'use client';

import Image from 'next/image';
import Link from 'next/link';
import type { MediaItem } from '@/lib/types';

interface MediaCardProps {
  media: MediaItem;
}

export function MediaCard({ media }: MediaCardProps) {
  return (
    <Link
      href={`/watch/${media.id}`}
      className="group block overflow-hidden rounded-lg bg-zinc-900 transition-transform hover:scale-105"
    >
      <div className="relative aspect-[2/3] w-full">
        <Image
          src={media.image || '/placeholder.jpg'}
          alt={media.title}
          fill
          className="object-cover"
          sizes="(max-width: 768px) 50vw, (max-width: 1200px) 33vw, 25vw"
        />
        <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent opacity-0 transition-opacity group-hover:opacity-100" />

        {media.type && (
          <div className="absolute right-2 top-2 rounded bg-black/60 px-2 py-1 text-xs font-medium text-white backdrop-blur-sm">
            {media.type}
          </div>
        )}
      </div>

      <div className="p-3">
        <h3 className="line-clamp-2 text-sm font-medium text-white">
          {media.title}
        </h3>

        <div className="mt-1 flex items-center gap-2 text-xs text-zinc-400">
          {media.releaseDate && <span>{media.releaseDate}</span>}
          {media.seasons && (
            <>
              <span>•</span>
              <span>{media.seasons} Season{media.seasons > 1 ? 's' : ''}</span>
            </>
          )}
        </div>
      </div>
    </Link>
  );
}
