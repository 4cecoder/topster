package com.topster.tv.ui.screens

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.topster.tv.TopsterApplication
import com.topster.tv.api.models.Season
import com.topster.tv.api.models.Episode
import com.topster.tv.api.models.MediaItem
import com.topster.tv.api.models.VideoInfo
import com.topster.tv.database.HistoryManager
import com.topster.tv.prefs.PlayerTweaksData
import kotlinx.coroutines.launch

private const val TAG = "HistoryScreen"

/**
 * Improved History Screen with hierarchical navigation
 * Shows:
 * - Grouped shows (one entry per TV show)
 * - Movies individually
 * - Ability to drill down to seasons/episodes
 * - Continue watching from last position
 */

data class GroupedHistoryEntry(
    val id: String,
    val title: String,
    val posterImage: String?,
    val type: String,  // "tv" or "movie"
    val lastWatched: Long,
    val lastEpisodeInfo: String?,  // For TV shows: "S01E03 - Episode Name"
    val totalEpisodes: Int?,  // Total episodes watched for this show
)

class HistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val historyManager = (application as TopsterApplication).historyManager

    var isLoading by mutableStateOf(true)
    var error by mutableStateOf<String?>(null)
    var groupedHistory by mutableStateOf<List<GroupedHistoryEntry>>(emptyList())

    // Current level: history -> show details -> seasons -> episodes
    var currentScreen by mutableStateOf(HistoryScreen.GROUPED_HISTORY)
    var selectedShow by mutableStateOf<GroupedHistoryEntry?>(null)
    var selectedSeason by mutableStateOf<Season?>(null)

    fun loadHistory() {
        viewModelScope.launch {
            try {
                isLoading = true
                error = null
                currentScreen = HistoryScreen.GROUPED_HISTORY

                val allHistory = historyManager.getAllWatchHistory()

                // Group by show title for TV shows
                val showGroups = mutableMapOf<String, MutableList<com.topster.tv.database.HistoryEntry>>()

                for (entry in allHistory) {
                    if (entry.type == "tv") {
                        if (!showGroups.containsKey(entry.title)) {
                            showGroups[entry.title] = mutableListOf()
                        }
                        showGroups[entry.title]!!.add(entry)
                    } else {
                        // Movies don't need grouping
                    }
                }

                // Build grouped entries (one per show + all movies)
                val grouped = mutableListOf<GroupedHistoryEntry>()

                for ((title, episodes) in showGroups) {
                    val latestEpisode = episodes.maxByOrNull { it.lastWatched }
                    if (latestEpisode != null) {
                        val totalEpisodes = episodes.size
                        val episodeInfo = buildString {
                            if (latestEpisode!!.seasonNumber != null && latestEpisode!!.episodeNumber != null) {
                                append("S${String.format("%02d", latestEpisode.seasonNumber)}")
                                append("E${String.format("%02d", latestEpisode.episodeNumber)}")
                                if (latestEpisode.episodeTitle != null) {
                                    append(" - ${latestEpisode.episodeTitle}")
                                }
                            }
                        }

                        grouped.add(GroupedHistoryEntry(
                            id = title,
                            title = title,
                            posterImage = latestEpisode.posterImage,
                            type = "tv",
                            lastWatched = latestEpisode.lastWatched,
                            lastEpisodeInfo = episodeInfo,
                            totalEpisodes = totalEpisodes
                        ))
                    }
                }

                // Add movies
                for (entry in allHistory.filter { it.type == "movie" }) {
                    grouped.add(GroupedHistoryEntry(
                        id = entry.mediaId,
                        title = entry.title,
                        posterImage = entry.posterImage,
                        type = "movie",
                        lastWatched = entry.lastWatched,
                        lastEpisodeInfo = null,
                        totalEpisodes = null
                    ))
                }

                // Sort by last watched
                groupedHistory = grouped.sortedByDescending { it.lastWatched }

                isLoading = false
            } catch (e: Exception) {
                error = "Failed to load history: ${e.message}"
                isLoading = false
            }
        }
    }

    fun selectShow(entry: GroupedHistoryEntry) {
        selectedShow = entry
        currentScreen = HistoryScreen.SHOW_DETAILS
    }

    fun selectSeason(season: Season) {
        selectedSeason = season
        currentScreen = HistoryScreen.SEASON_LIST
    }

    fun selectEpisode(episode: Episode) {
        // Navigate to player with the selected episode
        // This will trigger navigation to the player screen
    }

    fun backToHistory() {
        selectedSeason = null
        selectedShow = null
        currentScreen = HistoryScreen.GROUPED_HISTORY
    }

    fun clearHistory() {
        viewModelScope.launch {
            historyManager.clearHistory()
            loadHistory()
        }
    }
}

enum class HistoryScreen {
    GROUPED_HISTORY,
    SHOW_DETAILS,
    SEASON_LIST,
    EPISODE_LIST
}

