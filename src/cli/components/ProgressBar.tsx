import React from 'react';
import { Box, Text } from 'ink';

interface ProgressBarProps {
  current: number;
  total: number;
  width?: number;
  showPercentage?: boolean;
}

export const ProgressBar: React.FC<ProgressBarProps> = ({
  current,
  total,
  width = 40,
  showPercentage = true,
}) => {
  const percentage = Math.round((current / total) * 100);
  const filled = Math.round((current / total) * width);
  const empty = width - filled;

  const bar = '█'.repeat(filled) + '░'.repeat(empty);

  return (
    <Box>
      <Text color="cyan">{bar}</Text>
      {showPercentage && (
        <Text dimColor> {percentage}%</Text>
      )}
    </Box>
  );
};
