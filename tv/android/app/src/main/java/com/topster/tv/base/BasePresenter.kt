package com.topster.tv.base

import android.app.Activity
import android.content.Context
import java.lang.ref.WeakReference

/**
 * Base presenter following SmartTube's MVP pattern with weak references
 * to prevent memory leaks
 */
abstract class BasePresenter<T> {
    private var viewRef: WeakReference<T> = WeakReference(null)
    private var activityRef: WeakReference<Activity> = WeakReference(null)
    private var contextRef: WeakReference<Context> = WeakReference(null)

    fun setView(view: T) {
        viewRef = WeakReference(view)
    }

    fun getView(): T? = viewRef.get()

    fun setActivity(activity: Activity) {
        activityRef = WeakReference(activity)
        contextRef = WeakReference(activity.applicationContext)
    }

    fun getActivity(): Activity? = activityRef.get()

    fun getContext(): Context? = contextRef.get()

    open fun onViewDestroy() {
        viewRef.clear()
        activityRef.clear()
        // Don't clear context - it's application context
    }

    open fun onFinish() {
        onViewDestroy()
    }
}