@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel(),
    onPlayMedia: (String, String, Episode?) -> Unit,
    navController: NavController
) {
    val context = LocalContext.current

    // Load history on screen enter
    LaunchedEffect(Unit) {
        viewModel.loadHistory()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B2A))
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
                        modifier = Modifier.size(48.dp)
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
                        fontSize = 16.sp
                    )
                    Button(
                        onClick = { viewModel.loadHistory() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00F5FF),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Retry", fontSize = 16.sp)
                    }
                }
            }

            viewModel.currentScreen == HistoryScreen.GROUPED_HISTORY -> {
                GroupedHistoryView(
                    groupedHistory = viewModel.groupedHistory,
                    onSelectShow = { entry ->
                        viewModel.selectShow(entry)
                    },
                    onClearHistory = { viewModel.clearHistory() },
                    onBack = { navController.popBackStack() }
                )
            }

            viewModel.selectedShow != null && viewModel.currentScreen == HistoryScreen.SHOW_DETAILS -> {
                ShowDetailsView(
                    show = viewModel.selectedShow!!,
                    onSelectSeason = { season ->
                        viewModel.selectSeason(season)
                    },
                    onBack = { viewModel.backToHistory() }
                )
            }

            viewModel.selectedSeason != null && viewModel.currentScreen == HistoryScreen.SEASON_LIST -> {
                SeasonListView(
                    season = viewModel.selectedSeason!!,
                    onSelectEpisode = { episode ->
                        viewModel.selectEpisode(episode)
                        onPlayMedia(viewModel.selectedShow!!.id, viewModel.selectedShow!!.title, episode)
                    },
                    onBack = { viewModel.backToHistory() }
                )
            }
        }
    }
}

@Composable
private fun GroupedHistoryView(
    groupedHistory: List<GroupedHistoryEntry>,
    onSelectShow: (GroupedHistoryEntry) -> Unit,
    onClearHistory: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Continue Watching",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onClearHistory,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF4444),
                        contentColor = Color.White
                    )
                ) {
                    Text("Clear History", fontSize = 14.sp)
                }

                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White
                    )
                ) {
                    Text("Back", fontSize = 14.sp)
                }
            }
        }

        // History list
        if (groupedHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("📺", fontSize = 64.sp)
                    Text(
                        text = "No watch history",
                        fontSize = 20.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(groupedHistory) { entry ->
                    HistoryEntryCard(entry = entry, onClick = { onSelectShow(entry) })
                }
            }
        }
    }
}

@Composable
private fun HistoryEntryCard(entry: GroupedHistoryEntry, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    isFocused = interactionSource.collectIsFocusedAsState().value

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(animateContentSize(label = "history_card_${entry.id}")),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isFocused) 8.dp else 4.dp,
        color = if (isFocused) Color(0xFF1E3A5F) else Color(0xFF16213E),
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Poster/thumbnail
            Box(
                modifier = Modifier
                    .size(80.dp, 120.dp)
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (entry.type == "tv") "📺" else "🎬",
                    fontSize = 32.sp
                )
            }

            // Info column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = entry.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2
                )

                if (entry.type == "tv") {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📺",
                            fontSize = 16.sp
                        )
                        Text(
                            text = entry.lastEpisodeInfo ?: "No episodes watched yet",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }

                    if (entry.totalEpisodes != null && entry.totalEpisodes!! > 0) {
                        Text(
                            text = "${entry.totalEpisodes} episodes in history",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShowDetailsView(
    show: GroupedHistoryEntry,
    onSelectSeason: (Season) -> Unit,
    onBack: () -> Unit
) {
    // TODO: Fetch seasons for this show
    val seasons = emptyList<Season>()

    Column(modifier = Modifier.fillMaxSize()) {
        // Header with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("← Back", fontSize = 14.sp)
            }

            Text(
                text = show.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        if (seasons.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading seasons...",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(seasons) { season ->
                    SeasonCard(season = season, onClick = { onSelectSeason(season) })
                }
            }
        }
    }
}

@Composable
private fun SeasonListView(
    season: Season,
    onSelectEpisode: (Episode) -> Unit,
    onBack: () -> Unit
) {
    // TODO: Fetch episodes for this season
    val episodes = emptyList<Episode>()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("← Back", fontSize = 14.sp)
            }

            Text(
                text = "Season ${season.number}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
        }

        if (episodes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(episodes) { episode ->
                    EpisodeCard(episode = episode, onClick = { onSelectEpisode(episode) })
                }
            }
        }
    }
}

@Composable
private fun SeasonCard(season: Season, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    isFocused = interactionSource.collectIsFocusedAsState().value

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .then(animateContentSize(label = "season_${season.number}")),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = if (isFocused) 8.dp else 4.dp,
        color = if (isFocused) Color(0xFF1E3A5F) else Color(0xFF16213E),
        interactionSource = interactionSource
    ) {
        Text(
            text = "Season ${season.number}",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun EpisodeCard(episode: Episode, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        color = Color(0xFF16213E)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "E${String.format("%02d", episode.number)}: ${episode.title}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White,
                maxLines = 2
            )
        }
    }
}
