package com.notch.neru.core.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.core.app.NotificationCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import com.notch.neru.core.data.model.TimerMode
import com.notch.neru.core.data.model.TimerPhase
import com.notch.neru.core.service.R
import com.notch.neru.core.service.SleepTimerService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimerNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID = "sleep_timer_v2"
        private const val LEGACY_CHANNEL_ID = "sleep_timer"
        const val NOTIFICATION_ID = 1
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    fun createNotificationChannel() {
        notificationManager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun buildNotification(
        remainingMinutes: Int,
        stepMinutes: Int,
        phase: TimerPhase = TimerPhase.RUNNING,
        mode: TimerMode = TimerMode.Fixed,
        userJustActive: Boolean = false,
    ): Notification {
        val isFading = phase == TimerPhase.FADING_OUT

        val titleText = if (isFading) {
            context.getString(R.string.notification_checkin_title)
        } else {
            context.getString(R.string.notification_inactivity_title)
        }

        val contentText = when {
            isFading -> context.getString(R.string.notification_checkin_text)
            userJustActive -> context.getString(R.string.notification_inactivity_active)
            remainingMinutes > 0 -> {
                context.resources.getQuantityString(
                    R.plurals.notification_inactivity_minutes_remaining,
                    remainingMinutes,
                    remainingMinutes,
                )
            }
            else -> context.getString(R.string.notification_inactivity_less_than_minute)
        }

        // Content intent to open the app
        val contentIntent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?.let { intent ->
                PendingIntent.getActivity(
                    context,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
            }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(titleText)
            .setContentText(contentText)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (isFading) {
            val awakePendingIntent = servicePendingIntent(SleepTimerService.ACTION_I_AM_AWAKE, requestCode = 4)
            val cancelPendingIntent = servicePendingIntent(SleepTimerService.ACTION_CANCEL, requestCode = 3)

            builder
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOnlyAlertOnce(false)
                .addAction(
                    0,
                    context.getString(R.string.notification_action_awake),
                    awakePendingIntent,
                )
                .addAction(
                    0,
                    context.getString(R.string.notification_action_stop_now),
                    cancelPendingIntent,
                )
        } else {
            val subtractPendingIntent = servicePendingIntent(SleepTimerService.ACTION_SUBTRACT_MINUTES, requestCode = 1)
            val addPendingIntent = servicePendingIntent(SleepTimerService.ACTION_ADD_MINUTES, requestCode = 2)
            val cancelPendingIntent = servicePendingIntent(SleepTimerService.ACTION_CANCEL, requestCode = 3)

            builder
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOnlyAlertOnce(true)
                .addAction(
                    0,
                    context.getString(R.string.notification_action_subtract_minutes, stepMinutes),
                    subtractPendingIntent,
                )
                .addAction(
                    0,
                    context.getString(R.string.notification_action_add_minutes, stepMinutes),
                    addPendingIntent,
                )
                .addAction(0, context.getString(R.string.notification_action_cancel), cancelPendingIntent)
        }

        return builder.build()
    }

    private fun servicePendingIntent(actionName: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(
            context,
            requestCode,
            SleepTimerService.intent(context, actionName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun updateNotification(
        remainingMinutes: Int,
        stepMinutes: Int,
        phase: TimerPhase = TimerPhase.RUNNING,
        mode: TimerMode = TimerMode.Fixed,
        userJustActive: Boolean = false,
    ) {
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(remainingMinutes, stepMinutes, phase, mode, userJustActive),
        )
    }

    fun cancelNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }
}
