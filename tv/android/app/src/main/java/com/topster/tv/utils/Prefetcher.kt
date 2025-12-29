package com.topster.tv.utils

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.request.ImageRequest
import com.topster.tv.network.MCPClient
import com.topster.tv.network.MediaItem
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

/**
 * Content prefetcher for improved perceived performance
 * Preloads images and data for upcoming content
 */
class Prefetcher(context: Context, private val imageLoader: ImageLoader) {

    private val contextRef = WeakReference(context)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val mcpClient = MCPClient()

    companion object {
        private const val TAG = "Prefetcher"
        private const val PREFETCH_COUNT = 20 // Prefetch next 20 items
    }

    /**
     * Prefetch images for media items
     */
    fun prefetchImages(items: List<MediaItem>) {
        val context = contextRef.get() ?: return

        scope.launch {
            items.take(PREFETCH_COUNT).forEach { media ->
                try {
                    val request = ImageRequest.Builder(context)
                        .data(media.image)
                        .build()

                    imageLoader.enqueue(request)
                    Log.d(TAG, "Prefetched image: ${media.title}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to prefetch image: ${media.title}", e)
                }

                // Small delay to avoid overwhelming the network
                delay(100)
            }
        }
    }

    /**
     * Prefetch trending content in background
     */
    fun prefetchTrending() {
        scope.launch {
            try {
                val trending = mcpClient.getTrending()
                Log.d(TAG, "Prefetched ${trending.size} trending items")

                // Prefetch their images
                prefetchImages(trending)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prefetch trending", e)
            }
        }
    }

    /**
     * Prefetch recent content in background
     */
    fun prefetchRecent() {
        scope.launch {
            try {
                val recent = mcpClient.getRecent()
                Log.d(TAG, "Prefetched ${recent.size} recent items")

                // Prefetch their images
                prefetchImages(recent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prefetch recent", e)
            }
        }
    }

    /**
     * Prefetch seasons for a TV show
     */
    fun prefetchSeasons(mediaId: String) {
        scope.launch {
            try {
                val seasons = mcpClient.getSeasons(mediaId)
                Log.d(TAG, "Prefetched ${seasons.size} seasons for $mediaId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to prefetch seasons", e)
            }
        }
    }

    /**
     * Clear all prefetch jobs
     */
    fun clear() {
        scope.coroutineContext.cancelChildren()
    }

    /**
     * Cleanup
     */
    fun cleanup() {
        scope.cancel()
    }
}
