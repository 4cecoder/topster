package com.topster.tv

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.gson.Gson
import com.topster.tv.scraper.FlixHQScraper
import com.topster.tv.scraper.extractors.*
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ExtractorTest {

    companion object {
        private const val TAG = "ExtractorTest"
    }

    @Test
    fun testMovieExtraction() = runBlocking {
        Log.d(TAG, "╔═══════════════════════════════════════════════════════════╗")
        Log.d(TAG, "║         TOPSTER TV - EXTRACTOR TEST (KOTLIN)              ║")
        Log.d(TAG, "╚═══════════════════════════════════════════════════════════╝")

        // Setup
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = Gson()

        val megaCloudExtractor = MegaCloudExtractor(client, gson)
        val vidCloudExtractor = VidCloudExtractor(client, gson)
        val rapidCloudExtractor = RapidCloudExtractor(client, gson)
        val streamSBExtractor = StreamSBExtractor(client)

        val scraper = FlixHQScraper(
            client = client,
            gson = gson,
            megaCloudExtractor = megaCloudExtractor,
            vidCloudExtractor = vidCloudExtractor,
            rapidCloudExtractor = rapidCloudExtractor,
            streamSBExtractor = streamSBExtractor
        )

        // Test 1: Search
        Log.d(TAG, "")
        Log.d(TAG, "🔍 TEST 1: Searching for 'The Matrix'...")
        try {
            val searchResults = scraper.search("The Matrix", page = 1)
            Log.d(TAG, "✅ Found ${searchResults.size} results")

            if (searchResults.isEmpty()) {
                Log.e(TAG, "❌ ERROR: No results found")
                assert(false) { "No search results" }
            }

            val movie = searchResults.first()
            Log.d(TAG, "📺 Selected: ${movie.title} (${movie.year}) - ID: ${movie.id}")
            Log.d(TAG, "   Type: ${movie.type}")
            Log.d(TAG, "   URL: ${movie.url}")

            // Test 2: Get video sources
            Log.d(TAG, "")
            Log.d(TAG, "🎬 TEST 2: Getting video sources for movie...")
            try {
                val sources = scraper.getVideoSources(movie.id, isEpisode = false)
                Log.d(TAG, "✅ Found ${sources.size} video sources")

                if (sources.isEmpty()) {
                    Log.e(TAG, "❌ ERROR: No video sources found")
                    assert(false) { "No video sources" }
                }

                sources.forEachIndexed { index, source ->
                    Log.d(TAG, "")
                    Log.d(TAG, "📡 Source ${index + 1}: ${source.provider}")
                    Log.d(TAG, "   Videos: ${source.sources.size}")
                    source.sources.forEach { video ->
                        Log.d(TAG, "   → URL: ${video.url.take(100)}...")
                        Log.d(TAG, "   → Quality: ${video.quality}")
                        Log.d(TAG, "   → Referer: ${video.referer}")
                        Log.d(TAG, "   → Subtitles: ${video.subtitles.size}")

                        // Verify it's an M3U8 stream
                        assert(video.url.contains(".m3u8")) {
                            "Video URL is not M3U8: ${video.url}"
                        }
                    }
                }

                Log.d(TAG, "")
                Log.d(TAG, "✅ SUCCESS! Found working M3U8 stream")
                Log.d(TAG, "🎉 First stream URL: ${sources.first().sources.first().url}")

            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR getting video sources: ${e.message}", e)
                throw e
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR searching: ${e.message}", e)
            throw e
        }

        Log.d(TAG, "")
        Log.d(TAG, "═══════════════════════════════════════════════════════════")
    }

    @Test
    fun testTVShowExtraction() = runBlocking {
        Log.d(TAG, "")
        Log.d(TAG, "🔍 TEST 3: Searching for 'Breaking Bad'...")

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = Gson()
        val megaCloudExtractor = MegaCloudExtractor(client, gson)
        val vidCloudExtractor = VidCloudExtractor(client, gson)
        val rapidCloudExtractor = RapidCloudExtractor(client, gson)
        val streamSBExtractor = StreamSBExtractor(client)

        val scraper = FlixHQScraper(
            client = client,
            gson = gson,
            megaCloudExtractor = megaCloudExtractor,
            vidCloudExtractor = vidCloudExtractor,
            rapidCloudExtractor = rapidCloudExtractor,
            streamSBExtractor = streamSBExtractor
        )

        try {
            val searchResults = scraper.search("Breaking Bad", page = 1)
            Log.d(TAG, "✅ Found ${searchResults.size} results")

            val show = searchResults.firstOrNull { it.type == "tv" }
            if (show == null) {
                Log.e(TAG, "❌ ERROR: No TV shows found")
                assert(false) { "No TV shows found" }
            }

            Log.d(TAG, "📺 Selected: ${show!!.title} (${show.year})")

            // Get seasons
            Log.d(TAG, "")
            Log.d(TAG, "📺 Getting seasons...")
            val seasons = scraper.getSeasons(show.id)
            Log.d(TAG, "✅ Found ${seasons.size} seasons")

            if (seasons.isEmpty()) {
                Log.e(TAG, "❌ ERROR: No seasons found")
                assert(false) { "No seasons" }
            }

            // Get episodes from first season
            val season = seasons.first()
            Log.d(TAG, "")
            Log.d(TAG, "📺 Getting episodes for ${season.title}...")
            val episodes = scraper.getEpisodes(season.id)
            Log.d(TAG, "✅ Found ${episodes.size} episodes")

            if (episodes.isEmpty()) {
                Log.e(TAG, "❌ ERROR: No episodes found")
                assert(false) { "No episodes" }
            }

            // Get video sources for first episode
            val episode = episodes.first()
            Log.d(TAG, "")
            Log.d(TAG, "🎬 Getting video sources for ${episode.title}...")
            val sources = scraper.getVideoSources(episode.id, isEpisode = true)
            Log.d(TAG, "✅ Found ${sources.size} video sources")

            if (sources.isEmpty()) {
                Log.e(TAG, "❌ ERROR: No video sources found for episode")
                assert(false) { "No video sources for episode" }
            }

            sources.forEach { source ->
                Log.d(TAG, "📡 Source: ${source.provider}")
                source.sources.forEach { video ->
                    Log.d(TAG, "   → ${video.url.take(100)}...")
                }
            }

            Log.d(TAG, "✅ SUCCESS! TV show extraction working")

        } catch (e: Exception) {
            Log.e(TAG, "❌ ERROR: ${e.message}", e)
            throw e
        }
    }
}
