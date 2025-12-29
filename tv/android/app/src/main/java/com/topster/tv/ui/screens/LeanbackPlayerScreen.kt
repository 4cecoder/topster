package com.topster.tv.ui.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.Log
import android.util.Rational
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
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
import com.topster.tv.prefs.PlayerTweaksData
import com.topster.tv.player.actions.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "LeanbackPlayerScreen"

/**
 * ViewModel for the new Leanback-based player
 */
class LeanbackPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val scraper = (application as TopsterApplication).scraper
    private val historyManager = (application as TopsterApplication).historyManager
    private val tweaksData = PlayerTweaksData

    var isLoading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var currentItem by mutableStateOf<PlaybackItem?>(null)
    var videoInfo by mutableStateOf<VideoInfo?>(null)
    var player: ExoPlayer? = null

    // Playback state
    var isPlaying by mutableStateOf(false)
    var currentPosition by mutableStateOf(0L)
    var duration by mutableStateOf(0L)
    var isControlsVisible by mutableStateOf(true)

    // Actions for Leanback
    val playPauseAction = mutableStateOf<PlayPauseAction?>(null)
    val skipNextAction = mutableStateOf<SkipNextAction?>(null)
    val skipPreviousAction = mutableStateOf<SkipPreviousAction?>(null)
    val fastForwardAction = mutableStateOf<FastForwardAction?>(null)
    val rewindAction = mutableStateOf<RewindAction?>(null)
    val videoSpeedAction = mutableStateOf<VideoSpeedAction?>(null)
    val backAction = mutableStateOf<BackAction?>(null)

    // Queue management
    var queue by mutableStateOf<List<PlaybackItem>>(emptyList())
    var currentQueueIndex by mutableStateOf(0)

    fun initActions(context: android.content.Context) {
        playPauseAction.value = PlayPauseAction(context, false)
        skipNextAction.value = SkipNextAction(context)
        skipPreviousAction.value = SkipPreviousAction(context)
        fastForwardAction.value = FastForwardAction(context)
        rewindAction.value = RewindAction(context)
        videoSpeedAction.value = VideoSpeedAction(context)
        backAction.value = BackAction(context)
    }

    fun loadVideo(item: PlaybackItem) {
        viewModelScope.launch {
            try {
                isLoading = true
                error = null
                currentItem = item

                Log.d(TAG, "Loading video for: ${item.title}")

                val isEpisode = item.episodeId != null
                val id = item.episodeId ?: item.mediaId

                val sources = scraper.getVideoSources(id, isEpisode)
                Log.d(TAG, "Found ${sources.size} video sources")

                if (sources.isEmpty()) {
                    throw Exception("No video sources found")
                }

                val source = sources.firstOrNull { it.sources.isNotEmpty() }
                if (source == null) {
                    throw Exception("No playable sources found")
                }

                videoInfo = source.sources.firstOrNull()

                isLoading = false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load video", e)
                error = "Video Load Error: ${e.message ?: "Unknown error"}"
                isLoading = false
            }
        }
    }

    @OptIn(UnstableApi::class)
    fun initializePlayer(context: android.content.Context): ExoPlayer {
        if (player == null) {
            player = ExoPlayer.Builder(context).build().apply {
                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            if (queue.isNotEmpty() && currentQueueIndex < queue.size - 1) {
                                playNext()
                            }
                        }
                    }
                    override fun onIsPlayingChanged(playing: Boolean) {
                        isPlaying = playing
                        playPauseAction.value?.setPlayingState(playing)
                    }
                })
            }
        }
        return player!!
    }

    @OptIn(UnstableApi::class)
    fun playVideo(player: ExoPlayer, videoInfo: VideoInfo) {
        try {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setDefaultRequestProperties(mapOf(
                    "Referer" to (videoInfo.referer ?: ""),
                    "Accept" to "*/*",
                    "Accept-Language" to "en-US,en;q=0.9",
                    "Origin" to (videoInfo.referer ?: "")
                ))

            val mediaItemBuilder = MediaItem.Builder()
                .setUri(videoInfo.url)

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

            val mediaSource = HlsMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)

            player.setMediaSource(mediaSource)
            player.prepare()
            player.playWhenReady = true

            Log.d(TAG, "Player started")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play video", e)
            error = e.message
        }
    }

    fun togglePlayPause() {
        player?.let {
            if (isPlaying) {
                it.pause()
            } else {
                it.play()
            }
        }
    }

    fun seekForward() {
        val seekTime = tweaksData.getSeekInterval() * 1000L
        player?.seekTo((currentPosition + seekTime).coerceAtMost(duration))
        showControls()
    }

    fun seekBackward() {
        val seekTime = tweaksData.getSeekInterval() * 1000L
        player?.seekTo((currentPosition - seekTime).coerceAtLeast(0))
        showControls()
    }

    fun playNext() {
        if (queue.isNotEmpty() && currentQueueIndex < queue.size - 1) {
            currentQueueIndex++
            val nextItem = queue[currentQueueIndex]
            loadVideo(nextItem)
        }
    }

    fun playPrevious() {
        if (currentQueueIndex > 0) {
            currentQueueIndex--
            val prevItem = queue[currentQueueIndex]
            loadVideo(prevItem)
        }
    }

    fun showControls() {
        isControlsVisible = true
        viewModelScope.launch {
            delay(tweaksData.getAutoHideDelay().toLong())
            isControlsVisible = false
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
                        url = "",
                        posterImage = item.posterImage,
                        episodeId = item.episodeId,
                        episodeTitle = item.episodeTitle,
                        seasonNumber = item.seasonNumber,
                        episodeNumber = item.episodeNumber,
                        position = p.currentPosition,
                        duration = p.duration
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save progress", e)
            }
        }
    }

    fun handleKeyEvent(keyCode: Int, activity: Activity): Boolean {
        val seekTime = tweaksData.getSeekInterval() * 1000L

        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER -> {
                togglePlayPause()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                togglePlayPause()
                true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                seekForward()
                true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                seekBackward()
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                seekForward()
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                seekBackward()
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (tweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_NEXT)) {
                    playNext()
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (tweaksData.isPlayerButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_PREVIOUS)) {
                    playPrevious()
                }
                true
            }
            KeyEvent.KEYCODE_BACK -> {
                saveProgress()
                player?.release()
                activity.finish()
                true
            }
            else -> false
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
fun LeanbackPlayerScreen(
    playbackItem: PlaybackItem,
    viewModel: LeanbackPlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    LaunchedEffect(Unit) {
        viewModel.initActions(context)
        viewModel.loadVideo(playbackItem)
    }

    LaunchedEffect(viewModel.videoInfo) {
        val player = viewModel.initializePlayer(context)
        viewModel.videoInfo?.let { info ->
            viewModel.playVideo(player, info)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            viewModel.player?.let {
                viewModel.isPlaying = it.isPlaying
                viewModel.currentPosition = it.currentPosition
                viewModel.duration = it.duration
            }
            delay(100)
        }
    }

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
                    Text("❌", fontSize = 48.sp, color = Color(0xFFFF4444))
                    Text(
                        text = viewModel.error ?: "Unknown error",
                        color = Color.White,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { activity?.finish() }) {
                        Text("Back", fontSize = 16.sp)
                    }
                }
            }
            else -> {
                // Video player
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = viewModel.player
                            useController = false
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            viewModel.showControls()
                        }
                )

                // Minimal controls overlay (SmartTube-inspired)
                if (viewModel.isControlsVisible) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Black.copy(alpha = 0.7f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                )
                            )
                    ) {
                        // Center controls
                        Row(
                            modifier = Modifier
                                .align(Alignment.Center),
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Rewind
                            if (viewModel.rewindAction.value != null) {
                                val interactionSource = remember { MutableInteractionSource() }
                                val isFocused by interactionSource.collectIsFocusedAsState()

                                Box(
                                    modifier = Modifier
                                        .scale(if (isFocused) 1.1f else 1f)
                                ) {
                                    Surface(
                                        onClick = { viewModel.seekBackward() },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isFocused) Color(0xFF00F5FF) else Color.White.copy(alpha = 0.2f),
                                        interactionSource = interactionSource
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("⏪", fontSize = 28.sp, color = Color.White)
                                        }
                                    }
                                }
                            }

                            // Play/Pause (main action)
                            val interactionSource = remember { MutableInteractionSource() }
                            val isFocused by interactionSource.collectIsFocusedAsState()

                            Box(
                                modifier = Modifier.scale(if (isFocused) 1.15f else 1f)
                            ) {
                                Surface(
                                    onClick = { viewModel.togglePlayPause() },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isFocused) Color(0xFF00F5FF) else Color.White.copy(alpha = 0.2f),
                                    interactionSource = interactionSource
                                ) {
                                    Box(
                                        modifier = Modifier
                                                .size(80.dp)
                                                .padding(16.dp),
                                            contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            if (viewModel.isPlaying) "⏸️" else "▶️",
                                            fontSize = 40.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                            }

                            // Fast forward
                            if (viewModel.fastForwardAction.value != null) {
                                val interactionSource = remember { MutableInteractionSource() }
                                val isFocused by interactionSource.collectIsFocusedAsState()

                                Box(
                                    modifier = Modifier
                                        .scale(if (isFocused) 1.1f else 1f)
                                ) {
                                    Surface(
                                        onClick = { viewModel.seekForward() },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isFocused) Color(0xFF00F5FF) else Color.White.copy(alpha = 0.2f),
                                        interactionSource = interactionSource
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(56.dp)
                                                .padding(12.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("⏩", fontSize = 28.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // Episode navigation (top-right)
                        if (viewModel.currentQueueIndex > 0 ||
                            (viewModel.queue.isNotEmpty() && viewModel.currentQueueIndex < viewModel.queue.size - 1)) {

                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (viewModel.currentQueueIndex > 0) {
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isFocused by interactionSource.collectIsFocusedAsState()

                                    Box(
                                        modifier = Modifier
                                            .scale(if (isFocused) 1.1f else 1f)
                                    ) {
                                        Surface(
                                            onClick = { viewModel.playPrevious() },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isFocused) Color(0xFF00F5FF) else Color.White.copy(alpha = 0.2f),
                                            interactionSource = interactionSource
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                        .size(44.dp)
                                                        .padding(8.dp),
                                                    contentAlignment = Alignment.Center
                                            ) {
                                                Text("⏮", fontSize = 20.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }

                                if (viewModel.queue.isNotEmpty() && viewModel.currentQueueIndex < viewModel.queue.size - 1) {
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isFocused by interactionSource.collectIsFocusedAsState()

                                    Box(
                                        modifier = Modifier
                                            .scale(if (isFocused) 1.1f else 1f)
                                    ) {
                                        Surface(
                                            onClick = { viewModel.playNext() },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isFocused) Color(0xFF00F5FF) else Color.White.copy(alpha = 0.2f),
                                            interactionSource = interactionSource
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                        .size(44.dp)
                                                        .padding(8.dp),
                                                    contentAlignment = Alignment.Center
                                            ) {
                                                Text("⏭", fontSize = 20.sp, color = Color.White)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Progress bar (bottom)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 16.dp)
                        ) {
                            Slider(
                                value = if (viewModel.duration > 0) {
                                    viewModel.currentPosition.toFloat() / viewModel.duration.toFloat()
                                } else 0f,
                                onValueChange = { value ->
                                    viewModel.player?.seekTo((value * viewModel.duration).toLong())
                                    viewModel.showControls()
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
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatTime(viewModel.currentPosition),
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = formatTime(viewModel.duration),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Back button (top-left)
                        val interactionSource = remember { MutableInteractionSource() }
                        val isFocused by interactionSource.collectIsFocusedAsState()

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(16.dp)
                        ) {
                            Surface(
                                onClick = { activity?.finish() },
                                shape = RoundedCornerShape(8.dp),
                                color = if (isFocused) Color(0xFF00F5FF) else Color.White.copy(alpha = 0.2f),
                                interactionSource = interactionSource
                            ) {
                                Box(
                                    modifier = Modifier
                                            .size(40.dp)
                                            .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("←", fontSize = 24.sp, color = Color.White)
                                }
                            }
                        }
                    }

                    // Queue indicator
                    if (viewModel.queue.isNotEmpty()) {
                        Text(
                            text = "${viewModel.currentQueueIndex + 1}/${viewModel.queue.size}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

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
