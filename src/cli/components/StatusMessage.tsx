import React from 'react';
import { Box, Text } from 'ink';

interface StatusMessageProps {
  type: 'success' | 'error' | 'info' | 'warning';
  message: string;
}

export const StatusMessage: React.FC<StatusMessageProps> = ({
  type,
  message,
}) => {
  const config = {
    success: { icon: '✓', color: 'green' as const },
    error: { icon: '✗', color: 'red' as const },
    info: { icon: 'ℹ', color: 'blue' as const },
    warning: { icon: '⚠', color: 'yellow' as const },
  };

  const { icon, color } = config[type];

  return (
    <Box>
      <Text color={color} bold>
        {icon} {message}
      </Text>
    </Box>
  );
};
