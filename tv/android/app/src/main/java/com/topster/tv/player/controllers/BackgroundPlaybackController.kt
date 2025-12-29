package com.topster.tv.player.controllers

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.topster.tv.player.PlayerEventListener
import java.lang.ref.WeakReference

/**
 * Background playback controller
 * Manages different background modes (PiP, audio-only, hidden)
 */
class BackgroundPlaybackController(activity: Activity) : PlayerEventListener {

    private val activityRef = WeakReference(activity)
    private val pipController = PiPController(activity)

    private var backgroundMode = BackgroundMode.NONE
    private var shouldBlockDestroy = false

    companion object {
        private const val TAG = "BackgroundPlayback"
    }

    enum class BackgroundMode {
        NONE,           // Normal foreground playback
        PIP,            // Picture-in-picture
        AUDIO_ONLY,     // Audio continues, video paused
        PLAY_BEHIND     // Continue playing hidden
    }

    /**
     * Handle user leaving the app
     */
    fun onUserLeaveHint() {
        when (backgroundMode) {
            BackgroundMode.PIP -> {
                enterPiPMode()
            }
            BackgroundMode.AUDIO_ONLY -> {
                enterAudioOnlyMode()
            }
            BackgroundMode.PLAY_BEHIND -> {
                enterPlayBehindMode()
            }
            BackgroundMode.NONE -> {
                // Pause playback
                Log.d(TAG, "No background mode, pausing")
            }
        }
    }

    /**
     * Enter Picture-in-Picture mode
     */
    private fun enterPiPMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val success = pipController.enterPiP()
            if (success) {
                shouldBlockDestroy = true
                Log.d(TAG, "Entered PiP mode, blocking destroy")
            }
        } else {
            Log.w(TAG, "PiP not supported, falling back to audio-only")
            enterAudioOnlyMode()
        }
    }

    /**
     * Enter audio-only background mode
     */
    private fun enterAudioOnlyMode() {
        shouldBlockDestroy = true
        Log.d(TAG, "Entered audio-only mode")
        // Actual video pause would be handled by player controller
    }

    /**
     * Enter play-behind mode (minimize but keep playing)
     */
    private fun enterPlayBehindMode() {
        shouldBlockDestroy = true
        Log.d(TAG, "Entered play-behind mode")
    }

    /**
     * Set background mode preference
     */
    fun setBackgroundMode(mode: BackgroundMode) {
        backgroundMode = mode
        Log.d(TAG, "Background mode set to: $mode")
    }

    /**
     * Get current background mode
     */
    fun getBackgroundMode(): BackgroundMode = backgroundMode

    /**
     * Check if activity destruction should be blocked
     */
    fun shouldBlockDestroy(): Boolean = shouldBlockDestroy

    /**
     * Handle PiP mode changes
     */
    fun onPictureInPictureModeChanged(isInPiPMode: Boolean) {
        pipController.onPictureInPictureModeChanged(isInPiPMode)

        if (!isInPiPMode) {
            // Exited PiP, allow normal destroy
            shouldBlockDestroy = false
        }
    }

    /**
     * Handle activity resume
     */
    override fun onViewResumed() {
        shouldBlockDestroy = false
    }

    /**
     * Cleanup
     */
    override fun onFinish() {
        shouldBlockDestroy = false
    }
}
