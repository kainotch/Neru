package com.notch.neru.core.service.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import kotlin.math.sqrt

/**
 * Lightweight accelerometer shake detector used during the "Are you still awake?"
 * check-in / fade-out grace period.
 *
 * Only active while the grace period is running — consumes 0% battery during normal countdowns
 * and requires ZERO Android permissions.
 */
class ShakeDetector(
    private val context: Context,
    private val onShakeDetected: () -> Unit,
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    @Volatile
    private var isListening = false
    private var lastShakeElapsed = 0L

    fun start() {
        if (isListening || sensorManager == null || accelerometer == null) return
        val registered = sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI,
        )
        isListening = registered
        Log.d(TAG, "Shake detector started: registered=$registered")
    }

    fun stop() {
        if (!isListening) return
        sensorManager?.unregisterListener(this)
        isListening = false
        Log.d(TAG, "Shake detector stopped")
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isListening) return
        val x = event.values[0] / SensorManager.GRAVITY_EARTH
        val y = event.values[1] / SensorManager.GRAVITY_EARTH
        val z = event.values[2] / SensorManager.GRAVITY_EARTH

        val gForce = sqrt((x * x + y * y + z * z).toDouble()).toFloat()

        // Threshold > 2.0g represents a deliberate nudge or shake in bed
        if (gForce > 2.0f) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastShakeElapsed > DEBOUNCE_MILLIS) {
                lastShakeElapsed = now
                Log.d(TAG, "Shake detected (gForce=$gForce) -> invoking reset")
                onShakeDetected()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    companion object {
        private const val TAG = "ShakeDetector"
        private const val DEBOUNCE_MILLIS = 2_000L
    }
}
