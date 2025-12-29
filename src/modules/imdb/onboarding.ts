import { getConfig } from '../../core/config.js';
import { existsSync, readFileSync, writeFileSync } from 'fs';
import { join } from 'path';

interface OnboardingState {
  imdbOnboardingCompleted: boolean;
  imdbFeatureShown: boolean;
}

let onboardingState: OnboardingState = {
  imdbOnboardingCompleted: false,
  imdbFeatureShown: false,
};

let ONBOARDING_FILE_PATH: string;

/**
 * Load onboarding state from disk
 */
function loadOnboardingState(): void {
  try {
    const config = getConfig();
    ONBOARDING_FILE_PATH = join(config.getDataDir(), 'imdb-onboarding.json');

    if (existsSync(ONBOARDING_FILE_PATH)) {
      const data = readFileSync(ONBOARDING_FILE_PATH, 'utf-8');
      onboardingState = JSON.parse(data);
    }
  } catch (error) {
    // Ignore errors - will use defaults
  }
}

/**
 * Save onboarding state to disk
 */
function saveOnboardingState(): void {
  try {
    if (!ONBOARDING_FILE_PATH) {
      const config = getConfig();
      ONBOARDING_FILE_PATH = join(config.getDataDir(), 'imdb-onboarding.json');
    }

    writeFileSync(ONBOARDING_FILE_PATH, JSON.stringify(onboardingState, null, 2));
  } catch (error) {
    // Ignore save errors
  }
}

// Load state on module initialization
loadOnboardingState();

/**
 * Check if user needs IMDb onboarding
 */
export function needsIMDbOnboarding(): boolean {
  const config = getConfig();
  const hasApiKey = !!config.get('omdbApiKey');

  // Need onboarding if: no API key AND haven't completed onboarding
  return !hasApiKey && !onboardingState.imdbOnboardingCompleted;
}

/**
 * Check if we should show the IMDb feature hint
 */
export function shouldShowIMDbHint(): boolean {
  const config = getConfig();
  const hasApiKey = !!config.get('omdbApiKey');

  // Show hint if: has API key AND haven't shown the feature yet
  return hasApiKey && !onboardingState.imdbFeatureShown;
}

/**
 * Mark onboarding as completed
 */
export function completeIMDbOnboarding(): void {
  onboardingState.imdbOnboardingCompleted = true;
  saveOnboardingState();
}

/**
 * Mark feature hint as shown
 */
export function markIMDbFeatureShown(): void {
  onboardingState.imdbFeatureShown = true;
  saveOnboardingState();
}

/**
 * Reset onboarding (for testing)
 */
export function resetIMDbOnboarding(): void {
  onboardingState = {
    imdbOnboardingCompleted: false,
    imdbFeatureShown: false,
  };
  saveOnboardingState();
}
