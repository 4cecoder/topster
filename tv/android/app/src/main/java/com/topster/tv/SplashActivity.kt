package com.topster.tv

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = application as TopsterApplication

        setContent {
            MaterialTheme {
                SplashScreen(
                    scraper = app.scraper,
                    onComplete = {
                        // Navigate to MainActivity after loading completes
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun SplashScreen(
    scraper: com.topster.tv.scraper.FlixHQScraper,
    onComplete: () -> Unit
) {
    var progress by remember { mutableStateOf(0f) }
    var loadingStatus by remember { mutableStateOf("Initializing...") }
    val scope = rememberCoroutineScope()

    // Animated gradient colors - using simpler static colors for now
    val color1 = Color(0xFF6366F1) // Indigo
    val color2 = Color(0xFFA855F7) // Purple
    val color3 = Color(0xFFEC4899) // Pink

    // Animated transitions
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Rotation animation for orbit rings
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Pulse animation for logo
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Real-time loading
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                // Step 1: Load trending (33%)
                loadingStatus = "Loading trending content..."
                progress = 0.1f
                scraper.getTrending()
                progress = 0.33f

                // Step 2: Load recent movies (66%)
                loadingStatus = "Loading recent movies..."
                scraper.getRecent("movie")
                progress = 0.66f

                // Step 3: Load recent TV shows (100%)
                loadingStatus = "Loading TV shows..."
                scraper.getRecent("tv")
                progress = 1.0f

                loadingStatus = "Ready!"
                kotlinx.coroutines.delay(300)
                onComplete()
            } catch (e: Exception) {
                // If loading fails, still continue but show error status
                loadingStatus = "Error: ${e.message}"
                progress = 1.0f
                kotlinx.coroutines.delay(1000)
                onComplete()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Dark blue center
                        Color(0xFF020617)  // Almost black edges
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Animated background particles
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerX = size.width / 2
            val centerY = size.height / 2

            // Draw orbiting rings
            for (i in 1..3) {
                val radius = 150f * i
                val alpha = (0.2f / i)

                drawCircle(
                    color = when (i) {
                        1 -> color1
                        2 -> color2
                        else -> color3
                    }.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2f)
                )
            }

            // Draw rotating particles
            for (i in 0 until 12) {
                val angle = (rotation + i * 30) * Math.PI / 180
                val radius = 200f
                val x = centerX + (radius * cos(angle)).toFloat()
                val y = centerY + (radius * sin(angle)).toFloat()

                drawCircle(
                    color = color1.copy(alpha = 0.8f),
                    radius = 4f,
                    center = Offset(x, y)
                )
            }

            // Draw energy waves
            val waveCount = 6
            for (i in 0 until waveCount) {
                val wavePhase = (rotation + i * 60) % 360
                val waveRadius = 100f + (wavePhase / 360f) * 150f
                val waveAlpha = 1f - (wavePhase / 360f)

                drawCircle(
                    color = color2.copy(alpha = waveAlpha * 0.3f),
                    radius = waveRadius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 3f)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo/Title with pulse animation
            Text(
                text = "TOPSTER",
                fontSize = (72 * pulse).sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                style = MaterialTheme.typography.displayLarge,
                letterSpacing = 8.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Gradient subtitle
            Text(
                text = "STREAMING",
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = color1,
                letterSpacing = 12.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Futuristic loading bar
            Box(
                modifier = Modifier
                    .width(400.dp)
                    .height(4.dp)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(color1, color2, color3)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loading status
            Text(
                text = loadingStatus,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Loading percentage
            Text(
                text = "${(progress * 100).toInt()}%",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Light
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Tagline
            Text(
                text = "◆ UNLIMITED ENTERTAINMENT ◆",
                fontSize = 12.sp,
                color = color3.copy(alpha = 0.8f),
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Corner accents
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerSize = 100f
            val strokeWidth = 3f

            // Top-left corner
            drawLine(
                color = color1,
                start = Offset(0f, 0f),
                end = Offset(cornerSize, 0f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color1,
                start = Offset(0f, 0f),
                end = Offset(0f, cornerSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Top-right corner
            drawLine(
                color = color2,
                start = Offset(size.width, 0f),
                end = Offset(size.width - cornerSize, 0f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color2,
                start = Offset(size.width, 0f),
                end = Offset(size.width, cornerSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-left corner
            drawLine(
                color = color3,
                start = Offset(0f, size.height),
                end = Offset(cornerSize, size.height),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color3,
                start = Offset(0f, size.height),
                end = Offset(0f, size.height - cornerSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )

            // Bottom-right corner
            drawLine(
                color = color1,
                start = Offset(size.width, size.height),
                end = Offset(size.width - cornerSize, size.height),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color1,
                start = Offset(size.width, size.height),
                end = Offset(size.width, size.height - cornerSize),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
