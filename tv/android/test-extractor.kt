import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import com.google.gson.Gson
import com.topster.tv.scraper.FlixHQScraper
import com.topster.tv.scraper.extractors.*
import java.util.concurrent.TimeUnit

fun main() = runBlocking {
    println("╔═══════════════════════════════════════════════════════════╗")
    println("║         TOPSTER TV - EXTRACTOR TEST SCRIPT               ║")
    println("╚═══════════════════════════════════════════════════════════╝")
    println()

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

    // Test 1: Search for a popular movie
    println("🔍 TEST 1: Searching for 'The Matrix'...")
    try {
        val searchResults = scraper.search("The Matrix", page = 1)
        println("✅ Found ${searchResults.size} results")

        if (searchResults.isEmpty()) {
            println("❌ ERROR: No results found")
            return@runBlocking
        }

        val movie = searchResults.first()
        println("📺 Selected: ${movie.title} (${movie.year}) - ID: ${movie.id}")
        println()

        // Test 2: Get video sources
        println("🎬 TEST 2: Getting video sources for movie...")
        try {
            val sources = scraper.getVideoSources(movie.id, isEpisode = false)
            println("✅ Found ${sources.size} video sources")

            if (sources.isEmpty()) {
                println("❌ ERROR: No video sources found")
                return@runBlocking
            }

            sources.forEachIndexed { index, source ->
                println()
                println("📡 Source ${index + 1}: ${source.provider}")
                println("   Videos: ${source.sources.size}")
                source.sources.forEach { video ->
                    println("   → URL: ${video.url.take(100)}...")
                    println("   → Quality: ${video.quality}")
                    println("   → Referer: ${video.referer}")
                    println("   → Subtitles: ${video.subtitles.size}")
                }
            }

            println()
            println("✅ SUCCESS! Found working M3U8 stream")
            println("🎉 First stream URL: ${sources.first().sources.first().url}")

        } catch (e: Exception) {
            println("❌ ERROR getting video sources: ${e.message}")
            e.printStackTrace()
        }

    } catch (e: Exception) {
        println("❌ ERROR searching: ${e.message}")
        e.printStackTrace()
    }

    println()
    println("═══════════════════════════════════════════════════════════")
}
