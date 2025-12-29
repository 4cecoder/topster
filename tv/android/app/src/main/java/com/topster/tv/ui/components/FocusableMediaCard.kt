package com.topster.tv.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.topster.tv.network.MediaItem

/**
 * Advanced media card with proper TV focus management
 * Following SmartTube's card presenter patterns
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun FocusableMediaCard(
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier
            .width(180.dp)
            .height(270.dp)
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused || focusState.hasFocus
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        scale = CardDefaults.scale(
            focusedScale = 1.1f,
            pressedScale = 1.05f
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(4.dp, Color.White),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // Poster image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            ) {
                AsyncImage(
                    model = media.image,
                    contentDescription = media.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                // Type badge
                media.type?.let { type ->
                    Text(
                        text = type,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Title
            Text(
                text = media.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = if (isFocused) {
                    MaterialTheme.typography.titleSmall
                } else {
                    MaterialTheme.typography.bodySmall
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = if (isFocused) Color.White else Color.Gray
            )
        }
    }
}

/**
 * Optimized media card with proper resource cleanup
 * Implements SmartTube's ViewHolder pattern in Compose
 */
@Composable
fun OptimizedMediaCard(
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDispose: () -> Unit = {}
) {
    DisposableEffect(media.id) {
        onDispose {
            // Clear image cache for this card when disposed
            // Following SmartTube's Glide.clear() pattern
            onDispose()
        }
    }

    FocusableMediaCard(
        media = media,
        onClick = onClick,
        modifier = modifier
    )
}
