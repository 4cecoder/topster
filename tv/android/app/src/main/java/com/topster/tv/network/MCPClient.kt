package com.topster.tv.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * MCP (Model Context Protocol) client for communicating with Topster CLI/Web backend
 */
class MCPClient(
    private val host: String = "192.168.1.100", // Default to network IP
    private val port: Int = 3847
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private var requestId = 0

    companion object {
        private const val TAG = "MCPClient"
        private const val JSON_MEDIA_TYPE = "application/json"
    }

    data class MCPRequest(
        val jsonrpc: String = "2.0",
        val id: Int,
        val method: String,
        val params: Map<String, Any?>? = null
    )

    data class MCPResponse<T>(
        val jsonrpc: String,
        val id: Int,
        val result: T?,
        val error: MCPError?
    )

    data class MCPError(
        val code: Int,
        val message: String,
        val data: Any?
    )

    data class MCPToolResponse(
        val content: List<MCPContent>
    )

    data class MCPContent(
        val type: String,
        val text: String
    )

    /**
     * Generic RPC request
     */
    private suspend fun <T> request(
        method: String,
        params: Map<String, Any?>? = null,
        responseClass: Class<T>
    ): T = withContext(Dispatchers.IO) {
        val id = ++requestId
        val mcpRequest = MCPRequest(
            id = id,
            method = method,
            params = params
        )

        val requestBody = gson.toJson(mcpRequest)
            .toRequestBody(JSON_MEDIA_TYPE.toMediaType())

        val request = Request.Builder()
            .url("http://$host:$port/messages")
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val responseBody = response.body?.string()
                ?: throw Exception("Empty response body")

            val mcpResponse = gson.fromJson(responseBody, MCPResponse::class.java)

            if (mcpResponse.error != null) {
                throw Exception("MCP Error: ${mcpResponse.error.message}")
            }

            val resultJson = gson.toJson(mcpResponse.result)
            gson.fromJson(resultJson, responseClass)
        } catch (e: Exception) {
            Log.e(TAG, "Request failed: $method", e)
            throw e
        }
    }

    /**
     * Search for movies/TV shows
     */
    suspend fun search(query: String): List<MediaItem> {
        val result = request(
            method = "tools/call",
            params = mapOf(
                "name" to "search",
                "arguments" to mapOf("query" to query)
            ),
            responseClass = MCPToolResponse::class.java
        )

        return parseMediaItems(result)
    }

    /**
     * Get trending content
     */
    suspend fun getTrending(): List<MediaItem> {
        val result = request(
            method = "tools/call",
            params = mapOf(
                "name" to "get_trending",
                "arguments" to emptyMap<String, Any>()
            ),
            responseClass = MCPToolResponse::class.java
        )

        return parseMediaItems(result)
    }

    /**
     * Get recent releases
     */
    suspend fun getRecent(): List<MediaItem> {
        val result = request(
            method = "tools/call",
            params = mapOf(
                "name" to "get_recent",
                "arguments" to emptyMap<String, Any>()
            ),
            responseClass = MCPToolResponse::class.java
        )

        return parseMediaItems(result)
    }

    /**
     * Get video sources for playback
     */
    suspend fun getVideoSources(episodeId: String): StreamingData {
        val result = request(
            method = "tools/call",
            params = mapOf(
                "name" to "get_video_sources",
                "arguments" to mapOf("episodeId" to episodeId)
            ),
            responseClass = MCPToolResponse::class.java
        )

        val json = result.content.firstOrNull()?.text ?: "{}"
        return gson.fromJson(json, StreamingData::class.java)
    }

    /**
     * Get TV show seasons
     */
    suspend fun getSeasons(mediaId: String): List<Season> {
        val result = request(
            method = "tools/call",
            params = mapOf(
                "name" to "get_seasons",
                "arguments" to mapOf("mediaId" to mediaId)
            ),
            responseClass = MCPToolResponse::class.java
        )

        val json = result.content.firstOrNull()?.text ?: "[]"
        return gson.fromJson(json, Array<Season>::class.java).toList()
    }

    /**
     * Get episodes for a season
     */
    suspend fun getEpisodes(mediaId: String, season: Int): List<Episode> {
        val result = request(
            method = "tools/call",
            params = mapOf(
                "name" to "get_episodes",
                "arguments" to mapOf(
                    "mediaId" to mediaId,
                    "season" to season
                )
            ),
            responseClass = MCPToolResponse::class.java
        )

        val json = result.content.firstOrNull()?.text ?: "[]"
        return gson.fromJson(json, Array<Episode>::class.java).toList()
    }

    /**
     * Update watch history
     */
    suspend fun updateHistory(
        mediaId: String,
        title: String,
        type: String,
        position: Long,
        duration: Long,
        season: Int? = null,
        episode: Int? = null,
        image: String? = null
    ) {
        request(
            method = "tools/call",
            params = mapOf(
                "name" to "update_history",
                "arguments" to mapOf(
                    "mediaId" to mediaId,
                    "title" to title,
                    "type" to type,
                    "position" to position,
                    "duration" to duration,
                    "season" to season,
                    "episode" to episode,
                    "image" to image
                )
            ),
            responseClass = MCPToolResponse::class.java
        )
    }

    private fun parseMediaItems(response: MCPToolResponse): List<MediaItem> {
        val json = response.content.firstOrNull()?.text ?: "[]"
        return try {
            gson.fromJson(json, Array<MediaItem>::class.java).toList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse media items", e)
            emptyList()
        }
    }
}

// Data classes
data class MediaItem(
    val id: String,
    val title: String,
    val url: String,
    val image: String,
    val releaseDate: String? = null,
    val seasons: Int? = null,
    val type: String? = null
)

data class StreamingData(
    val sources: List<VideoSource>,
    val subtitles: List<SubtitleTrack>,
    val headers: Map<String, String>? = null
)

data class VideoSource(
    val url: String,
    val quality: String,
    val isM3U8: Boolean
)

data class SubtitleTrack(
    val url: String,
    val lang: String
)

data class Season(
    val season: Int,
    val episodes: List<Episode>
)

data class Episode(
    val id: String,
    val title: String,
    val number: Int,
    val season: Int,
    val url: String
)
