// Scraper module exports

export * from './http';
export * from './flixhq';

import type { Provider } from '../../core/types';
import { createFlixHQProvider } from './flixhq';
import { getConfig } from '../../core/config';

export type ProviderName = 'flixhq';

const providerFactories: Record<ProviderName, (baseUrl?: string) => Provider> = {
  flixhq: createFlixHQProvider,
};

export function createProvider(name: ProviderName = 'flixhq'): Provider {
  const factory = providerFactories[name];
  if (!factory) {
    throw new Error(`Unknown provider: ${name}`);
  }
  return factory(getConfig().get('baseUrl'));
}

export function getDefaultProvider(): Provider {
  return createProvider('flixhq');
}
