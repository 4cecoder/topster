package com.topster.tv.ui.screens

import android.app.Application
import android.os.Parcelable
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.topster.tv.TopsterApplication
import com.topster.tv.api.models.Episode
import com.topster.tv.api.models.MediaItem
import com.topster.tv.api.models.Season
import com.topster.tv.scraper.IMDbMetadata
import com.topster.tv.ui.components.FuturisticButton
import com.topster.tv.ui.components.FuturisticSectionHeader
import com.topster.tv.ui.components.FuturisticCard
import com.topster.tv.ui.components.CyberLoadingIndicator
import com.topster.tv.ui.components.AnimatedGradientBackground
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@Parcelize
data class PlaybackItem(
    val mediaId: String,
    val title: String,
    val type: String, // "movie" or "tv"
    val episodeId: String? = null,
    val episodeTitle: String? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
    val posterImage: String? = null
) : Parcelable

class DetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val scraper = (application as TopsterApplication).scraper
    private val imdbScraper = (application as TopsterApplication).imdbScraper

    var mediaId by mutableStateOf("")
        private set

    var mediaType by mutableStateOf("movie")
        private set

    var title by mutableStateOf("")
        private set

    var imdbData by mutableStateOf<IMDbMetadata?>(null)
        private set

    var seasons by mutableStateOf<List<Season>>(emptyList())
        private set

    var selectedSeason by mutableStateOf<Season?>(null)
        private set

    var episodes by mutableStateOf<List<Episode>>(emptyList())
        private set

    var isLoading by mutableStateOf(true)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun loadDetails(id: String, type: String, mediaTitle: String, mediaYear: String?) {
        mediaId = id
        mediaType = type
        title = mediaTitle

        // Load IMDb metadata in background
        loadIMDbData(mediaTitle, mediaYear)

        if (type == "tv") {
            loadSeasons()
        } else {
            isLoading = false
        }
    }

    private fun loadIMDbData(title: String, year: String?) {
        viewModelScope.launch {
            try {
                Log.d("DetailsScreen", "Loading IMDb data for: $title ($year)")
                imdbData = imdbScraper.searchAndGetMetadata(title, year)
                Log.d("DetailsScreen", "IMDb data loaded: ${imdbData?.title}")
            } catch (e: Exception) {
                Log.e("DetailsScreen", "Failed to load IMDb data", e)
            }
        }
    }

    private fun loadSeasons() {
        viewModelScope.launch {
            try {
                isLoading = true
                error = null

                Log.d("DetailsScreen", "Loading seasons for mediaId: $mediaId")
                seasons = scraper.getSeasons(mediaId)
                Log.d("DetailsScreen", "Loaded ${seasons.size} seasons")

                if (seasons.isNotEmpty()) {
                    // Auto-select first season
                    selectSeason(seasons[0])
                } else {
                    error = "No seasons found for this TV show"
                }

                isLoading = false
            } catch (e: Exception) {
                Log.e("DetailsScreen", "Failed to load seasons", e)
                error = "Failed to load seasons: ${e.message ?: "Unknown error"}"
                isLoading = false
            }
        }
    }

    fun selectSeason(season: Season) {
        selectedSeason = season
        loadEpisodes(season.id)
    }

    private fun loadEpisodes(seasonId: String) {
        viewModelScope.launch {
            try {
                isLoading = true
                error = null

                Log.d("DetailsScreen", "Loading episodes for seasonId: $seasonId")
                episodes = scraper.getEpisodes(seasonId)
                Log.d("DetailsScreen", "Loaded ${episodes.size} episodes")

                if (episodes.isEmpty()) {
                    error = "No episodes found for this season"
                }

                isLoading = false
            } catch (e: Exception) {
                Log.e("DetailsScreen", "Failed to load episodes", e)
                error = "Failed to load episodes: ${e.message ?: "Unknown error"}"
                isLoading = false
            }
        }
    }
}

