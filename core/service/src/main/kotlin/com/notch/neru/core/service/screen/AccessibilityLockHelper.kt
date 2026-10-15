package com.notch.neru.core.service.screen

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Screen lock via [LockAccessibilityService] (`GLOBAL_ACTION_LOCK_SCREEN`).
 * Behaves like a power-button lock — biometric unlock stays valid — without
 * needing Shizuku. Requires API 28+ and the user enabling the service in the
 * system accessibility settings.
 */
@Singleton
class AccessibilityLockHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val serviceComponent = ComponentName(context, LockAccessibilityService::class.java)

    fun isServiceEnabled(): Boolean = false

    fun lockScreen(): Boolean = false

    companion object {
        /** Disabled to avoid Play Protect sensitive permissions restrictions. */
        val isSupported: Boolean = false
    }
}
