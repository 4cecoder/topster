package com.topster.tv.scraper.extractors

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.topster.tv.api.models.Subtitle
import com.topster.tv.api.models.VideoInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

private const val TAG = "RapidCloudExtractor"
private const val FALLBACK_KEY = "c1d17096f2ca11b7"
private const val GITHUB_KEY_URL = "https://raw.githubusercontent.com/enimax-anime/key/e4/key.txt"

data class RapidCloudSourceResponse(
    @SerializedName("sources") val sources: Any? = null,  // Can be String or List
    @SerializedName("tracks") val tracks: List<Track>? = null,
    @SerializedName("encrypted") val encrypted: Boolean? = null,
    @SerializedName("intro") val intro: Intro? = null,
    @SerializedName("outro") val outro: Outro? = null,
    @SerializedName("server") val server: Int? = null
)

class RapidCloudExtractor(private val client: OkHttpClient, private val gson: Gson) : VideoExtractor {

    override suspend fun extract(embedUrl: String, referer: String?): List<VideoInfo> {
        try {
            val videoId = extractVideoId(embedUrl)
            val embedReferer = getRefererFromEmbed(embedUrl)

            // Determine the hostname from the embed URL
            val url = URL(embedUrl)
            val hostname = url.host

            // Build API URL
            val apiUrl = "https://$hostname/embed-2/ajax/e-1/getSources?id=$videoId"

            Log.d(TAG, "Fetching sources for video ID $videoId")

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Referer", embedUrl)
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }

            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            val data = gson.fromJson(responseBody, RapidCloudSourceResponse::class.java)

            var sources: List<Source> = emptyList()

            // Check if sources are encrypted
            when {
                data.encrypted == true && data.sources is String -> {
                    Log.d(TAG, "Sources are encrypted, fetching decryption key")

                    // Get decryption key from GitHub
                    val keyString = getDecryptionKey()
                    val keyIndices = parseDecryptionKey(keyString)

                    if (keyIndices.isNotEmpty()) {
                        // Extract the actual decryption key from the encrypted data
                        val (key, encrypted) = extractSecretKey(data.sources, keyIndices)

                        Log.d(TAG, "Decrypting sources with extracted key")

                        // Decrypt the sources
                        val decrypted = decryptSources(encrypted, key)
                        sources = gson.fromJson(decrypted, Array<Source>::class.java).toList()

                        Log.d(TAG, "Sources decrypted successfully")
                    } else {
                        // Try direct decryption with the key as-is
                        Log.d(TAG, "Using key directly for decryption")
                        val decrypted = decryptSources(data.sources, keyString)
                        sources = gson.fromJson(decrypted, Array<Source>::class.java).toList()
                    }
                }
                data.sources is List<*> -> {
                    @Suppress("UNCHECKED_CAST")
                    sources = gson.fromJson(gson.toJson(data.sources), Array<Source>::class.java).toList()
                    Log.d(TAG, "Sources are not encrypted")
                }
                data.sources is String -> {
                    sources = gson.fromJson(data.sources, Array<Source>::class.java).toList()
                }
            }

            // Filter for M3U8 sources
            val m3u8Sources = sources.filter { it.file.contains(".m3u8") }

            if (m3u8Sources.isEmpty()) {
                throw Exception("No M3U8 sources found")
            }

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
        } catch (e: Exception) {
            Log.e(TAG, "RapidCloud extraction failed", e)
            throw e
        }
    }

    private fun extractVideoId(url: String): String {
        // URL format: https://rapid-cloud.co/embed-6/xxxxx
        val parts = url.split("/")
        val idWithQuery = parts.lastOrNull() ?: throw Exception("Invalid RapidCloud URL format")
        return idWithQuery.split("?")[0]
    }

    private fun getDecryptionKey(): String {
        return try {
            val request = Request.Builder()
                .url(GITHUB_KEY_URL)
                .build()

            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return FALLBACK_KEY

            // Extract key from HTML response
            val match = Regex("blob-code blob-code-inner js-file-line\">([^<]+)").find(html)
            if (match != null && match.groupValues.size > 1) {
                return match.groupValues[1].trim()
            }

            Log.w(TAG, "Failed to extract key from GitHub, using fallback")
            FALLBACK_KEY
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch decryption key from GitHub", e)
            FALLBACK_KEY
        }
    }

    private fun parseDecryptionKey(key: String): List<List<Int>> {
        return try {
            gson.fromJson(key, Array<Array<Int>>::class.java).map { it.toList() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun extractSecretKey(encryptedData: String, indices: List<List<Int>>): Pair<String, String> {
        val key = StringBuilder()
        var currentIndex = 0
        val dataArray = encryptedData.toMutableList()

        for (indexPair in indices) {
            val offset = indexPair[0]
            val length = indexPair[1]

            val start = offset + currentIndex
            val end = start + length

            for (i in start until end.coerceAtMost(dataArray.size)) {
                key.append(dataArray[i])
                dataArray[i] = ' '
            }
            currentIndex += length
        }

        return Pair(key.toString(), dataArray.joinToString("").replace(" ", ""))
    }

    private fun decryptSources(encryptedData: String, key: String): String {
        try {
            // Decode base64
            val encrypted = Base64.decode(encryptedData, Base64.DEFAULT)

            // Create cipher for AES decryption
            val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
            val secretKey = SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES")
            cipher.init(Cipher.DECRYPT_MODE, secretKey)

            val decrypted = cipher.doFinal(encrypted)
            return String(decrypted, Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw Exception("Failed to decrypt video sources")
        }
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