@Composable
fun DetailsScreen(
    mediaItem: MediaItem,
    viewModel: DetailsViewModel = viewModel(),
    onPlayClick: (PlaybackItem) -> Unit = {},
    onBack: () -> Unit = {}
) {
    // Load details on first composition
    LaunchedEffect(mediaItem.id) {
        viewModel.loadDetails(mediaItem.id, mediaItem.type, mediaItem.title, mediaItem.year)
    }

    AnimatedGradientBackground {
        when {
            viewModel.isLoading && viewModel.seasons.isEmpty() && viewModel.episodes.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CyberLoadingIndicator()
                        Text(
                            text = "Loading details...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            viewModel.error != null && viewModel.seasons.isEmpty() -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(48.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "⚠️ ERROR",
                        color = Color(0xFFFF006E),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = viewModel.error ?: "Unknown error",
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    FuturisticButton(
                        text = "Go Back",
                        onClick = onBack,
                        icon = "←"
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(48.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Back button
                    item {
                        FuturisticButton(
                            text = "Back",
                            onClick = onBack,
                            icon = "←",
                            modifier = Modifier.width(150.dp)
                        )
                    }

                    // Title
                    item {
                        FuturisticSectionHeader(
                            text = viewModel.title
                        )
                    }

                    // Type badge and year
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (mediaItem.type == "tv") "📺 TV Series" else "🎬 Movie",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF06FFF0),
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (mediaItem.year != null) {
                                Text(
                                    text = "• ${mediaItem.year}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // IMDb metadata
                    viewModel.imdbData?.let { imdb ->
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (imdb.rating != "N/A") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "⭐",
                                            fontSize = 20.sp
                                        )
                                        Text(
                                            text = "${imdb.rating}/10",
                                            color = Color(0xFFFFD700),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp
                                        )
                                        Text(
                                            text = "IMDb Rating",
                                            color = Color.White.copy(alpha = 0.7f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }

                                if (imdb.plot != "No plot available") {
                                    Text(
                                        text = imdb.plot,
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    )
                                }

                                if (imdb.genres.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        imdb.genres.take(4).forEach { genre ->
                                            Text(
                                                text = genre,
                                                color = Color(0xFFB24BF3),
                                                fontSize = 13.sp,
                                                modifier = Modifier
                                                    .background(
                                                        Color(0xFF1E2139),
                                                        androidx.compose.foundation.shape.RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Movie: Single play button
                    if (mediaItem.type == "movie") {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            FuturisticButton(
                                text = "Play Movie",
                                onClick = {
                                    onPlayClick(PlaybackItem(
                                        mediaId = mediaItem.id,
                                        title = viewModel.title,
                                        type = "movie"
                                    ))
                                },
                                icon = "▶",
                                modifier = Modifier.height(60.dp)
                            )
                        }
                    }

                    // TV Show: Season selector and episodes
                    if (mediaItem.type == "tv") {
                        // Season selector
                        if (viewModel.seasons.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "🎬 Seasons",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFF06FFF0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            item {
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(viewModel.seasons) { season ->
                                        FuturisticButton(
                                            text = season.title,
                                            onClick = { viewModel.selectSeason(season) },
                                            gradient = if (viewModel.selectedSeason?.id == season.id)
                                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                    listOf(Color(0xFFB24BF3), Color(0xFFFF006E))
                                                )
                                            else
                                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                                    listOf(Color(0xFF374151), Color(0xFF4B5563))
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        // Episodes list
                        if (viewModel.episodes.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "📺 Episodes - ${viewModel.selectedSeason?.title ?: ""}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color(0xFF06FFF0),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp
                                )
                            }

                            items(viewModel.episodes.size) { index ->
                                val episode = viewModel.episodes[index]
                                EpisodeCard(
                                    episode = episode,
                                    seasonNumber = viewModel.selectedSeason?.number ?: 1,
                                    onClick = {
                                        onPlayClick(PlaybackItem(
                                            mediaId = mediaItem.id,
                                            title = viewModel.title,
                                            type = "tv",
                                            episodeId = episode.id,
                                            episodeTitle = episode.title,
                                            seasonNumber = viewModel.selectedSeason?.number,
                                            episodeNumber = episode.number,
                                            posterImage = mediaItem.image
                                        ))
                                    }
                                )
                            }
                        }

                        // Loading more episodes
                        if (viewModel.isLoading && viewModel.episodes.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        CyberLoadingIndicator()
                                        Text(
                                            text = "Loading episodes...",
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeCard(
    episode: Episode,
    seasonNumber: Int,
    onClick: () -> Unit
) {
    FuturisticCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(90.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "S${seasonNumber} E${episode.number}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF06FFF0),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = "▶",
                fontSize = 28.sp,
                color = Color(0xFFB24BF3)
            )
        }
    }
}
