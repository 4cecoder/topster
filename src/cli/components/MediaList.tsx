import React from 'react';
import { Box, Text } from 'ink';
import SelectInput from 'ink-select-input';
import type { MediaItem } from '../../core/types.js';

interface MediaListProps {
  items: MediaItem[];
  title?: string;
  onSelect: (item: MediaItem) => void;
  onCancel?: () => void;
  currentPage?: number;
  totalPages?: number;
  onNextPage?: () => void;
  onPrevPage?: () => void;
}

type SelectValue = MediaItem | 'next' | 'prev' | null;

export const MediaList: React.FC<MediaListProps> = ({
  items,
  title,
  onSelect,
  onCancel,
  currentPage,
  totalPages,
  onNextPage,
  onPrevPage,
}) => {
  const selectItems = items.map((item, index) => ({
    label: `${item.type === 'movie' ? '🎬' : '📺'} ${item.title}${item.releaseDate ? ` (${item.releaseDate})` : ''}${item.quality ? ` [${item.quality}]` : ''}`,
    value: item as SelectValue,
    key: `${item.id}-${index}`,
  }));

  // Add pagination controls if applicable
  if (currentPage && totalPages && totalPages > 1) {
    if (currentPage < totalPages && onNextPage) {
      selectItems.push({
        label: `→ Next Page (${currentPage + 1}/${totalPages})`,
        value: 'next' as SelectValue,
        key: 'next-page',
      });
    }
    if (currentPage > 1 && onPrevPage) {
      selectItems.push({
        label: `← Previous Page (${currentPage - 1}/${totalPages})`,
        value: 'prev' as SelectValue,
        key: 'prev-page',
      });
    }
  }

  // Add cancel option
  if (onCancel) {
    selectItems.push({
      label: '← Back',
      value: null as SelectValue,
      key: 'cancel',
    });
  }

  const handleSelect = (selected: { value: SelectValue }) => {
    if (selected.value === 'next' && onNextPage) {
      onNextPage();
    } else if (selected.value === 'prev' && onPrevPage) {
      onPrevPage();
    } else if (selected.value === null && onCancel) {
      onCancel();
    } else if (selected.value && typeof selected.value !== 'string') {
      onSelect(selected.value);
    }
  };

  return (
    <Box flexDirection="column">
      {title && (
        <Box marginBottom={1}>
          <Text bold color="cyan">
            {title}
            {currentPage && totalPages && totalPages > 1 && (
              <Text dimColor> (Page {currentPage}/{totalPages})</Text>
            )}
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
