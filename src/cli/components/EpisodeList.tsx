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
  episodeProgress?: Record<string, { position: string; percent: number; completed: boolean }>;
}

export const EpisodeList: React.FC<EpisodeListProps> = ({
  episodes,
  seasonNumber,
  onSelect,
  onCancel,
  episodeProgress,
}) => {
  const selectItems = episodes.map((episode) => {
    const progress = episodeProgress?.[`s${seasonNumber}e${episode.number}`];
    const progressText = progress
      ? progress.completed
        ? ' ✓'
        : ` [${progress.percent.toFixed(0)}%]`
      : '';
    return {
      label: `S${String(seasonNumber).padStart(2, '0')}E${String(episode.number).padStart(2, '0')} - ${episode.title}${progressText}`,
      value: episode,
      key: `s${seasonNumber}e${episode.number}`,
    };
  });

  if (onCancel) {
    selectItems.push({
      label: '← Back',
      value: null as any,
      key: 'cancel',
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
