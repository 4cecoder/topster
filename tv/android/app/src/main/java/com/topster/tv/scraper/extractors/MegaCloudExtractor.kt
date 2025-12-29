package com.topster.tv.scraper.extractors

import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.topster.tv.api.models.Subtitle
import com.topster.tv.api.models.VideoInfo
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

private const val TAG = "MegaCloudExtractor"
private const val MEGACLOUD_API_BASE = "https://megacloud.tv"
private val PLAYER_SCRIPT_PATTERN = Regex("https://megacloud\\.tv/js/player/a/prod/e\\d+-player\\.min\\.js")

data class MegaCloudSourceResponse(
    @SerializedName("sources") val sources: Any? = null,  // Can be String or List
    @SerializedName("tracks") val tracks: List<Track>? = null,
    @SerializedName("encrypted") val encrypted: Boolean? = null,
    @SerializedName("intro") val intro: Intro? = null,
    @SerializedName("outro") val outro: Outro? = null
)

data class Track(
    @SerializedName("file") val file: String,
    @SerializedName("kind") val kind: String,
    @SerializedName("label") val label: String? = null,
    @SerializedName("default") val default: Boolean? = null
)

data class Source(
    @SerializedName("file") val file: String,
    @SerializedName("type") val type: String? = null
)

data class Intro(
    @SerializedName("start") val start: Int,
    @SerializedName("end") val end: Int
)

data class Outro(
    @SerializedName("start") val start: Int,
    @SerializedName("end") val end: Int
)

class MegaCloudExtractor(private val client: OkHttpClient, private val gson: Gson) : VideoExtractor {

    override suspend fun extract(embedUrl: String, referer: String?): List<VideoInfo> {
        try {
            Log.d(TAG, "========================================")
            Log.d(TAG, "Starting MegaCloud extraction")
            Log.d(TAG, "Embed URL: $embedUrl")
            Log.d(TAG, "Referer: $referer")

            val videoId = extractVideoId(embedUrl)
            val apiUrl = "$MEGACLOUD_API_BASE/embed-2/ajax/e-1/getSources?id=$videoId"

            Log.d(TAG, "Extracted video ID: $videoId")
            Log.d(TAG, "API URL: $apiUrl")

            val request = Request.Builder()
                .url(apiUrl)
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Referer", embedUrl)
                .build()

            Log.d(TAG, "Sending API request...")
            val response = client.newCall(request).execute()
            Log.d(TAG, "Response code: ${response.code}")

            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}: ${response.message}")
            }

            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            Log.d(TAG, "Response body (first 200 chars): ${responseBody.take(200)}")

            val data = gson.fromJson(responseBody, MegaCloudSourceResponse::class.java)
            Log.d(TAG, "Parsed response - encrypted: ${data.encrypted}, sources type: ${data.sources?.javaClass?.simpleName}")

            var sources: List<Source> = emptyList()

            // Check if sources are encrypted
            when {
                data.encrypted == true && data.sources is String -> {
                    Log.d(TAG, "Sources are encrypted, extracting key from player script")

                    // Extract decryption variables from player script
                    val variables = extractKeyFromScript(embedUrl)

                    // Extract secret key and encrypted source
                    val (secret, encryptedSource) = getSecret(data.sources, variables)

                    // Decrypt using the extracted secret
                    val decrypted = decrypt(encryptedSource, secret)
                    sources = gson.fromJson(decrypted, Array<Source>::class.java).toList()

                    Log.d(TAG, "Sources decrypted successfully")
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

            Log.d(TAG, "Total sources found: ${sources.size}")
            sources.forEachIndexed { index, source ->
                Log.d(TAG, "Source $index: ${source.file.take(100)}, type: ${source.type}")
            }

            // Filter for M3U8 sources
            val m3u8Sources = sources.filter { it.file.contains(".m3u8") }
            Log.d(TAG, "M3U8 sources found: ${m3u8Sources.size}")

            if (m3u8Sources.isEmpty()) {
                Log.e(TAG, "ERROR: No M3U8 sources found in ${sources.size} total sources")
                throw Exception("No M3U8 sources found. Total sources: ${sources.size}")
            }

            // Parse subtitles
            val subtitles = (data.tracks ?: emptyList())
                .filter { it.kind == "captions" || it.kind == "subtitles" }
                .map { Subtitle(
                    url = it.file,
                    lang = it.label?.lowercase() ?: "unknown",
                    label = it.label ?: "Unknown"
                ) }
            Log.d(TAG, "Subtitles found: ${subtitles.size}")

            // Return video info for each source
            val results = m3u8Sources.map { source ->
                VideoInfo(
                    url = source.file,
                    subtitles = subtitles,
                    referer = referer ?: MEGACLOUD_API_BASE,
                    quality = source.type ?: "auto"
                )
            }

            Log.d(TAG, "MegaCloud extraction SUCCESS - returning ${results.size} video sources")
            Log.d(TAG, "========================================")
            return results
        } catch (e: Exception) {
            Log.e(TAG, "MegaCloud extraction FAILED: ${e.message}", e)
            Log.e(TAG, "========================================")
            throw e
        }
    }

