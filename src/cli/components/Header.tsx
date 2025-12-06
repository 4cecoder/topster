import React from 'react';
import { Box, Text } from 'ink';
import Gradient from 'ink-gradient';
import BigText from 'ink-big-text';

interface HeaderProps {
  showBigLogo?: boolean;
}

export const Header: React.FC<HeaderProps> = ({ showBigLogo = false }) => {
  if (showBigLogo) {
    return (
      <Box flexDirection="column" marginBottom={1}>
        <Gradient name="rainbow">
          <BigText text="TOPSTER" font="chrome" />
        </Gradient>
        <Box justifyContent="center">
          <Text dimColor>Stream movies & TV shows from the terminal</Text>
        </Box>
      </Box>
    );
  }

  return (
    <Box borderStyle="round" borderColor="cyan" paddingX={1} marginBottom={1}>
      <Gradient name="rainbow">
        <Text bold>🎬 TOPSTER</Text>
      </Gradient>
      <Text dimColor> | Stream movies & TV shows</Text>
    </Box>
  );
};
