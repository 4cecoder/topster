package com.topster.tv.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import com.topster.tv.network.MediaItem

/**
 * Optimized TV grid with proper focus management and recycling
 * Following SmartTube's RecyclerView patterns
 */
@Composable
fun TVMediaGrid(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    columns: Int = 6
) {
    val gridState = rememberLazyGridState()
    val focusRequester = remember { FocusRequester() }

    // Auto-focus first item when grid appears
    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            try {
                focusRequester.requestFocus()
            } catch (e: Exception) {
                // Ignore focus errors
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> item.id }
        ) { index, item ->
            val itemModifier = if (index == 0) {
                Modifier.focusRequester(focusRequester)
            } else {
                Modifier
            }

            OptimizedMediaCard(
                media = item,
                onClick = { onItemClick(item) },
                modifier = itemModifier,
                onDispose = {
                    // Cleanup when card is removed from composition
                }
            )
        }
    }
}

/**
 * Loading skeleton for grid
 */
@Composable
fun TVMediaGridSkeleton(
    modifier: Modifier = Modifier,
    columns: Int = 6,
    rows: Int = 3
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(columns * rows) {
            // Skeleton card
            Box(
                modifier = Modifier
                    .width(180.dp)
                    .height(270.dp)
            ) {
                // TODO: Add shimmer effect
            }
        }
    }
}