    private fun extractVideoId(url: String): String {
        // URL format: https://megacloud.tv/embed-2/e-1/xxxxx or similar
        val match = Regex("/e(?:-\\d+)?/([^?#/]+)").find(url)
            ?: throw Exception("Invalid MegaCloud URL format")
        return match.groupValues[1]
    }

    private fun extractKeyFromScript(embedUrl: String): List<List<Int>> {
        try {
            // Fetch the embed page to find the player script URL
            val htmlRequest = Request.Builder()
                .url(embedUrl)
                .build()

            val htmlResponse = client.newCall(htmlRequest).execute()
            val html = htmlResponse.body?.string() ?: throw Exception("Empty embed page")

            val scriptMatch = PLAYER_SCRIPT_PATTERN.find(html)
                ?: throw Exception("Could not find player script URL")

            val scriptUrl = "${scriptMatch.value}?t=${System.currentTimeMillis()}"

            // Fetch the player script
            val scriptRequest = Request.Builder()
                .url(scriptUrl)
                .build()

            val scriptResponse = client.newCall(scriptRequest).execute()
            val script = scriptResponse.body?.string() ?: throw Exception("Empty script")

            // Extract variables using regex patterns
            val variables = extractVariables(script)

            if (variables.isEmpty()) {
                throw Exception("Could not extract encryption variables from player script")
            }

            return variables
        } catch (e: Exception) {
            Log.e(TAG, "Failed to extract key from script", e)
            throw e
        }
    }

    private fun extractVariables(script: String): List<List<Int>> {
        // Pattern matches switch case statements with two variable assignments
        val regex = Regex("case\\s*0x[0-9a-f]+:(?![^;]*=partKey)\\s*\\w+\\s*=\\s*(\\w+)\\s*,\\s*\\w+\\s*=\\s*(\\w+);")
        val matches = regex.findAll(script)
        val variables = mutableListOf<List<Int>>()

        for (match in matches) {
            val firstVar = match.groupValues[1]
            val secondVar = match.groupValues[2]

            // Find hex values for these variables
            val firstValue = matchingKey(script, firstVar)
            val secondValue = matchingKey(script, secondVar)

            if (firstValue != null && secondValue != null) {
                variables.add(listOf(firstValue.toInt(16), secondValue.toInt(16)))
            }
        }

        return variables
    }

    private fun matchingKey(script: String, value: String): String? {
        val regex = Regex(",$value=((?:0x)?([0-9a-fA-F]+))")
        val match = regex.find(script)
        return match?.groupValues?.get(1)?.replace("0x", "")
    }

    private fun getSecret(encryptedString: String, indices: List<List<Int>>): Pair<String, String> {
        val secret = StringBuilder()
        var currentIndex = 0
        val sourceArray = encryptedString.toMutableList()

        for (indexPair in indices) {
            val offset = indexPair[0]
            val length = indexPair[1]

            val start = offset + currentIndex
            val end = start + length

            for (i in start until end.coerceAtMost(sourceArray.size)) {
                secret.append(sourceArray[i])
                sourceArray[i] = ' '
            }
            currentIndex += length
        }

        return Pair(secret.toString(), sourceArray.joinToString("").replace(" ", ""))
    }

    private fun decrypt(encrypted: String, keyOrPassword: String): String {
        try {
            val encryptedBytes = Base64.decode(encrypted, Base64.DEFAULT)

            // Check for Salted__ header (EVP_BytesToKey with MD5)
            if (encryptedBytes.size >= 16 &&
                encryptedBytes.sliceArray(0..7).toString(Charsets.UTF_8) == "Salted__") {

                val salt = encryptedBytes.sliceArray(8..15)
                val ciphertext = encryptedBytes.sliceArray(16 until encryptedBytes.size)

                // Derive key and IV using MD5 (OpenSSL's EVP_BytesToKey)
                val md5Hashes = mutableListOf<ByteArray>()
                var hash = ByteArray(0)

                for (i in 0 until 3) {
                    val data = hash + keyOrPassword.toByteArray(Charsets.UTF_8) + salt
                    val md = MessageDigest.getInstance("MD5")
                    hash = md.digest(data)
                    md5Hashes.add(hash)
                }

                val key = md5Hashes[0] + md5Hashes[1]
                val iv = md5Hashes[2]

                // Decrypt using AES-256-CBC
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
                val decrypted = cipher.doFinal(ciphertext)

                return String(decrypted, Charsets.UTF_8)
            }

            // Fallback: try simple AES decryption
            throw Exception("No Salted__ header found")
        } catch (e: Exception) {
            Log.e(TAG, "Decryption failed", e)
            throw e
        }
    }
}
