package com.topster.tv.scraper

import android.util.LruCache
import com.topster.tv.api.models.MediaItem

/**
 * Simple in-memory cache for API responses
 */
class CacheManager {
    private val searchCache = LruCache<String, List<MediaItem>>(20)
    private val trendingCache = LruCache<String, List<MediaItem>>(10)
    private val recentCache = LruCache<String, List<MediaItem>>(10)

    // Cache duration in milliseconds (5 minutes)
    private val cacheExpiry = 5 * 60 * 1000L
    private val cacheTimestamps = mutableMapOf<String, Long>()

    fun getCachedSearch(query: String, page: Int): List<MediaItem>? {
        val key = "search:$query:$page"
        return getIfNotExpired(key, searchCache)
    }

    fun cacheSearch(query: String, page: Int, items: List<MediaItem>) {
        val key = "search:$query:$page"
        searchCache.put(key, items)
        cacheTimestamps[key] = System.currentTimeMillis()
    }

    fun getCachedTrending(): List<MediaItem>? {
        val key = "trending"
        return getIfNotExpired(key, trendingCache)
    }

    fun cacheTrending(items: List<MediaItem>) {
        val key = "trending"
        trendingCache.put(key, items)
        cacheTimestamps[key] = System.currentTimeMillis()
    }

    fun getCachedRecent(type: String): List<MediaItem>? {
        val key = "recent:$type"
        return getIfNotExpired(key, recentCache)
    }

    fun cacheRecent(type: String, items: List<MediaItem>) {
        val key = "recent:$type"
        recentCache.put(key, items)
        cacheTimestamps[key] = System.currentTimeMillis()
    }

    fun clearCache() {
        searchCache.evictAll()
        trendingCache.evictAll()
        recentCache.evictAll()
        cacheTimestamps.clear()
    }

    private fun <T> getIfNotExpired(key: String, cache: LruCache<String, T>): T? {
        val timestamp = cacheTimestamps[key] ?: return null
        val age = System.currentTimeMillis() - timestamp

        return if (age < cacheExpiry) {
            cache.get(key)
        } else {
            cache.remove(key)
            cacheTimestamps.remove(key)
            null
        }
    }
}
