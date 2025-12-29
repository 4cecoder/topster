package com.topster.tv.ui.screens

import android.app.Application
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.topster.tv.TopsterApplication
import com.topster.tv.api.models.MediaItem
import com.topster.tv.ui.components.MediaCard
import com.topster.tv.ui.components.FuturisticButton
import com.topster.tv.ui.components.FuturisticTextField
import com.topster.tv.ui.components.FuturisticSectionHeader
import com.topster.tv.ui.components.CyberLoadingIndicator
import com.topster.tv.ui.components.AnimatedGradientBackground
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val scraper = (application as TopsterApplication).scraper

    var searchQuery by mutableStateOf("")
        private set

    var searchResults by mutableStateOf<List<MediaItem>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    fun updateQuery(query: String) {
        searchQuery = query
    }

    fun search() {
        if (searchQuery.isBlank()) return

        viewModelScope.launch {
            try {
                isLoading = true
                error = null

                Log.d("SearchScreen", "Searching for: $searchQuery")
                searchResults = scraper.search(searchQuery, page = 1)
                Log.d("SearchScreen", "Found ${searchResults.size} results")

                if (searchResults.isEmpty()) {
                    error = "No results found for \"$searchQuery\"\n\nTry different keywords or check spelling"
                }

                isLoading = false
            } catch (e: Exception) {
                Log.e("SearchScreen", "Search failed", e)
                error = "Search Error: ${e.message ?: "Failed to perform search"}\n\nPlease check your connection and try again"
                isLoading = false
            }
        }
    }

    fun clearResults() {
        searchResults = emptyList()
        error = null
    }
}

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = viewModel(),
    onMediaClick: (MediaItem) -> Unit = {},
    onBack: () -> Unit = {}
) {
    AnimatedGradientBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(48.dp)
        ) {
            // Back button
            FuturisticButton(
                text = "Back",
                onClick = onBack,
                icon = "←",
                modifier = Modifier.width(150.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            FuturisticSectionHeader(
                text = "🔍 Search Content"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Search input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FuturisticTextField(
                    value = viewModel.searchQuery,
                    onValueChange = { viewModel.updateQuery(it) },
                    placeholder = "Enter movie or TV show title...",
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { viewModel.search() }
                    )
                )

                FuturisticButton(
                    text = if (viewModel.isLoading) "Searching..." else "Search",
                    onClick = { viewModel.search() },
                    enabled = viewModel.searchQuery.isNotBlank() && !viewModel.isLoading,
                    icon = "🔎"
                )

                if (viewModel.searchResults.isNotEmpty()) {
                    FuturisticButton(
                        text = "Clear",
                        onClick = { viewModel.clearResults() },
                        icon = "✖"
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Results
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
                                text = "Searching for \"${viewModel.searchQuery}\"...",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                viewModel.error != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 48.sp,
                            color = Color(0xFFFF006E)
                        )
                        Text(
                            text = viewModel.error ?: "Unknown error",
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.bodyLarge,
                            fontSize = 16.sp
                        )
                    }
                }
                viewModel.searchResults.isNotEmpty() -> {
                    Column {
                        Text(
                            text = "Found ${viewModel.searchResults.size} results",
                            color = Color(0xFF06FFF0),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Grid of results
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 180.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(viewModel.searchResults) { media ->
                                MediaCard(
                                    media = media,
                                    onClick = { onMediaClick(media) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "🎬",
                                fontSize = 64.sp
                            )
                            Text(
                                text = "Enter a title to search for movies and TV shows",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
