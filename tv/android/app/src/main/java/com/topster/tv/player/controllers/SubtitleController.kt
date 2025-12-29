package com.topster.tv.player.controllers

import android.util.Log
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.topster.tv.player.PlayerEventListener
import com.topster.tv.player.Subtitle
import com.topster.tv.player.VideoMetadata

/**
 * Manages subtitle track selection
 */
class SubtitleController(
    private val trackSelector: DefaultTrackSelector
) : PlayerEventListener {

    companion object {
        private const val TAG = "SubtitleController"
    }

    private var availableSubtitles = listOf<Subtitle>()
    private var selectedSubtitleIndex = -1

    override fun onVideoLoaded(video: VideoMetadata) {
        availableSubtitles = video.subtitles
        selectedSubtitleIndex = -1

        // Auto-select first subtitle if available
        if (availableSubtitles.isNotEmpty()) {
            selectSubtitle(0)
        }
    }

    fun getAvailableSubtitles(): List<Subtitle> = availableSubtitles

    fun selectSubtitle(index: Int) {
        if (index < 0 || index >= availableSubtitles.size) {
            Log.w(TAG, "Invalid subtitle index: $index")
            return
        }

        selectedSubtitleIndex = index
        val subtitle = availableSubtitles[index]
        Log.d(TAG, "Selected subtitle: ${subtitle.language}")

        // In real implementation, this would configure ExoPlayer's text track
    }

    fun disableSubtitles() {
        selectedSubtitleIndex = -1
        Log.d(TAG, "Subtitles disabled")
    }

    fun getCurrentSubtitle(): Subtitle? {
        return if (selectedSubtitleIndex >= 0 && selectedSubtitleIndex < availableSubtitles.size) {
            availableSubtitles[selectedSubtitleIndex]
        } else {
            null
        }
    }
}
