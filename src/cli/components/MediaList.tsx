import React from 'react';
import { Box, Text, useInput } from 'ink';
import SelectInput from 'ink-select-input';
import type { MediaItem } from '../../core/types.js';
import { FilterBar, type Filters } from './FilterBar.js';

interface MediaListProps {
  items: MediaItem[];
  title?: string;
  onSelect: (item: MediaItem) => void;
  onCancel?: () => void;
  currentPage?: number;
  totalPages?: number;
  onNextPage?: () => void;
  onPrevPage?: () => void;
  onShowInfo?: (item: MediaItem) => void;
  filters?: Filters;
  onClearFilters?: () => void;
  onToggleFilter?: (filterType: string) => void;
}

type SelectValue = MediaItem | 'next' | 'prev' | null;

export const MediaList: React.FC<MediaListProps> = ({
  items,
  title,
  onSelect,
  onCancel,
  currentPage,
  totalPages,
  onNextPage,
  onPrevPage,
  onShowInfo,
  filters,
  onClearFilters,
  onToggleFilter,
}) => {
  const [selectedIndex, setSelectedIndex] = React.useState(0);
  const [allItems] = React.useState(items);

  // Apply filters
  const filteredItems = React.useMemo(() => {
    if (!filters) return items;

    return items.filter(item => {
      // Type filter
      if (filters.type && item.type !== filters.type) {
        return false;
      }

      // Year range filter
      if (filters.yearRange) {
        const itemYear = item.year ? parseInt(item.year) : null;
        if (itemYear) {
          if (filters.yearRange.min && itemYear < filters.yearRange.min) return false;
          if (filters.yearRange.max && itemYear > filters.yearRange.max) return false;
        } else if (filters.hasYear !== false) {
          return false;
        }
      }

      // Quality filter
      if (filters.quality && filters.quality.length > 0 && item.quality) {
        if (!filters.quality.includes(item.quality)) {
          return false;
        }
      }

      // Has year filter
      if (filters.hasYear !== undefined) {
        const hasYear = !!item.year;
        if (hasYear !== filters.hasYear) {
          return false;
        }
      }

      return true;
    });
  }, [items, filters]);

  // Handle 'i' key to show IMDb info, 'f' to clear filters, 'm'/'t' to toggle filters
  useInput((input, key) => {
    if (input === 'i' && onShowInfo && filteredItems.length > 0) {
      // Find the currently highlighted item
      const currentItem = filteredItems[selectedIndex];
      if (currentItem) {
        onShowInfo(currentItem);
      }
    } else if (input === 'f' && onClearFilters && filters) {
      onClearFilters();
    } else if (input === 'm' && onToggleFilter) {
      onToggleFilter('movies');
    } else if (input === 't' && onToggleFilter) {
      onToggleFilter('tv');
    } else if (input === 'h' && onToggleFilter) {
      onToggleFilter('hd');
    }
  });
  // Enhanced formatting for list items
  const formatMediaItem = (item: MediaItem, index: number): string => {
    const icon = item.type === 'movie' ? '🎬' : '📺';
    const year = item.year ? `(${item.year})` : '';
    const quality = item.quality ? `[${item.quality}]` : '';
    const duration = item.duration ? `⏱ ${item.duration}` : '';

    // Build parts array
    const parts = [icon, item.title];
    if (year) parts.push(year);
    if (quality) parts.push(quality);
    if (duration) parts.push(duration);

    return parts.join(' ');
  };

  const selectItems = filteredItems.map((item, index) => ({
    label: formatMediaItem(item, index),
    value: item as SelectValue,
    key: `${item.id}-${index}`,
  }));

  // Add pagination controls if applicable
  if (currentPage && totalPages && totalPages > 1) {
    if (currentPage < totalPages && onNextPage) {
      selectItems.push({
        label: `→ Next Page (${currentPage + 1}/${totalPages})`,
        value: 'next' as SelectValue,
        key: 'next-page',
      });
    }
    if (currentPage > 1 && onPrevPage) {
      selectItems.push({
        label: `← Previous Page (${currentPage - 1}/${totalPages})`,
        value: 'prev' as SelectValue,
        key: 'prev-page',
      });
    }
  }

  // Add cancel option
  if (onCancel) {
    selectItems.push({
      label: '← Back',
      value: null as SelectValue,
      key: 'cancel',
    });
  }

  const handleSelect = (selected: { value: SelectValue }) => {
    if (selected.value === 'next' && onNextPage) {
      onNextPage();
    } else if (selected.value === 'prev' && onPrevPage) {
      onPrevPage();
    } else if (selected.value === null && onCancel) {
      onCancel();
    } else if (selected.value && typeof selected.value !== 'string') {
      onSelect(selected.value);
    }
  };

  const handleHighlight = (item: { value: SelectValue }) => {
    // Track which item is currently highlighted
    const index = filteredItems.findIndex(i => i === item.value);
    if (index !== -1) {
      setSelectedIndex(index);
    }
  };

  return (
    <Box flexDirection="column">
      {title && (
        <Box marginBottom={1}>
          <Text bold color="cyan">
            {title}
            {currentPage && totalPages && totalPages > 1 && (
              <Text dimColor> (Page {currentPage}/{totalPages})</Text>
            )}
          </Text>
        </Box>
      )}

      {/* Show filter bar if filters are active */}
      {filters && (
        <FilterBar
          filters={filters}
          totalItems={items.length}
          filteredItems={filteredItems.length}
          onClear={onClearFilters}
        />
      )}

      {filteredItems.length === 0 ? (
        <Box flexDirection="column">
          <Text dimColor>No items match the current filters</Text>
          {onClearFilters && (
            <Text dimColor>Press 'f' to clear filters</Text>
          )}
        </Box>
      ) : (
        <>
          <SelectInput items={selectItems} onSelect={handleSelect} onHighlight={handleHighlight} />
          <Box marginTop={1} flexDirection="column">
            {onShowInfo && <Text dimColor>• Press 'i' for IMDb info</Text>}
            {onToggleFilter && (
              <>
                <Text dimColor>• Press 'm' for Movies only, 't' for TV only</Text>
                <Text dimColor>• Press 'h' for HD only, 'f' to clear filters</Text>
              </>
            )}
          </Box>
        </>
      )}
    </Box>
  );
};
