package com.notch.neru.core.service.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects which app is currently open on the screen using standard Android UsageStatsManager.
 * Does NOT require Accessibility Service and is never blocked by Android Restricted Settings.
 */
@Singleton
class ForegroundAppDetector @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    fun hasUsagePermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Returns the package name of the app currently in the foreground on screen,
     * or null if permission is missing or cannot be queried.
     */
    fun getForegroundPackage(): String? {
        if (!hasUsagePermission() || usageStatsManager == null) return null
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 10_000L

        val events = usageStatsManager.queryEvents(startTime, endTime) ?: return null
        val event = UsageEvents.Event()
        var lastResumedPackage: String? = null
        var lastTimestamp = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && event.timeStamp >= lastTimestamp) {
                lastResumedPackage = event.packageName
                lastTimestamp = event.timeStamp
            }
        }
        return lastResumedPackage
    }
}
