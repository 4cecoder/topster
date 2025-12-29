package com.topster.tv.player.controllers

import android.content.Context
import android.util.Log
import com.topster.tv.database.HistoryEntity
import com.topster.tv.database.HistoryManager
import com.topster.tv.player.PlayerEventListener
import com.topster.tv.player.VideoMetadata
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * Manages video state persistence and restoration
 * Based on SmartTube's VideoStateController
 */
class VideoStateController(
    context: Context,
    private val historyManager: HistoryManager
) : PlayerEventListener {

    private val contextRef = WeakReference(context)
    private var currentVideo: VideoMetadata? = null
    private var tickCounter = 0

    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "VideoStateController"
        // Save state every 3 minutes (following SmartTube)
        private const val HISTORY_UPDATE_INTERVAL_SECONDS = 180
    }

    override fun onVideoLoaded(video: VideoMetadata) {
        currentVideo = video
        tickCounter = 0
    }

    override fun onPositionUpdate(positionMs: Long, durationMs: Long) {
        // Update periodically in onTick
    }

    override fun onTick() {
        tickCounter++

        if (tickCounter >= HISTORY_UPDATE_INTERVAL_SECONDS) {
            tickCounter = 0
            saveState()
        }
    }

    override fun onPlayEnd() {
        saveState()
    }

    override fun onViewPaused() {
        saveState()
    }

    override fun onFinish() {
        saveState()
    }

    private fun saveState() {
        val video = currentVideo ?: return
        val context = contextRef.get() ?: return

        scope.launch {
            try {
                // Get current position from player
                // This would be injected in real implementation
                val position = 0L // Placeholder
                val duration = 0L // Placeholder

                if (position > 0 && duration > 0) {
                    historyManager.updateWatchHistory(
                        mediaId = video.id,
                        title = video.title,
                        type = video.type,
                        url = video.videoUrl,
                        posterImage = video.thumbnail,
                        episodeId = null, // Would need to be passed separately
                        episodeTitle = null,
                        seasonNumber = video.season,
                        episodeNumber = video.episode,
                        position = position,
                        duration = duration
                    )
                    Log.d(TAG, "State saved: ${video.title} at $position/$duration")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save state", e)
            }
        }
    }

    /**
     * Restore saved position for a video
     */
    suspend fun getSavedPosition(videoId: String, episodeId: String? = null): Long? {
        return try {
            historyManager.getHistoryEntry(videoId, episodeId)?.position
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore position", e)
            null
        }
    }
}
