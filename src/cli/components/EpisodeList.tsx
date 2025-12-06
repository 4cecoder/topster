import React from 'react';
import { Box, Text } from 'ink';
import SelectInput from 'ink-select-input';

export interface Episode {
  number: number;
  title: string;
}

interface EpisodeListProps {
  episodes: Episode[];
  seasonNumber: number;
  onSelect: (episode: Episode) => void;
  onCancel?: () => void;
}

export const EpisodeList: React.FC<EpisodeListProps> = ({
  episodes,
  seasonNumber,
  onSelect,
  onCancel,
}) => {
  const selectItems = episodes.map((episode) => ({
    label: `S${String(seasonNumber).padStart(2, '0')}E${String(episode.number).padStart(2, '0')} - ${episode.title}`,
    value: episode,
  }));

  if (onCancel) {
    selectItems.push({
      label: '← Back',
      value: null as any,
    });
  }

  const handleSelect = (selected: { value: Episode | null }) => {
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
          Select Episode - Season {seasonNumber}
        </Text>
      </Box>
      <SelectInput items={selectItems} onSelect={handleSelect} />
    </Box>
  );
};
