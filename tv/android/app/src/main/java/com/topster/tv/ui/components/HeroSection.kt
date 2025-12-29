package com.topster.tv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import com.topster.tv.api.models.MediaItem
import com.topster.tv.ui.theme.FuturisticColors

/**
 * Cinematic hero section inspired by PS5/Netflix
 * Large banner with featured content
 */
@Composable
fun HeroSection(
    mediaItem: MediaItem,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        // Background image with blur
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (mediaItem.image != null) {
                Image(
                    painter = rememberAsyncImagePainter(mediaItem.image),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(2.dp),
                    contentScale = ContentScale.Crop
                )
            }

            // Gradient overlays for depth
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black
                            ),
                            startY = 0f,
                            endY = 800f
                        )
                    )
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        }

        // Content overlay
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 40.dp, bottom = 40.dp, end = 40.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            // Quality badge
            if (mediaItem.quality != null) {
                Text(
                    text = mediaItem.quality,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier
                        .background(
                            FuturisticColors.CyberPurple.copy(alpha = 0.8f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Title - streamlined
            Text(
                text = mediaItem.title,
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 40.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Metadata row
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                mediaItem.year?.let { year ->
                    Text(
                        text = year,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                }

                mediaItem.rating?.let { rating ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⭐",
                            fontSize = 18.sp
                        )
                        Text(
                            text = rating,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }

                if (mediaItem.type == "tv") {
                    Text(
                        text = "TV SERIES",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = FuturisticColors.CyberBlue,
                        letterSpacing = 2.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Play button - compact
            FuturisticButton(
                text = if (mediaItem.type == "tv") "View Series" else "Play",
                onClick = onPlayClick,
                gradient = Brush.horizontalGradient(
                    listOf(
                        FuturisticColors.CyberPurple,
                        FuturisticColors.CyberPink
                    )
                )
            )
        }
    }
}
