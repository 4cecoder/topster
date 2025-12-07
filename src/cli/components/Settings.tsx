import React, { useState } from 'react';
import { Box, Text } from 'ink';
import SelectInput from 'ink-select-input';
import TextInput from 'ink-text-input';
import type { TopsterConfig } from '../../core/config.js';

type SettingCategory = 'main' | 'player' | 'provider' | 'subtitles' | 'download' | 'features' | 'ui' | 'mcp';
type SettingKey = keyof TopsterConfig;

interface SettingsProps {
  config: TopsterConfig;
  onUpdate: (key: SettingKey, value: any) => void;
  onBack: () => void;
}

export const Settings: React.FC<SettingsProps> = ({ config, onUpdate, onBack }) => {
  const [category, setCategory] = useState<SettingCategory>('main');
  const [editingKey, setEditingKey] = useState<SettingKey | null>(null);
  const [inputValue, setInputValue] = useState('');

  if (editingKey) {
    return (
      <Box flexDirection="column">
        <Box marginBottom={1}>
          <Text bold color="cyan">
            Edit {editingKey}
          </Text>
        </Box>
        <Box>
          <Text>Enter value: </Text>
          <TextInput
            value={inputValue}
            onChange={setInputValue}
            onSubmit={() => {
              // Parse value based on type
              const currentValue = config[editingKey];
              let newValue: any = inputValue;

              if (typeof currentValue === 'number') {
                newValue = parseInt(inputValue) || 0;
              } else if (typeof currentValue === 'boolean') {
                newValue = inputValue.toLowerCase() === 'true';
              }

              onUpdate(editingKey, newValue);
              setEditingKey(null);
              setInputValue('');
            }}
          />
        </Box>
        <Box marginTop={1}>
          <Text dimColor>Press Esc to cancel</Text>
        </Box>
      </Box>
    );
  }

  if (category === 'main') {
    const categories = [
      { label: '🎬 Player Settings', value: 'player' },
      { label: '🌐 Provider Settings', value: 'provider' },
      { label: '💬 Subtitle Settings', value: 'subtitles' },
      { label: '📥 Download Settings', value: 'download' },
      { label: '⚡ Features', value: 'features' },
      { label: '🎨 UI Settings', value: 'ui' },
      { label: '🔌 MCP Settings', value: 'mcp' },
      { label: '← Back', value: 'back' },
    ];

    return (
      <Box flexDirection="column">
        <Box marginBottom={1}>
          <Text bold color="cyan">
            Settings
          </Text>
        </Box>
        <SelectInput
          items={categories}
          onSelect={(item) => {
            if (item.value === 'back') {
              onBack();
            } else {
              setCategory(item.value as SettingCategory);
            }
          }}
        />
      </Box>
    );
  }

  // Player Settings
  if (category === 'player') {
    const items = [
      {
        label: `Player: ${config.player}`,
        value: 'player',
        setting: {
          options: ['mpv', 'vlc', 'iina', 'internal'],
        },
      },
      {
        label: `Quality: ${config.quality}`,
        value: 'quality',
        setting: {
          options: ['480', '720', '1080', 'auto'],
        },
      },
      { label: '← Back', value: 'back' },
    ];

    return (
      <Box flexDirection="column">
        <Box marginBottom={1}>
          <Text bold color="cyan">
            Player Settings
          </Text>
        </Box>
        <SelectInput
          items={items.map(i => ({ label: i.label, value: i.value }))}
          onSelect={(item) => {
            if (item.value === 'back') {
              setCategory('main');
            } else {
              const setting = items.find(i => i.value === item.value)?.setting;
              if (setting?.options) {
                const currentValue = config[item.value as SettingKey];
                const currentIndex = setting.options.indexOf(currentValue as string);
                const nextIndex = (currentIndex + 1) % setting.options.length;
                onUpdate(item.value as SettingKey, setting.options[nextIndex]);
              }
            }
          }}
        />
      </Box>
    );
  }

  // Provider Settings
  if (category === 'provider') {
    const items = [
      {
        label: `Provider: ${config.provider}`,
        value: 'provider',
        setting: {
          options: ['Vidcloud', 'UpCloud'],
        },
      },
      {
        label: `Base URL: ${config.baseUrl}`,
        value: 'baseUrl',
        setting: { editable: true },
      },
      { label: '← Back', value: 'back' },
    ];

    return (
      <Box flexDirection="column">
        <Box marginBottom={1}>
          <Text bold color="cyan">
            Provider Settings
          </Text>
        </Box>
        <SelectInput
          items={items.map(i => ({ label: i.label, value: i.value }))}
          onSelect={(item) => {
            if (item.value === 'back') {
              setCategory('main');
            } else {
              const setting = items.find(i => i.value === item.value)?.setting;
              if (setting?.options) {
                const currentValue = config[item.value as SettingKey];
                const currentIndex = setting.options.indexOf(currentValue as string);
                const nextIndex = (currentIndex + 1) % setting.options.length;
                onUpdate(item.value as SettingKey, setting.options[nextIndex]);
              } else if (setting?.editable) {
                setEditingKey(item.value as SettingKey);
                setInputValue(String(config[item.value as SettingKey]));
              }
            }
          }}
        />
      </Box>
    );
  }

  // Subtitle Settings
  if (category === 'subtitles') {
    const items = [
      {
        label: `Subtitles: ${config.subtitlesEnabled ? 'ON' : 'OFF'}`,
        value: 'subtitlesEnabled',
        setting: { toggle: true },
      },
      {
        label: `Language: ${config.subsLanguage}`,
        value: 'subsLanguage',
        setting: {
          options: ['english', 'spanish', 'french', 'german', 'japanese', 'korean', 'chinese'],
        },
      },
      { label: '← Back', value: 'back' },
    ];

    return (
      <Box flexDirection="column">
        <Box marginBottom={1}>
          <Text bold color="cyan">
            Subtitle Settings
          </Text>
        </Box>
        <SelectInput
          items={items.map(i => ({ label: i.label, value: i.value }))}
          onSelect={(item) => {
            if (item.value === 'back') {
              setCategory('main');
            } else {
              const setting = items.find(i => i.value === item.value)?.setting;
              if (setting?.toggle) {
                onUpdate(item.value as SettingKey, !config[item.value as SettingKey]);
              } else if (setting?.options) {
                const currentValue = config[item.value as SettingKey];
                const currentIndex = setting.options.indexOf(currentValue as string);
                const nextIndex = (currentIndex + 1) % setting.options.length;
                onUpdate(item.value as SettingKey, setting.options[nextIndex]);
              }
            }
          }}
        />
      </Box>
    );
  }

  // Download Settings
  if (category === 'download') {
    const items = [
      {
        label: `Download Directory: ${config.downloadDir}`,
        value: 'downloadDir',
        setting: { editable: true },
      },
      { label: '← Back', value: 'back' },
    ];

    return (
      <Box flexDirection="column">
        <Box marginBottom={1}>
          <Text bold color="cyan">
            Download Settings
          </Text>
        </Box>
        <SelectInput
          items={items.map(i => ({ label: i.label, value: i.value }))}
          onSelect={(item) => {
            if (item.value === 'back') {
              setCategory('main');
            } else {
              setEditingKey(item.value as SettingKey);
              setInputValue(String(config[item.value as SettingKey]));
            }
          }}
        />
      </Box>
    );
  }

  // Features
  if (category === 'features') {
    const items = [
      {
        label: `Watch History: ${config.historyEnabled ? 'ON' : 'OFF'}`,
        value: 'historyEnabled',
        setting: { toggle: true },
      },
      {
        label: `Image Preview: ${config.imagePreview ? 'ON' : 'OFF'}`,
        value: 'imagePreview',
        setting: { toggle: true },
      },
      {
        label: `Discord Presence: ${config.discordPresence ? 'ON' : 'OFF'}`,
        value: 'discordPresence',
        setting: { toggle: true },
      },
      {
        label: `Debug Mode: ${config.debug ? 'ON' : 'OFF'}`,
        value: 'debug',
        setting: { toggle: true },
      },
      { label: '← Back', value: 'back' },
    ];

    return (
      <Box flexDirection="column">
        <Box marginBottom={1}>
          <Text bold color="cyan">
            Features
          </Text>
        </Box>
        <SelectInput
          items={items.map(i => ({ label: i.label, value: i.value }))}
          onSelect={(item) => {
            if (item.value === 'back') {
              setCategory('main');
            } else {
              onUpdate(item.value as SettingKey, !config[item.value as SettingKey]);
            }
          }}
        />
      </Box>
    );
  }

  // UI Settings
  if (category === 'ui') {
    const items = [
      {
        label: `External Menu: ${config.useExternalMenu ? 'ON' : 'OFF'}`,
        value: 'useExternalMenu',
        setting: { toggle: true },
      },
      {
        label: `Preview Window: ${config.previewWindowSize}`,
        value: 'previewWindowSize',
        setting: { editable: true },
      },
      { label: '← Back', value: 'back' },
    ];

    return (
      <Box flexDirection="column">
        <Box marginBottom={1}>
          <Text bold color="cyan">
            UI Settings
          </Text>
        </Box>
        <SelectInput
          items={items.map(i => ({ label: i.label, value: i.value }))}
          onSelect={(item) => {
            if (item.value === 'back') {
              setCategory('main');
            } else {
              const setting = items.find(i => i.value === item.value)?.setting;
              if (setting?.toggle) {
                onUpdate(item.value as SettingKey, !config[item.value as SettingKey]);
              } else if (setting?.editable) {
                setEditingKey(item.value as SettingKey);
                setInputValue(String(config[item.value as SettingKey]));
              }
            }
          }}
        />
      </Box>
    );
  }

  // MCP Settings
  if (category === 'mcp') {
    const items = [
      {
        label: `MCP Port: ${config.mcpPort}`,
        value: 'mcpPort',
        setting: { editable: true },
      },
      {
        label: `MCP Host: ${config.mcpHost}`,
        value: 'mcpHost',
        setting: { editable: true },
      },
      { label: '← Back', value: 'back' },
    ];

    return (
      <Box flexDirection="column">
        <Box marginBottom={1}>
          <Text bold color="cyan">
            MCP Settings
          </Text>
        </Box>
        <SelectInput
          items={items.map(i => ({ label: i.label, value: i.value }))}
          onSelect={(item) => {
            if (item.value === 'back') {
              setCategory('main');
            } else {
              setEditingKey(item.value as SettingKey);
              setInputValue(String(config[item.value as SettingKey]));
            }
          }}
        />
      </Box>
    );
  }

  return null;
};
