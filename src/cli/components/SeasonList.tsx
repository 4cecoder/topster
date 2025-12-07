import React from 'react';
import { Box, Text } from 'ink';
import SelectInput from 'ink-select-input';

export interface Season {
  number: number;
  title: string;
}

interface SeasonListProps {
  seasons: Season[];
  onSelect: (season: Season) => void;
  onCancel?: () => void;
}

export const SeasonList: React.FC<SeasonListProps> = ({
  seasons,
  onSelect,
  onCancel,
}) => {
  const selectItems = seasons.map((season) => ({
    label: `📺 ${season.title}`,
    value: season,
    key: `season-${season.number}`,
  }));

  if (onCancel) {
    selectItems.push({
      label: '← Back',
      value: null as any,
      key: 'cancel',
    });
  }

  const handleSelect = (selected: { value: Season | null }) => {
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
          Select Season
        </Text>
      </Box>
      <SelectInput items={selectItems} onSelect={handleSelect} />
    </Box>
  );
};
