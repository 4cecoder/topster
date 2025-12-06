import React from 'react';
import { Box, Text } from 'ink';
import SelectInput from 'ink-select-input';

export interface MenuItem {
  label: string;
  value: string;
  description?: string;
}

interface MenuProps {
  title?: string;
  items: MenuItem[];
  onSelect: (value: string) => void;
}

export const Menu: React.FC<MenuProps> = ({ title, items, onSelect }) => {
  const selectItems = items.map((item) => ({
    label: item.description ? `${item.label} - ${item.description}` : item.label,
    value: item.value,
  }));

  const handleSelect = (selected: { value: string }) => {
    onSelect(selected.value);
  };

  return (
    <Box flexDirection="column">
      {title && (
        <Box marginBottom={1}>
          <Text bold color="cyan">
            {title}
          </Text>
        </Box>
      )}
      <SelectInput items={selectItems} onSelect={handleSelect} />
      <Box marginTop={1}>
        <Text dimColor>Use ↑↓ arrows to navigate, Enter to select</Text>
      </Box>
    </Box>
  );
};
