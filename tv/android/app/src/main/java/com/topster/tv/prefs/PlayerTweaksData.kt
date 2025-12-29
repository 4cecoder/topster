package com.topster.tv.prefs

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages player customization preferences with bitmask system.
 * Based on SmartTube's PlayerTweaksData.
 */
object PlayerTweaksData {
    private const val PREFS_NAME = "player_tweaks"
    private const val KEY_PLAYER_BUTTONS = "player_buttons"
    private const val KEY_SEEK_INTERVAL = "seek_interval"
    private const val KEY_AUTO_HIDE_DELAY = "auto_hide_delay"
    private const val KEY_PLAYBACK_SPEED = "playback_speed"

    // Player button flags (bitmask)
    const val PLAYER_BUTTON_PLAY_PAUSE = 1 shl 13
    const val PLAYER_BUTTON_REPEAT_MODE = 1 shl 14
    const val PLAYER_BUTTON_NEXT = 1 shl 15
    const val PLAYER_BUTTON_PREVIOUS = 1 shl 16
    const val PLAYER_BUTTON_VIDEO_SPEED = 1 shl 5
    const val PLAYER_BUTTON_SEEK_INTERVAL = 1 shl 20
    const val PLAYER_BUTTON_VIDEO_ZOOM = 1 shl 18
    const val PLAYER_BUTTON_VIDEO_ROTATE = 1 shl 23
    const val PLAYER_BUTTON_VIDEO_FLIP = 1 shl 27
    const val PLAYER_BUTTON_SOUND_OFF = 1 shl 25
    const val PLAYER_BUTTON_PIP = 1 shl 2

    // Default configuration - clean minimal player
    private const val DEFAULT_BUTTONS = PLAYER_BUTTON_PLAY_PAUSE or
                                      PLAYER_BUTTON_PREVIOUS or
                                      PLAYER_BUTTON_NEXT or
                                      PLAYER_BUTTON_VIDEO_SPEED

    // Default playback speed
    private const val DEFAULT_SPEED = 1.0f

    private lateinit var prefs: SharedPreferences
    private val listeners = mutableSetOf<() -> Unit>()

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if a specific player button is enabled
     */
    fun isPlayerButtonEnabled(buttonFlag: Int): Boolean {
        return getPlayerButtons() and buttonFlag == buttonFlag
    }

    /**
     * Enable a player button by adding its flag
     */
    fun enablePlayerButton(buttonFlag: Int) {
        val currentButtons = getPlayerButtons()
        setPlayerButtons(currentButtons or buttonFlag)
    }

    /**
     * Disable a player button by removing its flag
     */
    fun disablePlayerButton(buttonFlag: Int) {
        val currentButtons = getPlayerButtons()
        setPlayerButtons(currentButtons and buttonFlag.inv())
    }

    /**
     * Toggle a player button on/off
     */
    fun togglePlayerButton(buttonFlag: Int) {
        if (isPlayerButtonEnabled(buttonFlag)) {
            disablePlayerButton(buttonFlag)
        } else {
            enablePlayerButton(buttonFlag)
        }
    }

    fun getPlayerButtons(): Int {
        return prefs.getInt(KEY_PLAYER_BUTTONS, DEFAULT_BUTTONS)
    }

    private fun setPlayerButtons(flags: Int) {
        prefs.edit().putInt(KEY_PLAYER_BUTTONS, flags).apply()
        notifyListeners()
    }

    fun getSeekInterval(): Int {
        return prefs.getInt(KEY_SEEK_INTERVAL, 10) // Default: 10 seconds
    }

    fun setSeekInterval(seconds: Int) {
        prefs.edit().putInt(KEY_SEEK_INTERVAL, seconds).apply()
        notifyListeners()
    }

    fun getAutoHideDelay(): Int {
        return prefs.getInt(KEY_AUTO_HIDE_DELAY, 3000) // Default: 3 seconds
    }

    fun setAutoHideDelay(ms: Int) {
        prefs.edit().putInt(KEY_AUTO_HIDE_DELAY, ms).apply()
        notifyListeners()
    }

    /**
     * Add a listener to be notified of preference changes
     */
    fun addListener(listener: () -> Unit) {
        listeners.add(listener)
    }

    /**
     * Remove a preference listener
     */
    fun removeListener(listener: () -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners() {
        listeners.forEach { it() }
    }

    /**
     * Reset to default clean configuration
     */
    fun resetToDefaults() {
        prefs.edit().clear().apply()
        notifyListeners()
    }
}
