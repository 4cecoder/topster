package com.topster.tv.scraper.extractors

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.topster.tv.api.models.VideoInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL

private const val TAG = "StreamSBExtractor"

private val STREAMSB_HOSTS = listOf(
    "https://streamsss.net/sources50",
    "https://watchsb.com/sources50",
    "https://sbplay2.com/sources48"
)

data class StreamSBResponse(
    @SerializedName("stream_data") val streamData: Any? = null,
    @SerializedName("file") val file: String? = null
)

class StreamSBExtractor(private val client: OkHttpClient, private val gson: Gson) : VideoExtractor {

    override suspend fun extract(embedUrl: String, referer: String?): List<VideoInfo> {
        try {
            val videoId = extractVideoId(embedUrl)
            val hexId = stringToHex(videoId)
            val payload = generatePayload(hexId)
            val embedReferer = getRefererFromEmbed(embedUrl)

            Log.d(TAG, "Extracting video ID $videoId")

            // Try each host until one works
            var lastError: Exception? = null

            for (host in STREAMSB_HOSTS) {
                try {
                    val apiUrl = "$host/$payload"

                    val request = Request.Builder()
                        .url(apiUrl)
                        .addHeader("watchsb", "sbstream")
                        .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                        .addHeader("Referer", embedUrl)
                        .build()

                    val response = client.newCall(request).execute()
                    if (!response.isSuccessful) {
                        throw Exception("HTTP ${response.code}")
                    }

                    val responseBody = response.body?.string() ?: throw Exception("Empty response")

                    // Try to parse as JSON
                    val data = try {
                        gson.fromJson(responseBody, StreamSBResponse::class.java)
                    } catch (e: Exception) {
                        // If not JSON, try to find M3U8 URL in the response
                        val m3u8Match = Regex("(https?://[^\\s\"']+\\.m3u8[^\\s\"']*)").find(responseBody)
                        if (m3u8Match != null) {
                            Log.d(TAG, "Found M3U8 URL directly")
                            return listOf(VideoInfo(
                                url = m3u8Match.groupValues[1],
                                subtitles = emptyList(),
                                referer = embedReferer,
                                quality = "auto"
                            ))
                        }
                        throw Exception("Failed to parse StreamSB response")
                    }

                    // Check for stream_data in response
                    if (data.streamData != null) {
                        val streamUrl = when (data.streamData) {
                            is String -> data.streamData
                            is Map<*, *> -> {
                                @Suppress("UNCHECKED_CAST")
                                val streamMap = data.streamData as? Map<String, Any>
                                streamMap?.get("file") as? String
                            }
                            else -> null
                        }

                        if (streamUrl != null && streamUrl.contains(".m3u8")) {
                            Log.d(TAG, "Using referer $embedReferer for video URL")

                            // Fetch the M3U8 playlist to get quality variants
                            try {
                                val playlistRequest = Request.Builder()
                                    .url(streamUrl)
                                    .build()

                                val playlistResponse = client.newCall(playlistRequest).execute()
                                val playlistContent = playlistResponse.body?.string() ?: ""

                                val sources = parseM3U8(playlistContent, streamUrl)

                                if (sources.isNotEmpty()) {
                                    return sources.map { source ->
                                        VideoInfo(
                                            url = source.file,
                                            subtitles = emptyList(),
                                            referer = embedReferer,
                                            quality = source.type ?: "auto"
                                        )
                                    }
                                }
                            } catch (playlistError: Exception) {
                                Log.w(TAG, "Failed to fetch M3U8 playlist, using master URL", playlistError)
                            }

                            // Fallback to master URL
                            return listOf(VideoInfo(
                                url = streamUrl,
                                subtitles = emptyList(),
                                referer = embedReferer,
                                quality = "auto"
                            ))
                        }
                    }

                    // If we found data but no stream_data, check for direct file property
                    if (data.file != null) {
                        return listOf(VideoInfo(
                            url = data.file,
                            subtitles = emptyList(),
                            referer = embedReferer,
                            quality = "auto"
                        ))
                    }

                    // If we got here, this host didn't work, try next one
                    throw Exception("No valid stream data found")

                } catch (e: Exception) {
                    Log.w(TAG, "StreamSB host $host failed", e)
                    lastError = e
                    continue
                }
            }

            // All hosts failed
            throw Exception("All StreamSB hosts failed. Last error: ${lastError?.message}")

        } catch (e: Exception) {
            Log.e(TAG, "StreamSB extraction failed", e)
            throw e
        }
    }

    private fun extractVideoId(url: String): String {
        // URL format: https://watchsb.com/e/xxxxx.html or https://streamsss.net/e/xxxxx
        val parts = url.split("/e/")
        if (parts.size < 2) {
            throw Exception("Invalid StreamSB URL format")
        }

        var id = parts[1]
        if (id.isEmpty()) {
            throw Exception("Invalid StreamSB URL format")
        }

        // Remove .html extension if present
        id = id.replace(".html", "").split("?")[0]

        return id
    }

    private fun generatePayload(hex: String): String {
        return "566d337678566f743674494a7c7c${hex}7c7c346b6767586d6934774855537c7c73747265616d7362/6565417268755339773461447c7c346133383438333436313335376136323337373433383634376337633465366534393338373136643732373736343735373237613763376334363733353737303533366236333463353333363534366137633763373337343732363536313664373336327c7c6b586c3163614468645a47617c7c73747265616d7362"
    }

    private fun stringToHex(str: String): String {
        return str.toByteArray(Charsets.UTF_8)
            .joinToString("") { "%02x".format(it) }
    }

    private fun parseM3U8(content: String, baseUrl: String): List<Source> {
        val sources = mutableListOf<Source>()

        val lines = content.split("\n")

        for (i in lines.indices) {
            val line = lines[i]

            if (line.startsWith("#EXT-X-STREAM-INF:")) {
                // Extract resolution from RESOLUTION parameter
                val resolutionMatch = Regex("RESOLUTION=(\\d+)x(\\d+)").find(line)
                val quality = resolutionMatch?.let { "${it.groupValues[2]}p" }

                // Next line should be the URL
                if (i + 1 < lines.size) {
                    val urlLine = lines[i + 1].trim()
                    if (urlLine.isNotEmpty() && !urlLine.startsWith("#")) {
                        var fileUrl = urlLine

                        // Handle relative URLs
                        if (!fileUrl.startsWith("http")) {
                            val base = URL(baseUrl)
                            fileUrl = "${base.protocol}://${base.host}${if (fileUrl.startsWith("/")) "" else "/"}$fileUrl"
                        }

                        sources.add(Source(file = fileUrl, type = quality))
                    }
                }
            }
        }

        // If no variants found, the whole content might be the master URL
        if (sources.isEmpty() && content.contains(".m3u8")) {
            sources.add(Source(file = baseUrl, type = "auto"))
        }

        return sources
    }

    private fun getRefererFromEmbed(embedUrl: String): String {
        return try {
            val url = URL(embedUrl)
            "${url.protocol}://${url.host}/"
        } catch (e: Exception) {
            embedUrl
        }
    }
}
