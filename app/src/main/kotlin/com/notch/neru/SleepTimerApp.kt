package com.notch.neru

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import com.notch.neru.widget.TimerWidgetUpdater
import javax.inject.Inject

@HiltAndroidApp
class SleepTimerApp : Application() {

    @Inject lateinit var timerWidgetUpdater: TimerWidgetUpdater

    override fun onCreate() {
        super.onCreate()
        // Whoever wakes the process (activity, timer service, widget tap) also
        // brings the home-screen widgets in sync from here on.
        timerWidgetUpdater.start()
    }
}
