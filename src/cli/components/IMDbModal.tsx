import React from 'react';
import { Box, Text } from 'ink';
import type { IMDbInfo } from '../../modules/imdb/types.js';

interface IMDbModalProps {
  info: IMDbInfo;
  onClose: () => void;
}

export const IMDbModal: React.FC<IMDbModalProps> = ({ info, onClose }) => {
  return (
    <Box
      flexDirection="column"
      borderStyle="double"
      borderColor="yellow"
      paddingX={3}
      paddingY={1}
      width={80}
      backgroundColor="black"
    >
      <Box marginBottom={1} borderStyle="single" borderColor="yellow" paddingX={1}>
        <Text bold backgroundColor="yellow" color="black">
          {' '}📽️  IMDb INFO{' '}
        </Text>
      </Box>

      <Box marginBottom={1}>
        <Text bold color="cyan">
          {info.Title} ({info.Year})
        </Text>
      </Box>

      {info.imdbRating !== 'N/A' && (
        <Box marginBottom={1}>
          <Text color="yellow">⭐ IMDb: {info.imdbRating}/10</Text>
          <Text dimColor> ({info.imdbVotes} votes)</Text>
        </Box>
      )}

      {info.Ratings && info.Ratings.length > 0 && (
        <Box flexDirection="column" marginBottom={1}>
          {info.Ratings.map((rating, idx) => (
            <Text key={idx} dimColor>
              {rating.Source}: {rating.Value}
            </Text>
          ))}
        </Box>
      )}

      <Box marginBottom={1}>
        <Text>
          <Text color="green">Type:</Text> {info.Type === 'movie' ? '🎬 Movie' : '📺 TV Series'}
          {info.totalSeasons && ` (${info.totalSeasons} seasons)`}
        </Text>
      </Box>

      {info.Rated !== 'N/A' && (
        <Box marginBottom={1}>
          <Text>
            <Text color="green">Rated:</Text> {info.Rated}
          </Text>
        </Box>
      )}

      {info.Runtime !== 'N/A' && (
        <Box marginBottom={1}>
          <Text>
            <Text color="green">Runtime:</Text> {info.Runtime}
          </Text>
        </Box>
      )}

      {info.Genre !== 'N/A' && (
        <Box marginBottom={1}>
          <Text>
            <Text color="green">Genre:</Text> {info.Genre}
          </Text>
        </Box>
      )}

      {info.Director !== 'N/A' && (
        <Box marginBottom={1}>
          <Text>
            <Text color="green">Director:</Text> {info.Director}
          </Text>
        </Box>
      )}

      {info.Actors !== 'N/A' && (
        <Box marginBottom={1}>
          <Text>
            <Text color="green">Cast:</Text> {info.Actors}
          </Text>
        </Box>
      )}

      {info.Plot !== 'N/A' && (
        <Box marginBottom={1} flexDirection="column">
          <Text color="green">Plot:</Text>
          <Text>{info.Plot}</Text>
        </Box>
      )}

      {info.Awards !== 'N/A' && (
        <Box marginBottom={1}>
          <Text>
            <Text color="green">Awards:</Text> {info.Awards}
          </Text>
        </Box>
      )}

      <Box marginTop={1}>
        <Text dimColor>Press ESC to close • IMDb ID: {info.imdbID}</Text>
      </Box>
    </Box>
  );
};
