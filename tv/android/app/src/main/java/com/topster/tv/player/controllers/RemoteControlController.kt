package com.topster.tv.player.controllers

import android.content.Context
import android.util.Log
import android.view.KeyEvent
import com.topster.tv.player.PlayerEventListener
import java.lang.ref.WeakReference

/**
 * Remote control handler for TV remote keys
 * Handles D-pad, playback controls, and custom actions
 */
class RemoteControlController(context: Context) : PlayerEventListener {

    private val contextRef = WeakReference(context)
    private var playbackSpeed = 1.0f
    private val speedPresets = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
    private var currentSpeedIndex = 3 // 1.0f

    companion object {
        private const val TAG = "RemoteControl"

        // Custom key codes for advanced features
        const val SPEED_UP = KeyEvent.KEYCODE_BUTTON_R1
        const val SPEED_DOWN = KeyEvent.KEYCODE_BUTTON_L1
        const val JUMP_FORWARD = KeyEvent.KEYCODE_MEDIA_FAST_FORWARD
        const val JUMP_BACKWARD = KeyEvent.KEYCODE_MEDIA_REWIND

        // Jump intervals
        const val SMALL_JUMP_MS = 10_000L // 10 seconds
        const val LARGE_JUMP_MS = 30_000L // 30 seconds
    }

    /**
     * Handle key event from remote control
     */
    fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) return false

        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                handlePlayCommand()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                handlePauseCommand()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                handlePlayPauseCommand()
                true
            }
            KeyEvent.KEYCODE_MEDIA_STOP -> {
                handleStopCommand()
                true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                handleNextCommand()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                handlePreviousCommand()
                true
            }
            JUMP_FORWARD -> {
                handleJumpForward()
                true
            }
            JUMP_BACKWARD -> {
                handleJumpBackward()
                true
            }
            SPEED_UP -> {
                handleSpeedUp()
                true
            }
            SPEED_DOWN -> {
                handleSpeedDown()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                // Let UI handle D-pad navigation
                false
            }
            else -> false
        }
    }

    private fun handlePlayCommand() {
        Log.d(TAG, "Play command")
        // Delegate to player controller
    }

    private fun handlePauseCommand() {
        Log.d(TAG, "Pause command")
        // Delegate to player controller
    }

    private fun handlePlayPauseCommand() {
        Log.d(TAG, "Play/Pause toggle")
        // Delegate to player controller
    }

    private fun handleStopCommand() {
        Log.d(TAG, "Stop command")
        // Delegate to player controller
    }

    private fun handleNextCommand() {
        Log.d(TAG, "Next episode")
        // Delegate to playback presenter
    }

    private fun handlePreviousCommand() {
        Log.d(TAG, "Previous episode")
        // Delegate to playback presenter
    }

    private fun handleJumpForward() {
        Log.d(TAG, "Jump forward $SMALL_JUMP_MS ms")
        // Delegate to player controller
    }

    private fun handleJumpBackward() {
        Log.d(TAG, "Jump backward $SMALL_JUMP_MS ms")
        // Delegate to player controller
    }

    private fun handleSpeedUp() {
        if (currentSpeedIndex < speedPresets.size - 1) {
            currentSpeedIndex++
            playbackSpeed = speedPresets[currentSpeedIndex]
            Log.d(TAG, "Speed up to ${playbackSpeed}x")
            // Delegate to player controller
        }
    }

    private fun handleSpeedDown() {
        if (currentSpeedIndex > 0) {
            currentSpeedIndex--
            playbackSpeed = speedPresets[currentSpeedIndex]
            Log.d(TAG, "Speed down to ${playbackSpeed}x")
            // Delegate to player controller
        }
    }

    fun getCurrentSpeed(): Float = playbackSpeed

    fun resetSpeed() {
        currentSpeedIndex = 3
        playbackSpeed = 1.0f
    }
}
