package com.topster.tv.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topster.tv.ui.theme.FuturisticColors

/**
 * Streamlined button - compact and professional
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuturisticButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush = FuturisticColors.PrimaryGradient,
    enabled: Boolean = true,
    icon: String? = null,
    compact: Boolean = false,
    iconOnly: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Subtle scale animation when focused
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    val buttonHeight = when {
        iconOnly -> 36.dp
        compact -> 40.dp
        else -> 48.dp
    }

    val horizontalPadding = when {
        iconOnly -> 8.dp
        compact -> 12.dp
        else -> 16.dp
    }

    val fontSize = when {
        compact -> 14.sp
        else -> 16.sp
    }

    val iconSize = when {
        iconOnly -> 18.sp
        compact -> 16.sp
        else -> 18.sp
    }

    Box(
        modifier = modifier.scale(scale)
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .height(buttonHeight)
                .then(
                    if (isFocused) {
                        Modifier.border(
                            width = 2.dp,
                            color = FuturisticColors.CyberBlue.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    } else Modifier
                ),
            shape = RoundedCornerShape(8.dp),
            color = Color.Transparent,
            interactionSource = interactionSource,
            enabled = enabled
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isFocused) gradient else Brush.linearGradient(
                            colors = listOf(
                                FuturisticColors.DarkPanel.copy(alpha = 0.9f),
                                FuturisticColors.DarkPanel.copy(alpha = 0.8f)
                            )
                        )
                    )
                    .padding(horizontal = horizontalPadding, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.wrapContentWidth()
                ) {
                    if (icon != null) {
                        Text(
                            text = icon,
                            fontSize = iconSize,
                            color = Color.White
                        )
                        if (!iconOnly) {
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                    if (!iconOnly) {
                        Text(
                            text = text,
                            fontSize = fontSize,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

/**
 * Futuristic text input with animated border glow
 */
@Composable
fun FuturisticTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    // Animated border glow
    val infiniteTransition = rememberInfiniteTransition(label = "border")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border"
    )

    val borderBrush = if (isFocused) {
        Brush.horizontalGradient(
            colors = listOf(
                FuturisticColors.CyberBlue.copy(alpha = borderAlpha),
                FuturisticColors.CyberPurple.copy(alpha = borderAlpha),
                FuturisticColors.CyberPink.copy(alpha = borderAlpha)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                FuturisticColors.DarkPanel,
                FuturisticColors.DarkPanel
            )
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isFocused) 12.dp else 4.dp,
                shape = RoundedCornerShape(12.dp),
                spotColor = FuturisticColors.CyberBlue.copy(alpha = 0.5f)
            )
            .border(
                width = 2.dp,
                brush = borderBrush,
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = FuturisticColors.DarkCard.copy(alpha = 0.8f),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusable(interactionSource = interactionSource),
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            singleLine = singleLine,
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            interactionSource = interactionSource,
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 16.sp
                    )
                }
                innerTextField()
            }
        )
    }
}

/**
 * Futuristic card with gradient border and glow
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuturisticCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    gradient: Brush = Brush.verticalGradient(
        colors = listOf(
            FuturisticColors.DarkCard,
            FuturisticColors.DarkPanel
        )
    ),
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "scale"
    )

    Box(
        modifier = modifier.scale(scale)
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .shadow(
                    elevation = if (isFocused) 16.dp else 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = FuturisticColors.CyberPurple.copy(alpha = 0.6f)
                )
                .then(
                    if (isFocused) {
                        Modifier.border(
                            width = 2.dp,
                            brush = FuturisticColors.AccentGradient,
                            shape = RoundedCornerShape(16.dp)
                        )
                    } else Modifier
                ),
            shape = RoundedCornerShape(16.dp),
            color = Color.Transparent,
            interactionSource = interactionSource
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(gradient)
                    .padding(16.dp)
            ) {
                content()
            }
        }
    }
}

/**
 * Futuristic section header with gradient text effect
 */
@Composable
fun FuturisticSectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Glow background
        Box(
            modifier = Modifier
                .offset(y = 2.dp)
                .height(4.dp)
                .fillMaxWidth(0.3f)
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            FuturisticColors.CyberPurple.copy(alpha = 0.6f),
                            Color.Transparent
                        )
                    )
                )
        )

        Text(
            text = text,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 4.dp)
        )
    }
}

/**
 * Animated gradient background
 */
@Composable
fun AnimatedGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(FuturisticColors.BackgroundGradient)
    ) {
        content()
    }
}

/**
 * Cyber loading indicator with pulse animation
 */
@Composable
fun CyberLoadingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = FuturisticColors.CyberPurple,
            strokeWidth = 4.dp,
            modifier = Modifier.size(48.dp)
        )
    }
}
