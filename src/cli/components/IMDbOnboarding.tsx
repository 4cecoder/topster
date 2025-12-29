import React, { useState } from 'react';
import { Box, Text, useInput } from 'ink';
import TextInput from 'ink-text-input';

interface IMDbOnboardingProps {
  onComplete: (apiKey: string) => void;
  onSkip: () => void;
  step?: 'intro' | 'input';
}

export const IMDbOnboarding: React.FC<IMDbOnboardingProps> = ({ onComplete, onSkip, step: externalStep }) => {
  const [internalStep, setInternalStep] = useState<'intro' | 'input'>('intro');
  const [apiKey, setApiKey] = useState('');

  // Use external step if provided, otherwise use internal state
  const step = externalStep || internalStep;

  // Only handle input if using internal step management
  useInput((input, key) => {
    if (!externalStep) {
      if (step === 'intro' && key.return) {
        setInternalStep('input');
      }
    }
  });

  if (step === 'intro') {
    return (
      <Box
        flexDirection="column"
        borderStyle="double"
        borderColor="cyan"
        paddingX={3}
        paddingY={1}
        width={70}
        backgroundColor="black"
      >
        <Box marginBottom={1}>
          <Text bold color="cyan">
            🎬 New Feature: IMDb Lookup!
          </Text>
        </Box>

        <Box flexDirection="column" marginBottom={1}>
          <Text>Press <Text bold color="yellow">i</Text> while browsing to view detailed IMDb info:</Text>
          <Text dimColor>  • Ratings & reviews</Text>
          <Text dimColor>  • Cast & crew</Text>
          <Text dimColor>  • Plot synopsis</Text>
          <Text dimColor>  • Awards & more</Text>
        </Box>

        <Box flexDirection="column" marginBottom={1}>
          <Text bold color="green">Quick Setup (30 seconds):</Text>
          <Text dimColor>1. Visit: <Text color="cyan">omdbapi.com/apikey.aspx</Text></Text>
          <Text dimColor>2. Enter your email</Text>
          <Text dimColor>3. Check email & click activation link</Text>
          <Text dimColor>4. Copy your API key & paste below</Text>
        </Box>

        <Box marginTop={1}>
          <Text dimColor>Press <Text bold>Enter</Text> to continue or <Text bold>ESC</Text> to skip</Text>
        </Box>
      </Box>
    );
  }

  return (
    <Box
      flexDirection="column"
      borderStyle="double"
      borderColor="cyan"
      paddingX={3}
      paddingY={1}
      width={70}
      backgroundColor="black"
    >
      <Box marginBottom={1}>
        <Text bold color="cyan">
          Enter Your OMDb API Key
        </Text>
      </Box>

      <Box marginBottom={1}>
        <Text dimColor>
          Get your free key at: <Text color="cyan">omdbapi.com/apikey.aspx</Text>
        </Text>
      </Box>

      <Box marginBottom={1}>
        <Text>API Key: </Text>
        <TextInput
          value={apiKey}
          onChange={setApiKey}
          onSubmit={() => {
            if (apiKey.trim()) {
              onComplete(apiKey.trim());
            }
          }}
          placeholder="Paste your API key here..."
        />
      </Box>

      <Box marginTop={1}>
        <Text dimColor>Press <Text bold>Enter</Text> to save or <Text bold>ESC</Text> to skip</Text>
      </Box>
    </Box>
  );
};

interface IMDbFeatureHintProps {
  onDismiss: () => void;
}

export const IMDbFeatureHint: React.FC<IMDbFeatureHintProps> = ({ onDismiss }) => {
  return (
    <Box
      flexDirection="column"
      borderStyle="double"
      borderColor="green"
      paddingX={3}
      paddingY={1}
      width={60}
      backgroundColor="black"
    >
      <Box marginBottom={1}>
        <Text bold color="green">
          💡 Tip: IMDb Lookup Enabled
        </Text>
      </Box>

      <Box marginBottom={1}>
        <Text>
          Press <Text bold color="yellow">i</Text> on any show/movie to view IMDb info!
        </Text>
      </Box>

      <Box>
        <Text dimColor>Press any key to continue...</Text>
      </Box>
    </Box>
  );
};
