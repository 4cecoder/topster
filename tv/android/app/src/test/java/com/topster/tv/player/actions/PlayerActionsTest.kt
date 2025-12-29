package com.topster.tv.player.actions

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for PlayerAction classes
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlayerActionsTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun `PlayPauseAction should have play label when not playing`() {
        // Given
        val action = PlayPauseAction(context, false)

        // Then
        assertEquals("Should show 'Play' label when not playing",
            "Play",
            action.label1
        )
    }

    @Test
    fun `PlayPauseAction should have pause label when playing`() {
        // Given
        val action = PlayPauseAction(context, true)

        // Then
        assertEquals("Should show 'Pause' label when playing",
            "Pause",
            action.label1
        )
    }

    @Test
    fun `PlayPauseAction should update label when state changes`() {
        // Given
        val action = PlayPauseAction(context, false)

        // When - change to playing
        action.setPlayingState(true)

        // Then
        assertEquals("Should update to 'Pause' label",
            "Pause",
            action.label1
        )
    }

    @Test
    fun `SkipNextAction should have correct label`() {
        // Given
        val action = SkipNextAction(context)

        // Then
        assertEquals("Should have 'Next' label",
            "Next",
            action.label1
        )
    }

    @Test
    fun `SkipPreviousAction should have correct label`() {
        // Given
        val action = SkipPreviousAction(context)

        // Then
        assertEquals("Should have 'Previous' label",
            "Previous",
            action.label1
        )
    }

    @Test
    fun `FastForwardAction should have correct label`() {
        // Given
        val action = FastForwardAction(context)

        // Then
        assertEquals("Should have 'Fast Forward' label",
            "Fast Forward",
            action.label1
        )
    }

    @Test
    fun `RewindAction should have correct label`() {
        // Given
        val action = RewindAction(context)

        // Then
        assertEquals("Should have 'Rewind' label",
            "Rewind",
            action.label1
        )
    }

    @Test
    fun `VideoSpeedAction should display speed in label`() {
        // Given - create with default 1.0x
        val action = VideoSpeedAction(context)

        // Then
        assertEquals("Should show '1.0x' speed by default",
            "Speed 1.0x",
            action.label1
        )
    }

    @Test
    fun `VideoSpeedAction should update label when speed changes`() {
        // Given - create with default speed
        val action = VideoSpeedAction(context)

        // When - change to 1.5x
        action.setSpeed(1.5f)

        // Then
        assertEquals("Should update label to '1.5x'",
            "Speed 1.5x",
            action.label1
        )
    }

    @Test
    fun `VideoSpeedAction should format speed to one decimal`() {
        // Given
        val action = VideoSpeedAction(context)

        // When - set various speeds
        action.setSpeed(0.75f)
        assertTrue("0.75x should be displayed",
            action.label1.contains("0.75x")
        )

        action.setSpeed(1.0f)
        assertTrue("1.0x should be displayed",
            action.label1.contains("1.0x")
        )

        action.setSpeed(2.0f)
        assertTrue("2.0x should be displayed",
            action.label1.contains("2.0x")
        )
    }

    @Test
    fun `SeekIntervalAction should show seconds in label`() {
        // Given - create with 10 seconds
        val action = SeekIntervalAction(context, 10)

        // Then
        assertEquals("Should show 'Seek Interval 10s'",
            "Seek Interval 10s",
            action.label1
        )
    }

    @Test
    fun `SeekIntervalAction should update label when interval changes`() {
        // Given
        val action = SeekIntervalAction(context, 10)

        // When - change to 25 seconds
        action.setInterval(25)

        // Then
        assertEquals("Should update to 'Seek Interval 25s'",
            "Seek Interval 25s",
            action.label1
        )
    }

    @Test
    fun `PipAction should have correct label`() {
        // Given
        val action = PipAction(context)

        // Then
        assertEquals("Should have 'PiP' label",
            "PiP",
            action.label1
        )
    }

    @Test
    fun `BackAction should have correct label`() {
        // Given
        val action = BackAction(context)

        // Then
        assertEquals("Should have 'Back' label",
            "Back",
            action.label1
        )
    }

    @Test
    fun `all actions should have icons set`() {
        // Given
        val actions = listOf(
            PlayPauseAction(context, true),
            SkipNextAction(context),
            SkipPreviousAction(context),
            FastForwardAction(context),
            RewindAction(context),
            VideoSpeedAction(context),
            PipAction(context),
            BackAction(context)
        )

        // Then - all should have icons
        actions.forEach { action ->
            assertNotNull("Action should have icon set", action.icon)
        }
    }
}
