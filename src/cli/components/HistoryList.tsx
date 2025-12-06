import React from 'react';
import { Box, Text } from 'ink';
import SelectInput from 'ink-select-input';
import type { HistoryEntry } from '../../modules/history/types.js';

interface HistoryListProps {
  entries: HistoryEntry[];
  onSelect: (entry: HistoryEntry) => void;
  onCancel?: () => void;
}

export const HistoryList: React.FC<HistoryListProps> = ({
  entries,
  onSelect,
  onCancel,
}) => {
  const formatProgress = (entry: HistoryEntry): string => {
    if (entry.currentTime && entry.duration) {
      const progress = Math.round((entry.currentTime / entry.duration) * 100);
      return `[${progress}%]`;
    }
    return '';
  };

  const formatLabel = (entry: HistoryEntry): string => {
    const icon = entry.type === 'movie' ? '🎬' : '📺';
    const title = entry.title;
    const progress = formatProgress(entry);

    if (entry.type === 'tv' && entry.season && entry.episode) {
      return `${icon} ${title} S${String(entry.season).padStart(2, '0')}E${String(entry.episode).padStart(2, '0')} ${progress}`;
    }

    return `${icon} ${title} ${progress}`;
  };

  const selectItems = entries.map((entry, index) => ({
    label: formatLabel(entry),
    value: entry,
    key: `${entry.id}-${index}`,
  }));

  if (onCancel) {
    selectItems.push({
      label: '← Back',
      value: null as any,
      key: 'cancel',
    });
  }

  const handleSelect = (selected: { value: HistoryEntry | null }) => {
    if (selected.value === null && onCancel) {
      onCancel();
    } else if (selected.value) {
      onSelect(selected.value);
    }
  };

  return (
    <Box flexDirection="column">
      <Box marginBottom={1}>
        <Text bold color="cyan">
          Continue Watching
        </Text>
      </Box>
      {entries.length === 0 ? (
        <Text dimColor>No watch history found</Text>
      ) : (
        <SelectInput items={selectItems} onSelect={handleSelect} />
      )}
    </Box>
  );
};
