package com.topster.tv.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Futuristic color palette - Cyberpunk/Neon theme
object FuturisticColors {
    // Primary gradients
    val CyberPurple = Color(0xFFB24BF3) // Neon purple
    val CyberPink = Color(0xFFFF006E) // Hot pink
    val CyberBlue = Color(0xFF06FFF0) // Cyan
    val CyberIndigo = Color(0xFF6366F1) // Indigo
    val ElectricBlue = Color(0xFF3B82F6) // Electric blue

    // Accent colors
    val NeonGreen = Color(0xFF00FF88)
    val NeonYellow = Color(0xFFFFC700)
    val HotMagenta = Color(0xFFFF0080)

    // Dark backgrounds
    val DarkSpace = Color(0xFF0A0E27) // Deep dark blue
    val DarkVoid = Color(0xFF0D0221) // Almost black with purple tint
    val DarkPanel = Color(0xFF1A1F3A) // Dark panel
    val DarkCard = Color(0xFF1E2139) // Card background

    // Gradient brushes
    val PrimaryGradient = Brush.horizontalGradient(
        colors = listOf(CyberPurple, CyberPink)
    )

    val SecondaryGradient = Brush.horizontalGradient(
        colors = listOf(CyberBlue, ElectricBlue)
    )

    val AccentGradient = Brush.horizontalGradient(
        colors = listOf(CyberIndigo, CyberPurple, CyberPink)
    )

    val BackgroundGradient = Brush.verticalGradient(
        colors = listOf(DarkVoid, DarkSpace)
    )

    // Glow colors (with alpha for layering)
    val GlowPurple = Color(0x66B24BF3)
    val GlowPink = Color(0x66FF006E)
    val GlowBlue = Color(0x6606FFF0)
}
