package com.topster.tv.scraper.extractors

import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.topster.tv.api.models.Subtitle
import com.topster.tv.api.models.VideoInfo
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL

private const val TAG = "VidCloudExtractor"
private const val DECRYPT_API_URL = "https://dec.eatmynerds.live"
private const val MAX_RETRIES = 3

data class DecryptedResponse(
    @SerializedName("sources") val sources: List<Source>? = null,
    @SerializedName("tracks") val tracks: List<Track>? = null
)

class VidCloudExtractor(private val client: OkHttpClient, private val gson: Gson) : VideoExtractor {

    override suspend fun extract(embedUrl: String, referer: String?): List<VideoInfo> {
        val embedReferer = getRefererFromEmbed(embedUrl)
        var lastError: Exception? = null

        // Retry up to MAX_RETRIES times since the API can be flaky
        for (attempt in 1..MAX_RETRIES) {
            try {
                val decryptUrl = "$DECRYPT_API_URL/?url=${java.net.URLEncoder.encode(embedUrl, "UTF-8")}"

                Log.d(TAG, "Fetching from decryption API (attempt $attempt)")

                val request = Request.Builder()
                    .url(decryptUrl)
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw Exception("HTTP ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw Exception("Empty response")
                val data = gson.fromJson(responseBody, DecryptedResponse::class.java)

                if (data.sources != null && data.sources.isNotEmpty()) {
                    // Filter for M3U8 sources
                    val m3u8Sources = data.sources.filter { it.file.contains(".m3u8") }

                    if (m3u8Sources.isNotEmpty()) {
                        // Parse subtitles
                        val subtitles = (data.tracks ?: emptyList())
                            .filter { it.kind == "captions" || it.kind == "subtitles" }
                            .map { Subtitle(
                                url = it.file,
                                lang = it.label?.lowercase() ?: "unknown",
                                label = it.label ?: "Unknown"
                            ) }

                        Log.d(TAG, "Using referer $embedReferer for video URLs")

                        // Return video info for each source
                        return m3u8Sources.map { source ->
                            VideoInfo(
                                url = source.file,
                                subtitles = subtitles,
                                referer = embedReferer,
                                quality = source.type ?: "auto"
                            )
                        }
                    }
                }

                // API returned empty sources, retry after a short delay
                if (attempt < MAX_RETRIES) {
                    delay(1000L * attempt)
                }
            } catch (e: Exception) {
                lastError = e
                if (attempt < MAX_RETRIES) {
                    delay(1000L * attempt)
                }
            }
        }

        // All retries failed
        Log.e(TAG, "VidCloud extraction failed after $MAX_RETRIES attempts", lastError)
        throw Exception("Failed to extract video from VidCloud: $embedUrl")
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
