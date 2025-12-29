import React from 'react';
import { Box, Text } from 'ink';
import type { MediaType } from '../../core/types.js';

export interface Filters {
  type?: MediaType;
  yearRange?: { min?: number; max?: number };
  quality?: string[];
  hasYear?: boolean;
}

interface FilterBarProps {
  filters: Filters;
  totalItems: number;
  filteredItems: number;
  onClear?: () => void;
}

export const FilterBar: React.FC<FilterBarProps> = ({ filters, totalItems, filteredItems, onClear }) => {
  const activeFilters: string[] = [];

  if (filters.type) {
    activeFilters.push(`Type: ${filters.type === 'movie' ? '🎬 Movies' : '📺 TV Shows'}`);
  }

  if (filters.yearRange?.min || filters.yearRange?.max) {
    const min = filters.yearRange.min || '?';
    const max = filters.yearRange.max || '?';
    activeFilters.push(`Year: ${min}-${max}`);
  }

  if (filters.quality && filters.quality.length > 0) {
    activeFilters.push(`Quality: ${filters.quality.join(', ')}`);
  }

  if (filters.hasYear !== undefined) {
    activeFilters.push(filters.hasYear ? 'Has Year Only' : 'No Year Only');
  }

  if (activeFilters.length === 0) {
    return null;
  }

  return (
    <Box flexDirection="column" marginBottom={1}>
      <Box borderStyle="single" borderColor="blue" paddingX={1}>
        <Text color="blue">🔍 Filters: </Text>
        <Text>{activeFilters.join(' • ')}</Text>
        <Text dimColor> ({filteredItems}/{totalItems} items)</Text>
        {onClear && (
          <Text dimColor> • Press F to clear</Text>
        )}
      </Box>
    </Box>
  );
};
