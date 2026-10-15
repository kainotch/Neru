package com.notch.neru.core.service.interaction

import android.os.SystemClock
import android.util.Log

/**
 * Coordinates and debounces global touch, scroll, and interaction events from
 * [LockAccessibilityService] and the touch-watch overlay view.
 */
object InteractionDetector {
    private const val TAG = "InteractionDetector"
    private const val DEBOUNCE_MILLIS = 2_000L

    @Volatile
    var isWatching: Boolean = false
        private set

    @Volatile
    private var lastActivityElapsed: Long = 0L

    private var onInteractionCallback: (() -> Unit)? = null

    /** Registers a listener to be invoked whenever a debounced interaction occurs. */
    fun setInteractionListener(listener: (() -> Unit)?) {
        onInteractionCallback = listener
    }

    /** Enables or disables interaction event dispatching. */
    fun setWatchingEnabled(enabled: Boolean) {
        isWatching = enabled
        if (!enabled) {
            lastActivityElapsed = 0L
        }
    }

    /**
     * Called whenever a touch, scroll, or gesture event is detected across any application.
     * Throttled by [DEBOUNCE_MILLIS] to prevent flooding the service or draining battery.
     */
    fun onUserActivity() {
        if (!isWatching) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastActivityElapsed < DEBOUNCE_MILLIS) {
            return
        }
        lastActivityElapsed = now
        Log.d(TAG, "Debounced user activity detected across apps")
        onInteractionCallback?.invoke()
    }
}
