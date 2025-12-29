package com.topster.tv.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.LoadControl
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.trackselection.AdaptiveTrackSelection
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import java.io.File

/**
 * Advanced ExoPlayer manager with error recovery and optimizations
 * Following SmartTube's sophisticated player setup
 */
@UnstableApi
class ExoPlayerManager(private val context: Context) {

    private var player: ExoPlayer? = null
    private val trackSelector: DefaultTrackSelector
    private val bandwidthMeter: DefaultBandwidthMeter
    private val cache: SimpleCache
    private var playerListener: Player.Listener? = null

    companion object {
        private const val TAG = "ExoPlayerManager"

        // Buffer settings (optimized for TV)
        private const val MIN_BUFFER_MS = 15000 // 15 seconds
        private const val MAX_BUFFER_MS = 50000 // 50 seconds
        private const val BUFFER_FOR_PLAYBACK_MS = 2500 // 2.5 seconds
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5000 // 5 seconds

        // Cache settings
        private const val CACHE_SIZE = 100L * 1024 * 1024 // 100 MB

        // Retry settings
        private const val MAX_RETRY_COUNT = 3
        private const val RETRY_DELAY_MS = 1000L
    }

    init {
        // Following SmartTube: Disable HTTP keep-alive for better streaming
        System.setProperty("http.keepAlive", "false")

        // Initialize track selector with adaptive strategy
        bandwidthMeter = DefaultBandwidthMeter.Builder(context).build()

        val trackSelectionFactory = AdaptiveTrackSelection.Factory()
        trackSelector = DefaultTrackSelector(context, trackSelectionFactory)

        // Set initial track selection parameters
        trackSelector.parameters = DefaultTrackSelector.ParametersBuilder(context)
            .setPreferredAudioLanguage("en")
            .setPreferredTextLanguage("en")
            .build()

        // Initialize cache
        val cacheDir = File(context.cacheDir, "media_cache")
        val evictor = LeastRecentlyUsedCacheEvictor(CACHE_SIZE)
        cache = SimpleCache(cacheDir, evictor)
    }

    /**
     * Create and configure ExoPlayer instance
     */
    fun createPlayer(): ExoPlayer {
        if (player != null) {
            Log.w(TAG, "Player already exists, releasing old instance")
            releasePlayer()
        }

        // Custom load control for better buffering
        val loadControl = buildLoadControl()

        // Renderers factory with hardware acceleration
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)

        player = ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .setRenderersFactory(renderersFactory)
            .setBandwidthMeter(bandwidthMeter)
            .build()
            .apply {
                // Set playback parameters
                playWhenReady = false

                // Add listener
                playerListener?.let { addListener(it) }

                // Error listener
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        handlePlayerError(error)
                    }
                })
            }

        Log.d(TAG, "ExoPlayer created successfully")
        return player!!
    }

    /**
     * Build optimized load control following SmartTube
     */
    private fun buildLoadControl(): LoadControl {
        return DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                MIN_BUFFER_MS,
                MAX_BUFFER_MS,
                BUFFER_FOR_PLAYBACK_MS,
                BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()
    }

    /**
     * Play media from URL with caching
     */
    fun playMedia(
        url: String,
        headers: Map<String, String> = emptyMap(),
        startPosition: Long = 0
    ) {
        val currentPlayer = player ?: createPlayer()

        try {
            val mediaSource = buildMediaSource(url, headers)

            currentPlayer.apply {
                setMediaSource(mediaSource)
                prepare()
                seekTo(startPosition)
                playWhenReady = true
            }

            Log.d(TAG, "Playing media: $url")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play media", e)
            throw e
        }
    }

    /**
     * Build media source with caching
     */
    private fun buildMediaSource(url: String, headers: Map<String, String>): MediaSource {
        val uri = Uri.parse(url)

        // HTTP data source with custom headers
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent("Topster/1.0")
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(10000)
            .apply {
                if (headers.isNotEmpty()) {
                    setDefaultRequestProperties(headers)
                }
            }

        // Cache data source wrapping HTTP
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        return when {
            url.contains(".m3u8", ignoreCase = true) -> {
                // HLS stream
                HlsMediaSource.Factory(cacheDataSourceFactory)
                    .setAllowChunklessPreparation(true)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
            else -> {
                // Progressive download
                ProgressiveMediaSource.Factory(cacheDataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri))
            }
        }
    }

    /**
     * Handle player errors with retry logic
     */
    private var retryCount = 0

    private fun handlePlayerError(error: PlaybackException) {
        Log.e(TAG, "Player error: ${error.errorCodeName}", error)

        when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> {
                // Retry on network errors
                if (retryCount < MAX_RETRY_COUNT) {
                    retryCount++
                    Log.d(TAG, "Retrying playback (attempt $retryCount/$MAX_RETRY_COUNT)")

                    // Retry after delay
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        player?.prepare()
                    }, RETRY_DELAY_MS * retryCount)
                } else {
                    Log.e(TAG, "Max retry count reached, giving up")
                    retryCount = 0
                }
            }
            else -> {
                Log.e(TAG, "Unrecoverable error: ${error.errorCodeName}")
                retryCount = 0
            }
        }
    }

    /**
     * Set player listener
     */
    fun setPlayerListener(listener: Player.Listener) {
        playerListener = listener
        player?.addListener(listener)
    }

    /**
     * Remove player listener
     */
    fun removePlayerListener(listener: Player.Listener) {
        player?.removeListener(listener)
    }

    /**
     * Get current player instance
     */
    fun getPlayer(): ExoPlayer? = player

    /**
     * Get track selector for quality/subtitle control
     */
    fun getTrackSelector(): DefaultTrackSelector = trackSelector

    /**
     * Release player resources
     */
    fun releasePlayer() {
        player?.let {
            it.stop()
            it.release()
        }
        player = null
        retryCount = 0
        Log.d(TAG, "Player released")
    }

    /**
     * Clear cache
     */
    fun clearCache() {
        try {
            cache.release()
            val cacheDir = File(context.cacheDir, "media_cache")
            cacheDir.deleteRecursively()
            Log.d(TAG, "Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }

    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            size = cache.cacheSpace,
            keys = cache.keys.count()
        )
    }
}

data class CacheStats(
    val size: Long,
    val keys: Int
)
