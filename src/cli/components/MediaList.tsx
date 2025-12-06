import React from 'react';
import { Box, Text } from 'ink';
import SelectInput from 'ink-select-input';
import type { MediaItem } from '../../core/types.js';

interface MediaListProps {
  items: MediaItem[];
  title?: string;
  onSelect: (item: MediaItem) => void;
  onCancel?: () => void;
}

export const MediaList: React.FC<MediaListProps> = ({
  items,
  title,
  onSelect,
  onCancel,
}) => {
  const selectItems = items.map((item, index) => ({
    label: `${item.type === 'movie' ? '🎬' : '📺'} ${item.title}${item.releaseDate ? ` (${item.releaseDate})` : ''}${item.quality ? ` [${item.quality}]` : ''}`,
    value: item,
    key: `${item.id}-${index}`,
  }));

  // Add cancel option
  if (onCancel) {
    selectItems.push({
      label: '← Back',
      value: null as any,
      key: 'cancel',
    });
  }

  const handleSelect = (selected: { value: MediaItem | null }) => {
    if (selected.value === null && onCancel) {
      onCancel();
    } else if (selected.value) {
      onSelect(selected.value);
    }
  };

  return (
    <Box flexDirection="column">
      {title && (
        <Box marginBottom={1}>
          <Text bold color="cyan">
            {title}
          </Text>
        </Box>
      )}
      {items.length === 0 ? (
        <Text dimColor>No items found</Text>
      ) : (
        <SelectInput items={selectItems} onSelect={handleSelect} />
      )}
    </Box>
  );
};
