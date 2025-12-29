package com.topster.tv.prefs

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for PlayerTweaksData bitmask configuration system
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PlayerTweaksDataTest {

    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        PlayerTweaksData.init(context)
        PlayerTweaksData.resetToDefaults()
    }

    @Test
    fun `should initialize with default buttons`() {
        // Given
        val defaults = PlayerTweaksData.DEFAULT_BUTTONS

        // When
        val enabledButtons = PlayerTweaksData.getPlayerButtons()

        // Then - should have play/pause, previous, next, speed
        assertEquals("Play/pause should be enabled",
            true,
            (enabledButtons and PlayerTweaksData.PLAYER_BUTTON_PLAY_PAUSE) == PlayerTweaksData.PLAYER_BUTTON_PLAY_PAUSE
        )
        assertEquals("Previous should be enabled",
            true,
            (enabledButtons and PlayerTweaksData.PLAYER_BUTTON_PREVIOUS) == PlayerTweaksData.PLAYER_BUTTON_PREVIOUS
        )
        assertEquals("Next should be enabled",
            true,
            (enabledButtons and PlayerTweaksData.PLAYER_BUTTON_NEXT) == PlayerTweaksData.PLAYER_BUTTON_NEXT
        )
        assertEquals("Speed should be enabled",
            true,
            (enabledButtons and PlayerTweaksData.PLAYER_BUTTON_VIDEO_SPEED) == PlayerTweaksData.PLAYER_BUTTON_VIDEO_SPEED
        )
    }

    @Test
    fun `should enable button with bitmask`() {
        // Given
        val initialButtons = PlayerTweaksData.getPlayerButtons()

        // When - enable PIP button
        PlayerTweaksData.enablePlayerButton(PlayerTweaksData.PLAYER_BUTTON_PIP)
        val updatedButtons = PlayerTweaksData.getPlayerButtons()

        // Then - PIP flag should be set
        assertEquals("PIP should be enabled",
            initialButtons or PlayerTweaksData.PLAYER_BUTTON_PIP,
            updatedButtons
        )
        assertTrue("PIP flag should be set",
            (updatedButtons and PlayerTweaksData.PLAYER_BUTTON_PIP) == PlayerTweaksData.PLAYER_BUTTON_PIP
        )
    }

    @Test
    fun `should disable button with bitmask`() {
        // Given - ensure PIP is enabled first
        PlayerTweaksData.enablePlayerButton(PlayerTweaksData.PLAYER_BUTTON_PIP)

        // When - disable PIP button
        PlayerTweaksData.disablePlayerButton(PlayerTweaksData.PLAYER_BUTTON_PIP)
        val updatedButtons = PlayerTweaksData.getPlayerButtons()

        // Then - PIP flag should be cleared
        assertFalse("PIP should be disabled",
            (updatedButtons and PlayerTweaksData.PLAYER_BUTTON_PIP) == PlayerTweaksData.PLAYER_BUTTON_PIP
        )
    }

    @Test
    fun `should toggle button on and off`() {
        // Given
        val initialButtons = PlayerTweaksData.getPlayerButtons()
        val pipEnabled = (initialButtons and PlayerTweaksData.PLAYER_BUTTON_PIP) == PlayerTweaksData.PLAYER_BUTTON_PIP

        // When - toggle PIP
        PlayerTweaksData.togglePlayerButton(PlayerTweaksData.PLAYER_BUTTON_PIP)
        val toggledOnce = PlayerTweaksData.getPlayerButtons()

        // Then - should be opposite of initial state
        assertEquals("PIP should be toggled on",
            !pipEnabled,
            (toggledOnce and PlayerTweaksData.PLAYER_BUTTON_PIP) == PlayerTweaksData.PLAYER_BUTTON_PIP
        )

        // Toggle again
        PlayerTweaksData.togglePlayerButton(PlayerTweaksData.PLAYER_BUTTON_PIP)
        val toggledTwice = PlayerTweaksData.getPlayerButtons()

        // Then - should be back to initial state
        assertEquals("PIP should be toggled back to original",
            pipEnabled,
            (toggledTwice and PlayerTweaksData.PLAYER_BUTTON_PIP) == PlayerTweaksData.PLAYER_BUTTON_PIP
        )
    }

    @Test
    fun `should handle multiple button flags simultaneously`() {
        // Given
        val initialButtons = PlayerTweaksData.getPlayerButtons()

        // When - enable multiple buttons
        PlayerTweaksData.enablePlayerButton(PlayerTweaksData.PLAYER_BUTTON_PIP)
        PlayerTweaksData.enablePlayerButton(PlayerTweaksData.PLAYER_BUTTON_VIDEO_ZOOM)
        PlayerTweaksData.enablePlayerButton(PlayerTweaksData.PLAYER_BUTTON_VIDEO_ROTATE)
        val updatedButtons = PlayerTweaksData.getPlayerButtons()

        // Then - all flags should be set
        assertTrue("PIP should be enabled",
            (updatedButtons and PlayerTweaksData.PLAYER_BUTTON_PIP) == PlayerTweaksData.PLAYER_BUTTON_PIP
        )
        assertTrue("Zoom should be enabled",
            (updatedButtons and PlayerTweaksData.PLAYER_BUTTON_VIDEO_ZOOM) == PlayerTweaksData.PLAYER_BUTTON_VIDEO_ZOOM
        )
        assertTrue("Rotate should be enabled",
            (updatedButtons and PlayerTweaksData.PLAYER_BUTTON_VIDEO_ROTATE) == PlayerTweaksData.PLAYER_BUTTON_VIDEO_ROTATE
        )
        assertEquals("Should have 3 new flags",
            initialButtons or PlayerTweaksData.PLAYER_BUTTON_PIP or PlayerTweaksData.PLAYER_BUTTON_VIDEO_ZOOM or PlayerTweaksData.PLAYER_BUTTON_VIDEO_ROTATE,
            updatedButtons
        )
    }

    @Test
    fun `should persist seek interval`() {
        // Given
        val customInterval = 25

        // When
        PlayerTweaksData.setSeekInterval(customInterval)
        val retrieved = PlayerTweaksData.getSeekInterval()

        // Then
        assertEquals("Seek interval should be persisted", customInterval, retrieved)
    }

    @Test
    fun `should use default seek interval when not set`() {
        // Given - fresh instance
        PlayerTweaksData.resetToDefaults()

        // When
        val interval = PlayerTweaksData.getSeekInterval()

        // Then - default is 10 seconds
        assertEquals("Default seek interval should be 10 seconds", 10, interval)
    }

    @Test
    fun `should handle edge case seek intervals`() {
        // Test minimum (5 seconds)
        PlayerTweaksData.setSeekInterval(5)
        assertEquals("Min interval should be 5", 5, PlayerTweaksData.getSeekInterval())

        // Test maximum (30 seconds)
        PlayerTweaksData.setSeekInterval(30)
        assertEquals("Max interval should be 30", 30, PlayerTweaksData.getSeekInterval())
    }

    @Test
    fun `should persist auto-hide delay`() {
        // Given
        val customDelay = 5000 // 5 seconds

        // When
        PlayerTweaksData.setAutoHideDelay(customDelay)
        val retrieved = PlayerTweaksData.getAutoHideDelay()

        // Then
        assertEquals("Auto-hide delay should be persisted", customDelay, retrieved)
    }

    @Test
    fun `should reset to defaults correctly`() {
        // Given - modify some settings
        PlayerTweaksData.enablePlayerButton(PlayerTweaksData.PLAYER_BUTTON_PIP)
        PlayerTweaksData.setSeekInterval(20)
        PlayerTweaksData.setAutoHideDelay(5000)

        // When
        PlayerTweaksData.resetToDefaults()

        // Then - should be back to defaults
        val buttons = PlayerTweaksData.getPlayerButtons()
        assertEquals("Buttons should be default",
            PlayerTweaksData.DEFAULT_BUTTONS,
            buttons
        )
        assertEquals("Seek interval should be default",
            10,
            PlayerTweaksData.getSeekInterval()
        )
        assertEquals("Auto-hide delay should be default",
            3000,
            PlayerTweaksData.getAutoHideDelay()
        )
    }

    @Test
    fun `should notify listeners on preference change`() {
        // Given
        var listenerCallCount = 0
        val listener = { listenerCallCount++ }
        PlayerTweaksData.addListener(listener)

        // When
        PlayerTweaksData.setSeekInterval(15)

        // Then - listener should be notified
        assertEquals("Listener should be called once", 1, listenerCallCount)

        // Cleanup
        PlayerTweaksData.removeListener(listener)
    }

    @Test
    fun `should not crash on listener removal when not added`() {
        // Given - listener that was never added
        val listener = {}

        // When - should not throw
        try {
            PlayerTweaksData.removeListener(listener)
        } catch (e: Exception) {
            fail("Should not throw exception: ${e.message}")
        }

        // Then - test passes
        assertTrue("Should complete without exception", true)
    }
}
