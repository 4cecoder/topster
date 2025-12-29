package com.topster.tv.player.actions

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.leanback.widget.Action
import com.topster.tv.R

/**
 * Base class for player actions following SmartTube pattern
 */
abstract class PlayerAction protected constructor(context: Context) : Action(context) {
    protected abstract val labelResId: Int
    protected abstract val iconResId: Int
    protected abstract val isEnabled: Boolean
        get() = true

    init {
        val iconDrawable = ContextCompat.getDrawable(context, iconResId)
        if (iconDrawable != null) {
            setIcon(iconDrawable)
        }
        setLabel1(context.getString(labelResId))
        if (!isEnabled) {
            isEnabled = false
        }
    }
}

/**
 * Play/Pause action
 */
class PlayPauseAction(context: Context, private val isPlaying: Boolean) : PlayerAction(context) {
    override val labelResId = if (isPlaying) R.string.action_pause else R.string.action_play
    override val iconResId = R.drawable.ic_pause

    fun setPlayingState(playing: Boolean) {
        label1 = context.getString(if (playing) R.string.action_pause else R.string.action_play)
        icon = ContextCompat.getDrawable(context, if (playing) R.drawable.ic_pause else R.drawable.ic_play)
    }
}

/**
 * Skip to next episode
 */
class SkipNextAction(context: Context) : PlayerAction(context) {
    override val labelResId = R.string.action_next
    override val iconResId = R.drawable.ic_next
}

/**
 * Skip to previous episode
 */
class SkipPreviousAction(context: Context) : PlayerAction(context) {
    override val labelResId = R.string.action_previous
    override val iconResId = R.drawable.ic_previous
}

/**
 * Fast forward action
 */
class FastForwardAction(context: Context) : PlayerAction(context) {
    override val labelResId = R.string.action_fast_forward
    override val iconResId = R.drawable.ic_fast_forward
}

/**
 * Rewind action
 */
class RewindAction(context: Context) : PlayerAction(context) {
    override val labelResId = R.string.action_rewind
    override val iconResId = R.drawable.ic_rewind
}

/**
 * Video speed action
 */
class VideoSpeedAction(context: Context, private val speed: Float = 1.0f) : PlayerAction(context) {
    override val labelResId = R.string.action_speed
    override val iconResId = R.drawable.ic_speed

    fun setSpeed(newSpeed: Float) {
        label1 = context.getString(R.string.action_speed) + " ${String.format("%.1fx", newSpeed)}"
    }
}
/**
 * Seek interval action
 */
class SeekIntervalAction(context: Context, private val seconds: Int) : PlayerAction(context) {
    override val labelResId = R.string.action_seek_interval
    override val iconResId = R.drawable.ic_seek

    fun setInterval(newSeconds: Int) {
        label1 = context.getString(R.string.action_seek_interval) + " ${newSeconds}s"
    }
}

/**
 * Picture-in-Picture action
 */
class PipAction(context: Context) : PlayerAction(context) {
    override val labelResId = R.string.action_pip
    override val iconResId = R.drawable.ic_pip
}

/**
 * Back action
 */
class BackAction(context: Context) : PlayerAction(context) {
    override val labelResId = R.string.action_back
    override val iconResId = R.drawable.ic_back
}
