package com.topster.tv.scraper

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

data class IMDbMetadata(
    val title: String,
    val year: String,
    val rating: String,
    val plot: String,
    val genres: List<String>,
    val runtime: String,
    val cast: List<String>,
    val poster: String?,
    val imdbId: String
)

class IMDbScraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val tag = "IMDbScraper"
    private val cache = mutableMapOf<String, IMDbMetadata>()
    private val OMDB_API_KEY = "83209e13" // OMDb API key

    suspend fun searchAndGetMetadata(title: String, year: String? = null): IMDbMetadata? = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cacheKey = "$title-$year"
            cache[cacheKey]?.let { return@withContext it }

            Log.d(tag, "Fetching OMDb data for: $title")

            // Use OMDb API to get details by title
            val params = StringBuilder("http://www.omdbapi.com/?apikey=$OMDB_API_KEY&t=${java.net.URLEncoder.encode(title, "UTF-8")}&plot=full")
            if (year != null) {
                params.append("&y=$year")
            }

            val request = Request.Builder()
                .url(params.toString())
                .build()

            val response = client.newCall(request).execute()
            val json = response.body?.string() ?: return@withContext null

            // Parse JSON response
            val jsonObj = org.json.JSONObject(json)

            if (jsonObj.optString("Response") != "True") {
                Log.w(tag, "OMDb API error: ${jsonObj.optString("Error")}")
                return@withContext null
            }

            val metadata = IMDbMetadata(
                title = jsonObj.optString("Title", title),
                year = jsonObj.optString("Year", year ?: ""),
                rating = jsonObj.optString("imdbRating", "N/A"),
                plot = jsonObj.optString("Plot", "No plot available"),
                genres = jsonObj.optString("Genre", "").split(", ").filter { it.isNotEmpty() },
                runtime = jsonObj.optString("Runtime", "N/A"),
                cast = jsonObj.optString("Actors", "").split(", ").filter { it.isNotEmpty() },
                poster = jsonObj.optString("Poster").takeIf { it != "N/A" },
                imdbId = jsonObj.optString("imdbID", "")
            )

            // Cache the result
            cache[cacheKey] = metadata

            Log.d(tag, "Successfully fetched OMDb data for: ${metadata.title}")
            metadata
        } catch (e: Exception) {
            Log.e(tag, "Failed to fetch OMDb metadata", e)
            null
        }
    }
}
