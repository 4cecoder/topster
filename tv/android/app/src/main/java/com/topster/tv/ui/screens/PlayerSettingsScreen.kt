package com.topster.tv.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.topster.tv.prefs.PlayerTweaksData

/**
 * Simple settings screen for player customization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerSettingsScreen(
    viewModel: PlayerSettingsViewModel,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Player Settings",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Playback Buttons Section
        Text(
            text = "Playback Controls",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = viewModel.isButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_PLAY_PAUSE),
                onCheckedChange = {
                    if (it) {
                        viewModel.enableButton(PlayerTweaksData.PLAYER_BUTTON_PLAY_PAUSE)
                    } else {
                        viewModel.disableButton(PlayerTweaksData.PLAYER_BUTTON_PLAY_PAUSE)
                    }
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF00F5FF),
                    uncheckedColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Text(
                text = "Play/Pause",
                color = Color.White,
                fontSize = 16.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = viewModel.isButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_PREVIOUS),
                onCheckedChange = {
                    if (it) {
                        viewModel.enableButton(PlayerTweaksData.PLAYER_BUTTON_PREVIOUS)
                    } else {
                        viewModel.disableButton(PlayerTweaksData.PLAYER_BUTTON_PREVIOUS)
                    }
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF00F5FF),
                    uncheckedColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Text(
                text = "Previous Episode",
                color = Color.White,
                fontSize = 16.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = viewModel.isButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_NEXT),
                onCheckedChange = {
                    if (it) {
                        viewModel.enableButton(PlayerTweaksData.PLAYER_BUTTON_NEXT)
                    } else {
                        viewModel.disableButton(PlayerTweaksData.PLAYER_BUTTON_NEXT)
                    }
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF00F5FF),
                    uncheckedColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Text(
                text = "Next Episode",
                color = Color.White,
                fontSize = 16.sp
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = viewModel.isButtonEnabled(PlayerTweaksData.PLAYER_BUTTON_VIDEO_SPEED),
                onCheckedChange = {
                    if (it) {
                        viewModel.enableButton(PlayerTweaksData.PLAYER_BUTTON_VIDEO_SPEED)
                    } else {
                        viewModel.disableButton(PlayerTweaksData.PLAYER_BUTTON_VIDEO_SPEED)
                    }
                },
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF00F5FF),
                    uncheckedColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Text(
                text = "Speed Control",
                color = Color.White,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Seek Interval Section
        Text(
            text = "Seek Interval: ${viewModel.seekInterval} seconds",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )

        Slider(
            value = viewModel.seekInterval.toFloat(),
            onValueChange = { viewModel.seekInterval = it.toInt() },
            valueRange = 5f..30f,
            steps = 25,
            modifier = Modifier.fillMaxWidth(),
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF00F5FF),
                activeTrackColor = Color(0xFF00F5FF),
                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Reset Button
        Button(
            onClick = { viewModel.resetToDefaults() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF00F5FF),
                contentColor = Color.White
            )
        ) {
            Text("Reset to Defaults", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Back Button
        Button(
            onClick = onBack,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White
            )
        ) {
            Text("Back", fontSize = 16.sp)
        }
    }
}

/**
 * ViewModel for player settings
 */
class PlayerSettingsViewModel(context: Context) {
    private val tweaksData = PlayerTweaksData

    var isButtonEnabled: (Int) -> Boolean = { flag ->
        tweaksData.isPlayerButtonEnabled(flag)
    }

    var seekInterval by mutableIntStateOf(tweaksData.getSeekInterval())

    fun enableButton(flag: Int) {
        tweaksData.enablePlayerButton(flag)
    }

    fun disableButton(flag: Int) {
        tweaksData.disablePlayerButton(flag)
    }

    fun resetToDefaults() {
        tweaksData.resetToDefaults()
        seekInterval = tweaksData.getSeekInterval()
    }
}
