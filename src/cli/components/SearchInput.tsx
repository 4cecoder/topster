import React, { useState } from 'react';
import { Box, Text, useInput } from 'ink';
import TextInput from 'ink-text-input';

interface SearchInputProps {
  placeholder?: string;
  onSubmit: (query: string) => void;
  onCancel?: () => void;
}

export const SearchInput: React.FC<SearchInputProps> = ({
  placeholder = 'Search...',
  onSubmit,
  onCancel,
}) => {
  const [query, setQuery] = useState('');

  // Handle ESC key for back navigation
  useInput((input, key) => {
    if (key.escape && onCancel) {
      onCancel();
    }
  });

  const handleSubmit = () => {
    if (query.trim()) {
      onSubmit(query.trim());
    } else if (onCancel) {
      onCancel();
    }
  };

  return (
    <Box flexDirection="column" marginBottom={1}>
      <Box marginBottom={1}>
        <Text bold color="cyan">
          🔍 Search
        </Text>
      </Box>
      <Box>
        <Text color="gray">▶ </Text>
        <TextInput
          value={query}
          onChange={setQuery}
          onSubmit={handleSubmit}
          placeholder={placeholder}
        />
      </Box>
      <Box marginTop={1}>
        <Text dimColor>Press Enter to search, ESC to go back</Text>
      </Box>
    </Box>
  );
};
