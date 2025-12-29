#!/usr/bin/env bun
import { FlixHQProvider } from './src/modules/scraper/flixhq';

async function testExtractors() {
  console.log('╔═══════════════════════════════════════════════════════════╗');
  console.log('║         TOPSTER TV - EXTRACTOR TEST (TypeScript)         ║');
  console.log('╚═══════════════════════════════════════════════════════════╝');
  console.log();

  const scraper = new FlixHQProvider();

  // Test 1: Search
  console.log('🔍 TEST 1: Searching for "The Matrix"...');
  try {
    const searchResult = await scraper.search('The Matrix', 1);
    console.log(`✅ Found ${searchResult.items.length} results`);

    if (searchResult.items.length === 0) {
      console.log('❌ ERROR: No results found');
      return;
    }

    const movie = searchResult.items[0];
    console.log(`📺 Selected: ${movie.title} (${movie.year}) - ID: ${movie.id}`);
    console.log();

    // Test 2: Get video sources
    console.log('🎬 TEST 2: Getting video sources...');
    const sources = await scraper.getVideoSources(movie.id, false);
    console.log(`✅ Found ${sources.length} video sources`);

    if (sources.length === 0) {
      console.log('❌ ERROR: No video sources found');
      return;
    }

    sources.forEach((source, index) => {
      console.log();
      console.log(`📡 Source ${index + 1}: ${source.server}`);
      console.log(`   Videos: ${source.sources.length}`);
      source.sources.forEach(video => {
        console.log(`   → URL: ${video.url.substring(0, 100)}...`);
        console.log(`   → Quality: ${video.quality || 'auto'}`);
        console.log(`   → Referer: ${video.referer || 'none'}`);
      });
    });

    console.log();
    console.log('✅ SUCCESS! Found working stream');
    console.log(`🎉 First stream URL: ${sources[0].sources[0].url}`);

  } catch (error) {
    console.log(`❌ ERROR: ${error}`);
    console.error(error);
  }

  console.log();
  console.log('═══════════════════════════════════════════════════════════');
}

testExtractors();
