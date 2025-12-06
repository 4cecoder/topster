import React from 'react';
import { Box, Text } from 'ink';
import type { MediaItem as MediaItemType } from '../../core/types.js';

interface MediaItemProps {
  item: MediaItemType;
  isSelected?: boolean;
  showIndex?: boolean;
  index?: number;
}

export const MediaItem: React.FC<MediaItemProps> = ({
  item,
  isSelected = false,
  showIndex = false,
  index,
}) => {
  const icon = item.type === 'movie' ? '🎬' : '📺';
  const quality = item.quality ? `[${item.quality}]` : '';

  return (
    <Box>
      {showIndex && index !== undefined && (
        <Text color={isSelected ? 'cyan' : 'gray'}>
          {String(index + 1).padStart(2, ' ')}.
        </Text>
      )}
      <Text color={isSelected ? 'cyan' : undefined} bold={isSelected}>
        {isSelected && '▶ '}
        {icon} {item.title}
      </Text>
      {item.releaseDate && (
        <Text dimColor> ({item.releaseDate})</Text>
      )}
      {quality && (
        <Text color="green"> {quality}</Text>
      )}
    </Box>
  );
};
