package com.topster.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun MediaCard(
    media: MediaItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false // For hero row cards
) {
    val cardWidth = if (large) 280.dp else 220.dp
    val cardHeight = if (large) 400.dp else 330.dp

    FuturisticCard(
        onClick = onClick,
        modifier = modifier
            .width(cardWidth)
            .height(cardHeight),
        gradient = Brush.verticalGradient(
            colors = listOf(
                FuturisticColors.DarkCard,
                FuturisticColors.DarkPanel
            )
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Poster image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                FuturisticColors.DarkPanel,
                                FuturisticColors.DarkVoid
                            )
                        )
                    )
            ) {
                if (media.image != null) {
                    Image(
                        painter = rememberAsyncImagePainter(media.image),
                        contentDescription = media.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder with futuristic gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        FuturisticColors.CyberPurple.copy(alpha = 0.2f),
                                        FuturisticColors.DarkVoid
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = media.title.take(1),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold,
                            color = FuturisticColors.CyberPurple.copy(alpha = 0.6f)
                        )
                    }
                }

                // Quality badge with futuristic styling
                if (media.quality != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        FuturisticColors.CyberPurple.copy(alpha = 0.9f),
                                        FuturisticColors.CyberPink.copy(alpha = 0.9f)
                                    )
                                ),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = media.quality,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Media info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(if (large) 16.dp else 12.dp),
                verticalArrangement = Arrangement.spacedBy(if (large) 6.dp else 4.dp)
            ) {
                Text(
                    text = media.title,
                    fontSize = if (large) 17.sp else 14.sp,
                    fontWeight = if (large) FontWeight.Bold else FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = if (large) 22.sp else 18.sp
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    media.year?.let { year ->
                        Text(
                            text = year,
                            fontSize = if (large) 14.sp else 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = FuturisticColors.CyberBlue.copy(alpha = 0.8f)
                        )
                    }
                    media.rating?.let { rating ->
                        Text(
                            text = "⭐ $rating",
                            fontSize = if (large) 14.sp else 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD700)
                        )
                    }
                }
            }
        }
    }
}
