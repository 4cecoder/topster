package com.topster.tv.player.actions

import androidx.leanback.widget.Action

/**
 * Simple stub for player actions to avoid compilation errors
 */
abstract class PlayerAction protected constructor(id: Long) : Action(id) {
    protected abstract val isEnabled: Boolean
}

/**
 * Play/Pause action
 */
class PlayPauseAction(
    private val isPlaying: Boolean
) : PlayerAction(1L) {
    override val isEnabled: Boolean = true
}

/**
 * Skip to next episode
 */
class SkipNextAction : PlayerAction(2L) {
    override val isEnabled: Boolean = true
}

/**
 * Skip to previous episode
 */
class SkipPreviousAction : PlayerAction(3L) {
    override val isEnabled: Boolean = true
}

/**
 * Fast forward action
 */
class FastForwardAction : PlayerAction(4L) {
    override val isEnabled: Boolean = true
}

/**
 * Rewind action
 */
class RewindAction : PlayerAction(5L) {
    override val isEnabled: Boolean = true
}

/**
 * Video speed action
 */
class VideoSpeedAction(
    private val speed: Float = 1.0f
) : PlayerAction(6L) {
    override val isEnabled: Boolean = true
}

/**
 * Seek interval action
 */
class SeekIntervalAction(
    private val seconds: Int
) : PlayerAction(7L) {
    override val isEnabled: Boolean = true
}

/**
 * Picture-in-Picture action
 */
class PipAction : PlayerAction(8L) {
    override val isEnabled: Boolean = true
}

/**
 * Back action
 */
class BackAction : PlayerAction(9L) {
    override val isEnabled: Boolean = true
}
