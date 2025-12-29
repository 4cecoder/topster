package com.topster.tv.ui.screens

import android.app.Activity
import android.app.Application
import android.content.pm.ActivityInfo
import android.util.Log
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import com.topster.tv.TopsterApplication
import com.topster.tv.api.models.Episode
import com.topster.tv.api.models.Season
import com.topster.tv.api.models.VideoInfo

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "PlayerScreen"

private fun formatTime(milliseconds: Long): String {
    if (milliseconds < 0) return "00:00"
    val totalSeconds = milliseconds / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

class PlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val scraper = (application as TopsterApplication).scraper
    private val historyManager = (application as TopsterApplication).historyManager

    var isLoading by mutableStateOf(true)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var currentItem by mutableStateOf<PlaybackItem?>(null)
        private set

    var videoInfo by mutableStateOf<VideoInfo?>(null)
        private set

    var queue by mutableStateOf<List<PlaybackItem>>(emptyList())
        private set

    var currentQueueIndex by mutableStateOf(0)
        private set

    var player: ExoPlayer? = null
        private set

    var seasons by mutableStateOf<List<Season>>(emptyList())
        private set

    var episodes by mutableStateOf<List<Episode>>(emptyList())
        private set

    fun loadVideo(item: PlaybackItem) {
        viewModelScope.launch {
            try {
                isLoading = true
                error = null
                currentItem = item

                Log.d(TAG, "Loading video for: ${item.title}")
                Log.d(TAG, "Media ID: ${item.mediaId}, Episode ID: ${item.episodeId}, Type: ${item.type}")

                // For TV shows, load seasons and episodes if not already in queue
                if (item.type == "tv" && item.episodeId != null && queue.isEmpty()) {
                    loadTVShowQueue(item)
                }

                // Get video sources
                val isEpisode = item.episodeId != null
                val id = item.episodeId ?: item.mediaId
                Log.d(TAG, "Fetching video sources for ID: $id (isEpisode: $isEpisode)")
                val sources = scraper.getVideoSources(id, isEpisode)
                Log.d(TAG, "Found ${sources.size} video sources")

                if (sources.isEmpty()) {
                    throw Exception("No video sources found\n\nThe video extractors may be experiencing issues. Check logs for details.")
                }

                // Get the first working source
                val source = sources.firstOrNull { it.sources.isNotEmpty() }
                if (source == null) {
                    Log.e(TAG, "All sources are empty: ${sources.map { it.provider }}")
                    throw Exception("No playable sources found\n\nProviders checked: ${sources.map { it.provider }.joinToString(", ")}")
                }

                videoInfo = source.sources.firstOrNull()

                Log.d(TAG, "Selected provider: ${source.provider}")
                Log.d(TAG, "Video URL: ${videoInfo?.url}")
                Log.d(TAG, "Referer: ${videoInfo?.referer}")
                Log.d(TAG, "Quality: ${videoInfo?.quality}")

                isLoading = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load video", e)
                error = "Video Load Error:\n${e.message ?: "Unknown error"}\n\nPlease check your connection or try a different title."
                isLoading = false
            }
        }
    }

    private suspend fun loadTVShowQueue(currentItem: PlaybackItem) {
        try {
            // Load all seasons
            seasons = scraper.getSeasons(currentItem.mediaId)

            // Find current season
            val currentSeason = seasons.find { it.number == currentItem.seasonNumber }
            if (currentSeason != null) {
                // Load episodes for current season
                episodes = scraper.getEpisodes(currentSeason.id)

                // Build queue from current episode onwards
                val currentEpisodeIndex = episodes.indexOfFirst { it.id == currentItem.episodeId }
                if (currentEpisodeIndex != -1) {
                    val queueItems = mutableListOf<PlaybackItem>()

                    // Add remaining episodes from current season
                    for (i in currentEpisodeIndex until episodes.size) {
                        val ep = episodes[i]
                        queueItems.add(PlaybackItem(
                            mediaId = currentItem.mediaId,
                            title = currentItem.title,
                            type = "tv",
                            episodeId = ep.id,
                            episodeTitle = ep.title,
                            seasonNumber = currentSeason.number,
                            episodeNumber = ep.number,
                            posterImage = currentItem.posterImage
                        ))
                    }

                    // Add episodes from next seasons
                    val nextSeasonIndex = seasons.indexOfFirst { it.number == currentSeason.number } + 1
                    for (i in nextSeasonIndex until seasons.size) {
                        val season = seasons[i]
                        val seasonEpisodes = scraper.getEpisodes(season.id)
                        seasonEpisodes.forEach { ep ->
                            queueItems.add(PlaybackItem(
                                mediaId = currentItem.mediaId,
                                title = currentItem.title,
                                type = "tv",
                                episodeId = ep.id,
                                episodeTitle = ep.title,
                                seasonNumber = season.number,
                                episodeNumber = ep.number,
                                posterImage = currentItem.posterImage
                            ))
                        }
                    }

                    queue = queueItems
                    currentQueueIndex = 0
                    Log.d(TAG, "Queue loaded with ${queue.size} episodes")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load TV show queue", e)
        }
    }

    fun playNext() {
        if (queue.isNotEmpty() && currentQueueIndex < queue.size - 1) {
            currentQueueIndex++
            val nextItem = queue[currentQueueIndex]
            loadVideo(nextItem)
        }
    }

    fun hasPrevious(): Boolean = queue.isNotEmpty() && currentQueueIndex > 0

    fun hasNext(): Boolean = queue.isNotEmpty() && currentQueueIndex < queue.size - 1

    fun playPrevious() {
        if (hasPrevious()) {
            currentQueueIndex--
            val prevItem = queue[currentQueueIndex]
            loadVideo(prevItem)
        }
    }

    @OptIn(UnstableApi::class)
    fun initializePlayer(context: android.content.Context): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            // Auto-play next episode
                            if (hasNext()) {
                                playNext()
                            }
                        }
                    }
                })
            }
        }
        return player!!
    }

    @OptIn(UnstableApi::class)
    fun playVideo(player: ExoPlayer, videoInfo: VideoInfo) {
        try {
            // Create HTTP data source with required headers
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setDefaultRequestProperties(mapOf(
                    "Referer" to (videoInfo.referer ?: ""),
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Origin" to (videoInfo.referer ?: "")
                ))

            // Create media item with subtitles
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(videoInfo.url)

            // Add subtitles
            videoInfo.subtitles.forEach { subtitle ->
                mediaItemBuilder.setSubtitleConfigurations(
                    listOf(MediaItem.SubtitleConfiguration.Builder(
                        android.net.Uri.parse(subtitle.url)
                    )
                        .setMimeType(MimeTypes.TEXT_VTT)
                        .setLanguage(subtitle.lang)
                        .setLabel(subtitle.label)
                        .build())
                )
            }

            val mediaItem = mediaItemBuilder.build()

            // Create HLS media source
            val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)

            player.setMediaSource(mediaSource)
            player.prepare()
            player.playWhenReady = true

            Log.d(TAG, "Player started with URL: ${videoInfo.url}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play video", e)
            error = e.message
        }
    }

    fun saveProgress() {
        viewModelScope.launch {
            try {
                val item = currentItem ?: return@launch
                val p = player ?: return@launch

                if (p.duration > 0) {
                    historyManager.updateWatchHistory(
                        mediaId = item.mediaId,
                        title = item.title,
                        type = item.type,
                        url = "", // We don't have the URL here
                        posterImage = item.posterImage,
                        episodeId = item.episodeId,
                        episodeTitle = item.episodeTitle,
                        seasonNumber = item.seasonNumber,
                        episodeNumber = item.episodeNumber,
                        position = p.currentPosition,
                        duration = p.duration
                    )
                    Log.d(TAG, "Progress saved: ${p.currentPosition}/${p.duration}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save progress", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player?.release()
        player = null
    }
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    playbackItem: PlaybackItem,
    viewModel: PlayerViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // Initialize player
    val player = remember { viewModel.initializePlayer(context) }

    // Load video on first composition
    LaunchedEffect(playbackItem) {
        viewModel.loadVideo(playbackItem)
    }

    // Play video when videoInfo is available
    LaunchedEffect(viewModel.videoInfo) {
        viewModel.videoInfo?.let { info ->
            viewModel.playVideo(player, info)
        }
    }

    // Save progress periodically
    LaunchedEffect(player) {
        while (true) {
            delay(5000) // Save every 5 seconds
            viewModel.saveProgress()
        }
    }

    // Auto-hide controls
    var controlsVisible by remember { mutableStateOf(true) }
    var hideJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Track player state for UI updates
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var currentPosition by remember { mutableStateOf(player.currentPosition) }
    var duration by remember { mutableStateOf(player.duration) }

    fun showControlsTemporarily() {
        controlsVisible = true
        hideJob?.cancel()
        hideJob = viewModel.viewModelScope.launch {
            delay(3000) // Hide after 3 seconds of inactivity
            controlsVisible = false
        }
    }

    LaunchedEffect(Unit) {
        showControlsTemporarily()
    }

    // Update player state periodically for UI
    LaunchedEffect(player) {
        while (true) {
            isPlaying = player.isPlaying
            currentPosition = player.currentPosition
            duration = player.duration
            delay(100) // Update 10 times per second for smooth progress bar
        }
    }

    // Lock to landscape orientation
    DisposableEffect(Unit) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            viewModel.saveProgress()
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        when {
            viewModel.isLoading -> {
                Box(
                    modifier = Modifier.align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            viewModel.error != null -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "❌",
                        fontSize = 48.sp,
                        color = Color(0xFFFF4444)
                    )
                    Text(
                        text = viewModel.error ?: "Unknown error",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = onBack,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Go Back", fontSize = 16.sp)
                    }
                }
            }
            else -> {
                // Video player
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = false  // Use custom controls instead
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { showControlsTemporarily() }
                )

                // Auto-hiding controls overlay
                AnimatedVisibility(
                    visible = controlsVisible,
                    enter = fadeIn(animationSpec = tween(200)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    ) {
                        // Streamlined playback controls - center
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rewind 10 seconds
                            IconButton(
                                onClick = {
                                    player.seekTo((player.currentPosition - 10000).coerceAtLeast(0))
                                    showControlsTemporarily()
                                },
                                modifier = Modifier.size(60.dp)
                            ) {
                                Text("⏪", fontSize = 32.sp, color = Color.White)
                            }

                            // Play/Pause button (larger for main action)
                            IconButton(
                                onClick = {
                                    if (isPlaying) {
                                        player.pause()
                                    } else {
                                        player.play()
                                    }
                                    showControlsTemporarily()
                                },
                                modifier = Modifier.size(80.dp)
                            ) {
                                Text(
                                    if (isPlaying) "⏸️" else "▶️",
                                    fontSize = 40.sp,
                                    color = Color.White
                                )
                            }

                            // Fast forward 10 seconds
                            IconButton(
                                onClick = {
                                    player.seekTo((player.currentPosition + 10000).coerceAtMost(player.duration))
                                    showControlsTemporarily()
                                },
                                modifier = Modifier.size(60.dp)
                            ) {
                                Text("⏩", fontSize = 32.sp, color = Color.White)
                            }
                        }

                        // Minimal episode navigation - top right
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Previous episode button
                            if (viewModel.hasPrevious()) {
                                IconButton(
                                    onClick = {
                                        viewModel.playPrevious()
                                        showControlsTemporarily()
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Text("⏮", fontSize = 20.sp, color = Color.White)
                                }
                            }

                            // Next episode button
                            if (viewModel.hasNext()) {
                                IconButton(
                                    onClick = {
                                        viewModel.playNext()
                                        showControlsTemporarily()
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Text("⏭", fontSize = 20.sp, color = Color.White)
                                }
                            }
                        }

                        // Progress bar and time - bottom (streamlined)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            // Progress bar
                            Slider(
                                value = if (duration > 0) {
                                    currentPosition.toFloat() / duration.toFloat()
                                } else 0f,
                                onValueChange = { value ->
                                    player.seekTo((value * duration).toLong())
                                    showControlsTemporarily()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.White,
                                    inactiveTrackColor = Color.White.copy(alpha = 0.4f)
                                )
                            )

                            // Time display
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(currentPosition),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = formatTime(duration),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Back button - top left (minimal)
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                                .size(40.dp)
                        ) {
                            Text("←", fontSize = 24.sp, color = Color.White)
                        }
                    }
                }

                // Minimal queue info
                if (viewModel.queue.isNotEmpty()) {
                    Text(
                        text = "${viewModel.currentQueueIndex + 1}/${viewModel.queue.size}",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp)
                    )
                }
            }
        }
    }
}
