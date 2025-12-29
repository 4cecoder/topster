// IMDb/OMDb types
// Note: OMDb API returns PascalCase field names

export interface IMDbInfo {
  Title: string;
  Year: string;
  Rated: string;
  Released: string;
  Runtime: string;
  Genre: string;
  Director: string;
  Writer: string;
  Actors: string;
  Plot: string;
  Language: string;
  Country: string;
  Awards: string;
  Poster: string;
  Ratings: IMDbRating[];
  imdbRating: string;
  imdbVotes: string;
  imdbID: string;
  Type: string;
  totalSeasons?: string;
  Response: string;
}

export interface IMDbRating {
  Source: string;
  Value: string;
}

export interface IMDbSearchResult {
  Title: string;
  Year: string;
  imdbID: string;
  Type: string;
  Poster: string;
}

export interface IMDbError {
  Response: 'False';
  Error: string;
}
