package com.topster.tv.player

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.topster.tv.base.BasePresenter
import com.topster.tv.database.HistoryManager
import com.topster.tv.player.controllers.QualityController
import com.topster.tv.player.controllers.SubtitleController
import com.topster.tv.player.controllers.VideoStateController
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import kotlinx.coroutines.*

/**
 * Main playback presenter coordinating all player controllers
 * Following SmartTube's architecture
 */
class PlaybackPresenter private constructor(context: Context) : BasePresenter<PlaybackView>() {

    private val eventListeners = mutableListOf<PlayerEventListener>()
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Controllers
    private val trackSelector = DefaultTrackSelector(context)
    private val videoStateController: VideoStateController
    private val qualityController: QualityController
    private val subtitleController: SubtitleController

    // Tick job for periodic updates
    private var tickJob: Job? = null

    init {
        // Initialize controllers
        val app = context.applicationContext as com.topster.tv.TopsterApplication
        videoStateController = VideoStateController(context, app.historyManager)
        qualityController = QualityController(trackSelector)
        subtitleController = SubtitleController(trackSelector)

        // Register controllers
        eventListeners.add(videoStateController)
        eventListeners.add(qualityController)
        eventListeners.add(subtitleController)

        Log.d(TAG, "PlaybackPresenter initialized with ${eventListeners.size} controllers")
    }

    companion object {
        private const val TAG = "PlaybackPresenter"

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: PlaybackPresenter? = null

        fun getInstance(context: Context): PlaybackPresenter {
            return instance ?: synchronized(this) {
                instance ?: PlaybackPresenter(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    fun onInit() {
        eventListeners.forEach { it.onInit() }
        startTicker()
    }

    fun onViewResumed() {
        eventListeners.forEach { it.onViewResumed() }
    }

    fun onViewPaused() {
        eventListeners.forEach { it.onViewPaused() }
    }

    override fun onViewDestroy() {
        super.onViewDestroy()
        stopTicker()
        eventListeners.forEach { it.onViewDestroy() }
    }

    override fun onFinish() {
        super.onFinish()
        stopTicker()
        eventListeners.forEach { it.onFinish() }
    }

    // Video lifecycle
    fun openVideo(video: VideoMetadata) {
        Log.d(TAG, "Opening video: ${video.title}")
        eventListeners.forEach { it.onVideoLoaded(video) }

        // Restore saved position
        scope.launch {
            val savedPosition = videoStateController.getSavedPosition(video.id)
            if (savedPosition != null && savedPosition > 0) {
                Log.d(TAG, "Restoring position: $savedPosition")
                getView()?.seekTo(savedPosition)
            }
        }
    }

    fun onPlay() {
        eventListeners.forEach { it.onPlay() }
    }

    fun onPause() {
        eventListeners.forEach { it.onPause() }
    }

    fun onSeek(positionMs: Long) {
        eventListeners.forEach { it.onSeek(positionMs) }
    }

    fun onPlayEnd() {
        eventListeners.forEach { it.onPlayEnd() }
    }

    fun onSourceChanged() {
        eventListeners.forEach { it.onSourceChanged() }
    }

    fun onPlayerStateChanged(isPlaying: Boolean, playbackState: Int) {
        eventListeners.forEach { it.onPlayerStateChanged(isPlaying, playbackState) }
    }

    fun onPositionUpdate(positionMs: Long, durationMs: Long) {
        eventListeners.forEach { it.onPositionUpdate(positionMs, durationMs) }
    }

    fun onEngineError(error: Exception) {
        Log.e(TAG, "Player engine error", error)
        eventListeners.forEach { it.onEngineError(error) }
    }

    // Public accessors for controllers
    fun getQualityController(): QualityController = qualityController
    fun getSubtitleController(): SubtitleController = subtitleController
    fun getTrackSelector(): DefaultTrackSelector = trackSelector

    // Ticker for periodic updates (every second)
    private fun startTicker() {
        stopTicker()
        tickJob = scope.launch {
            while (isActive) {
                delay(1000) // 1 second
                eventListeners.forEach { it.onTick() }
            }
        }
    }

    private fun stopTicker() {
        tickJob?.cancel()
        tickJob = null
    }
}

/**
 * View interface for playback screen
 */
interface PlaybackView {
    fun showProgressBar(show: Boolean)
    fun showControls(show: Boolean)
    fun showError(message: String)
    fun seekTo(positionMs: Long)
    fun setPlaybackSpeed(speed: Float)
}
