package com.topster.tv.database

import android.content.Context
import android.util.Log
import com.topster.tv.api.models.MediaItem
import kotlinx.coroutines.flow.Flow

class HistoryManager(context: Context) {
    private val database = TopsterDatabase.getDatabase(context)
    private val historyDao = database.historyDao()
    private val tag = "HistoryManager"

    /**
     * Get recent watch history
     */
    fun getRecentHistory(limit: Int = 50): Flow<List<HistoryEntity>> {
        return historyDao.getRecentHistory(limit)
    }

    /**
     * Get incomplete (unfinished) watch history for "Continue Watching"
     */
    fun getIncompleteHistory(): Flow<List<HistoryEntity>> {
        return historyDao.getIncompleteHistory()
    }

    /**
     * Get history entry for a specific media/episode
     */
    suspend fun getHistoryEntry(mediaId: String, episodeId: String? = null): HistoryEntity? {
        return historyDao.getHistoryEntry(mediaId, episodeId)
    }

    /**
     * Get all watch history for grouped display
     */
    fun getAllWatchHistory(): List<HistoryEntity> {
        return historyDao.getAllHistory().blockingFirst() ?: emptyList()
    }

    /**
     * Get all history for a specific TV show
     */
    fun getHistoryByShow(showTitle: String): List<HistoryEntity> {
        return historyDao.getHistoryByShow(showTitle).blockingFirst() ?: emptyList()
    }

    /**
     * Clear all watch history
     */
    suspend fun clearHistory() {
        historyDao.clearHistory()
        Log.d(tag, "All watch history cleared")
    }

    /**
     * Add or update watch history
     */
    suspend fun updateWatchHistory(
        mediaId: String,
        title: String,
        type: String,
        url: String,
        posterImage: String? = null,
        episodeId: String? = null,
        episodeTitle: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        position: Long = 0,
        duration: Long = 0
    ) {
        try {
            val percentWatched = if (duration > 0) {
                (position.toFloat() / duration.toFloat()) * 100f
            } else 0f

            val completed = percentWatched >= 90f

            // Check if entry exists
            val existing = historyDao.getHistoryEntry(mediaId, episodeId)

            if (existing != null) {
                // Update existing entry
                val updated = existing.copy(
                    position = position,
                    duration = duration,
                    percentWatched = percentWatched,
                    completed = completed,
                    lastWatched = System.currentTimeMillis()
                )
                historyDao.updateHistory(updated)
                Log.d(tag, "Updated history for $title")
            } else {
                // Create new entry
                val newEntry = HistoryEntity(
                    mediaId = mediaId,
                    title = title,
                    type = type,
                    url = url,
                    posterImage = posterImage,
                    episodeId = episodeId,
                    episodeTitle = episodeTitle,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                    position = position,
                    duration = duration,
                    percentWatched = percentWatched,
                    completed = completed,
                    lastWatched = System.currentTimeMillis(),
                    firstWatched = System.currentTimeMillis()
                )
                historyDao.insertHistory(newEntry)
                Log.d(tag, "Added history for $title")
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to update history: ${e.message}")
        }
    }

    /**
     * Delete a history entry
     */
    suspend fun deleteHistoryEntry(mediaId: String, episodeId: String? = null) {
        historyDao.deleteHistoryEntry(mediaId, episodeId)
        Log.d(tag, "Deleted history entry for $mediaId")
    }

    /**
     * Clear all history
     */
    suspend fun clearAllHistory() {
        historyDao.clearAllHistory()
        Log.d(tag, "Cleared all history")
    }

    /**
     * Get total history count
     */
    suspend fun getHistoryCount(): Int {
        return historyDao.getHistoryCount()
    }
}
