import React from 'react';
import { Box, Text } from 'ink';
import SelectInput from 'ink-select-input';
import type { HistoryEntry } from '../../core/types.js';

interface HistoryListProps {
  entries: HistoryEntry[];
  onSelect: (entry: HistoryEntry) => void;
  onCancel?: () => void;
  currentPage?: number;
  totalEntries?: number;
  hasMore?: boolean;
  onNextPage?: () => void;
  onPrevPage?: () => void;
}

export const HistoryList: React.FC<HistoryListProps> = ({
  entries,
  onSelect,
  onCancel,
  currentPage = 0,
  totalEntries = 0,
  hasMore = false,
  onNextPage,
  onPrevPage,
}) => {
  const formatProgress = (entry: HistoryEntry): string => {
    if (entry.percentWatched > 0) {
      return `[${Math.round(entry.percentWatched)}%]`;
    }
    return '';
  };

  const formatLabel = (entry: HistoryEntry): string => {
    const icon = entry.type === 'movie' ? '🎬' : '📺';
    const progress = formatProgress(entry);

    if (entry.type === 'tv' && entry.seasonNumber && entry.episodeNumber) {
      // For TV shows, show the episode info more prominently
      const episodeInfo = `S${String(entry.seasonNumber).padStart(2, '0')}E${String(entry.episodeNumber).padStart(2, '0')}`;
      const episodeTitle = entry.episodeTitle ? ` - ${entry.episodeTitle}` : '';
      return `${icon} ${entry.title} ${episodeInfo}${episodeTitle} ${progress}`;
    }

    return `${icon} ${entry.title} ${progress}`;
  };

  const selectItems = entries.map((entry, index) => ({
    label: formatLabel(entry),
    value: entry,
    key: `${entry.id}-${entry.episodeId || 'main'}-${index}`,
  }));

  // Add pagination controls only if pagination is actually enabled
  if (currentPage > 0 && onPrevPage) {
    selectItems.unshift({
      label: '⬅️ Previous Page',
      value: 'prev' as any,
      key: 'prev-page',
    });
  }

  if (hasMore && onNextPage) {
    selectItems.push({
      label: '➡️ Next Page',
      value: 'next' as any,
      key: 'next-page',
    });
  }

  if (onCancel) {
    selectItems.push({
      label: '← Back',
      value: null as any,
      key: 'cancel',
    });
  }

  const handleSelect = (selected: { value: HistoryEntry | string | null }) => {
    if (selected.value === 'prev' && onPrevPage) {
      onPrevPage();
    } else if (selected.value === 'next' && onNextPage) {
      onNextPage();
    } else if (selected.value === null && onCancel) {
      onCancel();
    } else if (selected.value && typeof selected.value === 'object') {
      onSelect(selected.value as HistoryEntry);
    }
  };

  return (
    <Box flexDirection="column">
      <Box marginBottom={1}>
        <Text bold color="cyan">
          Continue Watching
        </Text>
        {totalEntries > 0 && (
          <Text dimColor>
            {' '}({entries.length} shown, {totalEntries} total)
          </Text>
        )}
      </Box>
      {entries.length === 0 ? (
        <Text dimColor>No watch history found</Text>
      ) : (
        <SelectInput items={selectItems} onSelect={handleSelect} />
      )}
    </Box>
  );
};
