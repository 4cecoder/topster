package com.topster.tv.utils

import android.content.Context
import android.os.Build
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Image cache configuration following SmartTube's approach
 * Uses Coil instead of Glide for Compose compatibility
 */
object ImageCache {
    private const val MEMORY_CACHE_SIZE_MB = 10L * 1024 * 1024 // 10 MB
    private const val DISK_CACHE_SIZE_MB = 50L * 1024 * 1024 // 50 MB

    /**
     * Create optimized ImageLoader for TV
     */
    fun createImageLoader(context: Context): ImageLoader {
        // Following SmartTube: Disable HTTP keep-alive for better performance
        System.setProperty("http.keepAlive", "false")

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizeBytes(MEMORY_CACHE_SIZE_MB.toInt())
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(DISK_CACHE_SIZE_MB)
                    .build()
            }
            .okHttpClient(okHttpClient)
            .respectCacheHeaders(false) // Following SmartTube: Ignore cache headers
            .build()
    }

    /**
     * Clear all image caches
     */
    fun clearCache(imageLoader: ImageLoader) {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }
}
