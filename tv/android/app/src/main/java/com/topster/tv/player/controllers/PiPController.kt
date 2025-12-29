package com.topster.tv.player.controllers

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.util.Rational
import androidx.annotation.RequiresApi
import com.topster.tv.player.PlayerEventListener
import com.topster.tv.player.VideoMetadata
import java.lang.ref.WeakReference

/**
 * Picture-in-Picture controller
 * Manages PiP mode for background playback
 */
class PiPController(activity: Activity) : PlayerEventListener {

    private val activityRef = WeakReference(activity)
    private var isPiPSupported = false
    private var isInPiPMode = false

    companion object {
        private const val TAG = "PiPController"
    }

    init {
        isPiPSupported = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        } else {
            false
        }

        Log.d(TAG, "PiP supported: $isPiPSupported")
    }

    /**
     * Enter Picture-in-Picture mode
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun enterPiP(): Boolean {
        if (!isPiPSupported) {
            Log.w(TAG, "PiP not supported on this device")
            return false
        }

        val activity = activityRef.get() ?: return false

        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()

            val result = activity.enterPictureInPictureMode(params)
            if (result) {
                isInPiPMode = true
                Log.d(TAG, "Entered PiP mode")
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enter PiP mode", e)
            return false
        }
    }

    /**
     * Update PiP parameters (e.g., aspect ratio)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun updatePiPParams(aspectRatio: Rational = Rational(16, 9)) {
        if (!isPiPSupported || !isInPiPMode) return

        val activity = activityRef.get() ?: return

        try {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()

            activity.setPictureInPictureParams(params)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update PiP params", e)
        }
    }

    /**
     * Handle PiP mode changes
     */
    fun onPictureInPictureModeChanged(isInPiPMode: Boolean) {
        this.isInPiPMode = isInPiPMode
        Log.d(TAG, "PiP mode changed: $isInPiPMode")
    }

    fun isInPiPMode(): Boolean = isInPiPMode

    fun isPiPSupported(): Boolean = isPiPSupported
}
