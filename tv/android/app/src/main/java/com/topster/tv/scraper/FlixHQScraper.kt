package com.topster.tv.scraper

import android.util.Log
import com.google.gson.Gson
import com.topster.tv.api.models.*
import com.topster.tv.scraper.extractors.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

class FlixHQScraper(
    private val baseUrl: String = "https://flixhq.to",
    private val cacheManager: CacheManager = CacheManager()
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val gson = Gson()
    private val tag = "FlixHQScraper"

    // Initialize extractors
    private val megaCloudExtractor = MegaCloudExtractor(client, gson)
    private val vidCloudExtractor = VidCloudExtractor(client, gson)
    private val rapidCloudExtractor = RapidCloudExtractor(client, gson)
    private val streamSBExtractor = StreamSBExtractor(client, gson)

    /**
     * Search for movies and TV shows
     */
    suspend fun search(query: String, page: Int = 1): List<MediaItem> = withContext(Dispatchers.IO) {
        // Check cache first
        cacheManager.getCachedSearch(query, page)?.let { cached ->
            Log.d(tag, "Returning cached search results for: $query")
            return@withContext cached
        }

        Log.d(tag, "Searching for: $query (page $page)")

        val searchQuery = query.trim().replace(Regex("\\s+"), "-")
        val url = "$baseUrl/search/$searchQuery?page=$page"

        val html = fetchHtml(url)
        val doc = Jsoup.parse(html)

        val items = mutableListOf<MediaItem>()

        doc.select(".flw-item").forEach { element ->
            parseMediaItem(element)?.let { items.add(it) }
        }

        Log.d(tag, "Found ${items.size} results")

        // Cache the results
        cacheManager.cacheSearch(query, page, items)

        items
    }

    /**
     * Get trending content
     */
    suspend fun getTrending(): List<MediaItem> = withContext(Dispatchers.IO) {
        // Check cache first
        cacheManager.getCachedTrending()?.let { cached ->
            Log.d(tag, "Returning cached trending content")
            return@withContext cached
        }

        Log.d(tag, "Fetching trending content")

        val url = "$baseUrl/home"
        val html = fetchHtml(url)
        val doc = Jsoup.parse(html)

        val items = mutableListOf<MediaItem>()

        doc.select("#trending-movies .flw-item, #trending-tv .flw-item").forEach { element ->
            parseMediaItem(element)?.let { items.add(it) }
        }

        Log.d(tag, "Found ${items.size} trending items")

        // Cache the results
        cacheManager.cacheTrending(items)

        items
    }

    /**
     * Get recent content by type
     */
    suspend fun getRecent(type: String): List<MediaItem> = withContext(Dispatchers.IO) {
        // Check cache first
        cacheManager.getCachedRecent(type)?.let { cached ->
            Log.d(tag, "Returning cached recent $type content")
            return@withContext cached
        }

        Log.d(tag, "Fetching recent $type content")

        val url = when (type.lowercase()) {
            "movie" -> "$baseUrl/movie"
            "tv" -> "$baseUrl/tv-show"
            else -> "$baseUrl/home"
        }

        val html = fetchHtml(url)
        val doc = Jsoup.parse(html)

        val items = mutableListOf<MediaItem>()

        doc.select(".flw-item").forEach { element ->
            parseMediaItem(element)?.let { items.add(it) }
        }

        Log.d(tag, "Found ${items.size} recent $type items")

        // Cache the results
        cacheManager.cacheRecent(type, items)

        items
    }

    /**
     * Get seasons for a TV show
     */
    suspend fun getSeasons(mediaId: String): List<Season> = withContext(Dispatchers.IO) {
        Log.d(tag, "Fetching seasons for: $mediaId")

        val url = "$baseUrl/ajax/v2/tv/seasons/$mediaId"
        val html = fetchHtml(url)
        val doc = Jsoup.parse(html)

        val seasons = mutableListOf<Season>()

        doc.select(".dropdown-menu a").forEach { element ->
            val seasonId = element.attr("data-id")
            val seasonText = element.text().trim()
            val seasonNumber = seasonText.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

            if (seasonId.isNotEmpty()) {
                seasons.add(
                    Season(
                        id = seasonId,
                        number = seasonNumber,
                        title = seasonText
                    )
                )
            }
        }

        Log.d(tag, "Found ${seasons.size} seasons")
        seasons
    }

    /**
     * Get episodes for a season
     */
    suspend fun getEpisodes(seasonId: String): List<Episode> = withContext(Dispatchers.IO) {
        Log.d(tag, "Fetching episodes for season: $seasonId")

        val url = "$baseUrl/ajax/v2/season/episodes/$seasonId"
        val html = fetchHtml(url)
        val doc = Jsoup.parse(html)

        val episodes = mutableListOf<Episode>()

        doc.select(".eps-item").forEach { element ->
            val episodeId = element.attr("data-id")
            val episodeTitle = element.attr("title")
            val episodeNumber = element.select(".episode-number").text()
                .replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0

            if (episodeId.isNotEmpty()) {
                episodes.add(
                    Episode(
                        id = episodeId,
                        number = episodeNumber,
                        title = episodeTitle.ifEmpty { "Episode $episodeNumber" },
                        url = null
                    )
                )
            }
        }

        Log.d(tag, "Found ${episodes.size} episodes")
        episodes
    }

    /**
     * Get video sources for streaming
     */
    suspend fun getVideoSources(id: String, isEpisode: Boolean): List<VideoSource> = withContext(Dispatchers.IO) {
        Log.d(tag, "================================================")
        Log.d(tag, "Fetching video sources for: $id (isEpisode: $isEpisode)")

        // Build correct URL based on type
        val serversUrl = if (isEpisode) {
            "$baseUrl/ajax/v2/episode/servers/$id"
        } else {
            // For movies, we need to get the episode ID first
            "$baseUrl/ajax/movie/episodes/$id"
        }
        Log.d(tag, "Servers URL: $serversUrl")

        // Fetch with proper AJAX headers
        val request = Request.Builder()
            .url(serversUrl)
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .addHeader("Referer", baseUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.message}")
        }
        val html = response.body?.string() ?: throw Exception("Empty response body")
        val doc = Jsoup.parse(html)

        val sources = mutableListOf<VideoSource>()

        // Different selectors for movies vs episodes
        val serverElements = doc.select("a.link-item")
        Log.d(tag, "Found ${serverElements.size} server items in HTML")

        // Get available servers
        serverElements.forEachIndexed { index, serverElement ->
            val serverId: String
            val serverName: String

            if (isEpisode) {
                // For episodes, use data-id
                serverId = serverElement.attr("data-id")
                // Title is "Server Vidcloud", extract just the server name
                val rawTitle = serverElement.attr("title")
                serverName = rawTitle.replace(Regex("^Server\\s*", RegexOption.IGNORE_CASE), "").ifEmpty {
                    serverElement.selectFirst("span")?.text()?.trim() ?: ""
                }
            } else {
                // For movies, extract episode_id from href
                val href = serverElement.attr("href")
                serverName = serverElement.attr("title").ifEmpty {
                    serverElement.selectFirst("span")?.text()?.trim() ?: ""
                }
                // Extract episode_id from href (the number after the dot)
                val match = Regex("\\.([0-9]+)$").find(href)
                serverId = match?.groupValues?.get(1) ?: ""
            }

            Log.d(tag, "Server ${index + 1}/${serverElements.size}: $serverName (ID: $serverId)")

            if (serverId.isNotEmpty()) {
                try {
                    val videoInfo = extractVideoSource(serverId, serverName)
                    if (videoInfo != null) {
                        Log.d(tag, "✅ $serverName extraction SUCCESS")
                        sources.add(
                            VideoSource(
                                provider = serverName,
                                sources = listOf(videoInfo)
                            )
                        )
                    } else {
                        Log.w(tag, "⚠️ $serverName returned null")
                    }
                } catch (e: Exception) {
                    Log.e(tag, "❌ $serverName extraction FAILED: ${e.message}", e)
                }
            }
        }

        Log.d(tag, "Final result: ${sources.size} working sources out of ${serverElements.size} servers")
        Log.d(tag, "================================================")
        sources
    }

    /**
     * Extract video URL from a server
     */
    private suspend fun extractVideoSource(serverId: String, serverName: String): VideoInfo? {
        Log.d(tag, "  → Extracting from server: $serverName (ID: $serverId)")

        // Get the embed URL - correct endpoint is /ajax/episode/sources/{id}
        val embedUrl = "$baseUrl/ajax/episode/sources/$serverId"
        Log.d(tag, "  → Getting embed link from: $embedUrl")

        val request = Request.Builder()
            .url(embedUrl)
            .addHeader("X-Requested-With", "XMLHttpRequest")
            .addHeader("Referer", baseUrl)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .build()

        val response = client.newCall(request).execute()
        val embedHtml = response.body?.string() ?: throw Exception("Empty response")
        Log.d(tag, "  → Response: ${embedHtml.take(200)}")

        // Parse JSON response
        val linkMatch = Regex("\"link\":\\s*\"([^\"]+)\"").find(embedHtml)
        val link = linkMatch?.groupValues?.get(1)?.replace("\\", "")

        if (link == null) {
            Log.e(tag, "  → ERROR: Could not extract link from response")
            return null
        }

        Log.d(tag, "  → Embed link extracted: $link")

        // Extract video based on server type
        return try {
            val extractorName: String
            val extractor: VideoExtractor = when {
                serverName.contains("MegaCloud", ignoreCase = true) ||
                link.contains("megacloud.tv") -> {
                    extractorName = "MegaCloudExtractor"
                    megaCloudExtractor
                }

                serverName.contains("VidCloud", ignoreCase = true) ||
                serverName.contains("UpCloud", ignoreCase = true) ||
                link.contains("vidcloud") ||
                link.contains("upcloud") -> {
                    extractorName = "VidCloudExtractor"
                    vidCloudExtractor
                }

                serverName.contains("RapidCloud", ignoreCase = true) ||
                link.contains("rapid-cloud.co") ||
                link.contains("rabbitstream.net") -> {
                    extractorName = "RapidCloudExtractor"
                    rapidCloudExtractor
                }

                serverName.contains("StreamSB", ignoreCase = true) ||
                link.contains("streamsb") ||
                link.contains("watchsb") ||
                link.contains("sbplay") -> {
                    extractorName = "StreamSBExtractor"
                    streamSBExtractor
                }

                else -> {
                    extractorName = "MegaCloudExtractor (fallback)"
                    Log.w(tag, "  → Unknown server type: $serverName, using MegaCloud extractor as fallback")
                    megaCloudExtractor
                }
            }

            Log.d(tag, "  → Using extractor: $extractorName")
            val videoInfoList = extractor.extract(link, baseUrl)
            Log.d(tag, "  → Extractor returned ${videoInfoList.size} video sources")

            videoInfoList.firstOrNull()
        } catch (e: Exception) {
            Log.e(tag, "  → Extraction FAILED: ${e.message}", e)
            null
        }
    }

    /**
     * Parse a media item from HTML element
     */
    private fun parseMediaItem(element: Element): MediaItem? {
        try {
            val linkElement = element.selectFirst(".film-poster-ahref") ?: return null
            val detailElement = element.selectFirst(".film-detail")

            val url = linkElement.attr("href")
            val title = linkElement.attr("title").ifEmpty {
                detailElement?.selectFirst(".film-name a")?.text()?.trim() ?: ""
            }

            val imageElement = element.selectFirst(".film-poster-img")
            val image = imageElement?.attr("data-src")?.ifEmpty {
                imageElement.attr("src")
            }

            val id = extractId(url)
            val type = if (url.contains("/tv/")) "tv" else "movie"

            // Extract quality and duration
            val fdiItems = element.select(".fdi-item")
            var quality: String? = null
            var duration: String? = null
            var year: String? = null

            fdiItems.forEachIndexed { index, item ->
                val text = item.text().trim()
                when {
                    text.matches(Regex("\\d+m")) -> duration = text
                    text.matches(Regex("(HD|SD|CAM|4K)")) -> quality = text
                    index == 0 && text.matches(Regex("\\d{4}")) -> year = text
                }
            }

            if (id.isEmpty() || title.isEmpty()) return null

            return MediaItem(
                id = id,
                title = title,
                type = type,
                year = year,
                quality = quality,
                duration = duration,
                image = image,
                url = "$baseUrl$url"
            )
        } catch (e: Exception) {
            Log.e(tag, "Error parsing media item: ${e.message}")
            return null
        }
    }

    /**
     * Extract ID from URL
     */
    private fun extractId(url: String): String {
        return url.substringAfterLast("-").substringBefore("?")
    }

    /**
     * Fetch HTML from URL
     */
    private fun fetchHtml(url: String): String {
        Log.d(tag, "Fetching: $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .addHeader("Accept-Language", "en-US,en;q=0.9")
            .build()

        val response = client.newCall(request).execute()

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code}: ${response.message}")
        }

        return response.body?.string() ?: throw Exception("Empty response body")
    }
}
