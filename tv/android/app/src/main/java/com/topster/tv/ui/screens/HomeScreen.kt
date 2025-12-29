package com.topster.tv.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topster.tv.TopsterApplication
import com.topster.tv.api.models.MediaItem
import com.topster.tv.ui.components.MediaCard
import com.topster.tv.ui.components.FuturisticButton
import com.topster.tv.ui.components.FuturisticSectionHeader
import com.topster.tv.ui.components.CyberLoadingIndicator
import com.topster.tv.ui.components.AnimatedGradientBackground
import com.topster.tv.ui.components.HeroSection
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val scraper = (application as TopsterApplication).scraper

    var trending by mutableStateOf<List<MediaItem>>(emptyList())
        private set

    var recentMovies by mutableStateOf<List<MediaItem>>(emptyList())
        private set

    var recentTV by mutableStateOf<List<MediaItem>>(emptyList())
        private set

    var isLoading by mutableStateOf(false) // Start false since splash preloads
        private set

    var error by mutableStateOf<String?>(null)
        private set

    init {
        loadContent()
    }

    private fun loadContent() {
        viewModelScope.launch {
            try {
                // Only show loading if we don't have cached data
                if (trending.isEmpty() && recentMovies.isEmpty() && recentTV.isEmpty()) {
                    isLoading = true
                }
                error = null

                Log.d("HomeScreen", "Loading home content...")

                // Load content using the Kotlin scraper directly (will use cache if available)
                trending = scraper.getTrending()
                Log.d("HomeScreen", "Loaded ${trending.size} trending items")

                recentMovies = scraper.getRecent("movie")
                Log.d("HomeScreen", "Loaded ${recentMovies.size} recent movies")

                recentTV = scraper.getRecent("tv")
                Log.d("HomeScreen", "Loaded ${recentTV.size} recent TV shows")

                isLoading = false
            } catch (e: Exception) {
                Log.e("HomeScreen", "Failed to load content", e)
                error = "Network Error: ${e.message ?: "Failed to connect to server"}\n\nPlease check your internet connection."
                isLoading = false
            }
        }
    }

    fun refresh() {
        loadContent()
    }
}

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onMediaClick: (MediaItem) -> Unit = {},
    onSearchClick: () -> Unit = {}
) {
    AnimatedGradientBackground {
        when {
            viewModel.isLoading -> {
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
                            text = "Loading content...",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            viewModel.error != null -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "⚠️ ERROR",
                            color = Color(0xFFFF006E),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = viewModel.error ?: "Unknown error",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 16.sp
                        )
                        FuturisticButton(
                            text = "Retry",
                            onClick = { viewModel.refresh() },
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Hero section - Featured content
                    if (viewModel.trending.isNotEmpty()) {
                        item {
                            HeroSection(
                                mediaItem = viewModel.trending.first(),
                                onPlayClick = { onMediaClick(viewModel.trending.first()) }
                            )
                        }
                    }

                    // Compact search button - top right fixed position
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            FuturisticButton(
                                text = "Search",
                                onClick = onSearchClick,
                                compact = true
                            )
                        }
                    }

                    // Trending Now section - Large cards
                    if (viewModel.trending.size > 1) {
                        item {
                            Text(
                                text = "Trending Now",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 40.dp, vertical = 12.dp)
                            )
                        }
                        item {
                            MediaRow(
                                items = viewModel.trending.drop(1),
                                onMediaClick = onMediaClick,
                                large = true
                            )
                        }
                    }

                    // Recent Movies section
                    if (viewModel.recentMovies.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "New Movies",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 40.dp, vertical = 12.dp)
                            )
                        }
                        item {
                            MediaRow(viewModel.recentMovies, onMediaClick)
                        }
                    }

                    // Recent TV Shows section
                    if (viewModel.recentTV.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                text = "New Series",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 40.dp, vertical = 12.dp)
                            )
                        }
                        item {
                            MediaRow(viewModel.recentTV, onMediaClick)
                        }
                    }

                    // Bottom spacing
                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MediaRow(
    items: List<MediaItem>,
    onMediaClick: (MediaItem) -> Unit = {},
    large: Boolean = false
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(if (large) 16.dp else 14.dp)
    ) {
        items(items) { media ->
            MediaCard(
                media = media,
                onClick = { onMediaClick(media) },
                large = large
            )
        }
    }
}
