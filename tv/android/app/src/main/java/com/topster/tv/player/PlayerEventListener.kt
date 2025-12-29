package com.topster.tv.player

import androidx.media3.common.Player

/**
 * Base interface for player event controllers
 * Following SmartTube's controller pattern
 */
interface PlayerEventListener {
    fun onInit() {}
    fun onViewResumed() {}
    fun onViewPaused() {}
    fun onViewDestroy() {}
    fun onFinish() {}

    fun onVideoLoaded(video: VideoMetadata) {}
    fun onPlay() {}
    fun onPause() {}
    fun onSeek(positionMs: Long) {}
    fun onPlayEnd() {}

    fun onSourceChanged() {}
    fun onPlayerStateChanged(isPlaying: Boolean, playbackState: Int) {}
    fun onPositionUpdate(positionMs: Long, durationMs: Long) {}
    fun onRepeatModeChanged(repeatMode: Int) {}
    fun onPlaybackSpeedChanged(speed: Float) {}

    fun onEngineInitialized() {}
    fun onEngineReleased() {}
    fun onEngineError(error: Exception) {}

    // Periodic tick (called every second)
    fun onTick() {}
}

data class VideoMetadata(
    val id: String,
    val title: String,
    val videoUrl: String,
    val subtitles: List<Subtitle> = emptyList(),
    val thumbnail: String? = null,
    val type: String = "movie", // "movie" or "tv"
    val season: Int? = null,
    val episode: Int? = null
)

data class Subtitle(
    val url: String,
    val language: String,
    val label: String = language
)
