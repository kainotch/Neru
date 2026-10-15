package com.notch.neru.core.service.screen

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Optional soft screen-lock via [AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN].
 * Behaves like a power-button lock — biometric unlock stays valid — without
 * needing Shizuku. Requires API 28+ and the user enabling the service in the
 * system accessibility settings.
 *
 * Must stay Hilt-free: the system creates it through the no-arg constructor.
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        serviceInfo = serviceInfo?.apply {
            eventTypes = 0
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: LockAccessibilityService? = null
            private set
    }
}
