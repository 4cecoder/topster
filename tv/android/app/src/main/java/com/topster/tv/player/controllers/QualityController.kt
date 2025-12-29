package com.topster.tv.player.controllers

import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.topster.tv.player.PlayerEventListener
import com.topster.tv.player.VideoMetadata

/**
 * Manages video quality/track selection
 * Based on SmartTube's track selector approach
 */
class QualityController(
    private val trackSelector: DefaultTrackSelector
) : PlayerEventListener {

    companion object {
        private const val TAG = "QualityController"
    }

    data class QualityOption(
        val height: Int,
        val label: String,
        val trackIndex: Int
    )

    private var availableQualities = listOf<QualityOption>()
    private var selectedQualityIndex = -1

    override fun onVideoLoaded(video: VideoMetadata) {
        // Reset quality selection
        availableQualities = emptyList()
        selectedQualityIndex = -1
    }

    override fun onSourceChanged() {
        // Refresh available qualities when source changes
        updateAvailableQualities()
    }

    private fun updateAvailableQualities() {
        try {
            // This would inspect ExoPlayer's tracks
            // Simplified for now
            availableQualities = listOf(
                QualityOption(1080, "1080p", 0),
                QualityOption(720, "720p", 1),
                QualityOption(480, "480p", 2),
                QualityOption(360, "360p", 3)
            )

            Log.d(TAG, "Available qualities: ${availableQualities.size}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update qualities", e)
        }
    }

    fun getAvailableQualities(): List<QualityOption> = availableQualities

    fun selectQuality(qualityIndex: Int) {
        if (qualityIndex < 0 || qualityIndex >= availableQualities.size) {
            Log.w(TAG, "Invalid quality index: $qualityIndex")
            return
        }

        selectedQualityIndex = qualityIndex
        val quality = availableQualities[qualityIndex]

        // Update track selector parameters
        try {
            val parameters = trackSelector.parameters
                .buildUpon()
                .setMaxVideoSize(Int.MAX_VALUE, quality.height)
                .build()

            trackSelector.parameters = parameters
            Log.d(TAG, "Selected quality: ${quality.label}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set quality", e)
        }
    }

    fun selectAuto() {
        try {
            val parameters = trackSelector.parameters
                .buildUpon()
                .clearVideoSizeConstraints()
                .build()

            trackSelector.parameters = parameters
            selectedQualityIndex = -1
            Log.d(TAG, "Selected auto quality")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set auto quality", e)
        }
    }
}
