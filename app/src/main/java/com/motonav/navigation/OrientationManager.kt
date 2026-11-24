package com.motonav.navigation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrientationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    /**
     * Emits real-time hardware compass heading in degrees (0..360, where 0 is North, 90 East, 180 South, 270 West).
     * Uses Sensor.TYPE_ROTATION_VECTOR (sensor fusion) with fallback to Accelerometer + Magnetometer.
     */
    fun getHeadingUpdates(): Flow<Float> = callbackFlow {
        if (sensorManager == null) {
            trySend(0f)
            close()
            return@callbackFlow
        }

        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val rotationMatrix = FloatArray(9)
        val orientationValues = FloatArray(3)
        var lastEmittedHeading = 0f

        val listener = object : SensorEventListener {
            private val lastAccel = FloatArray(3)
            private val lastMagnet = FloatArray(3)
            private var hasAccel = false
            private var hasMagnet = false

            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationValues)
                    val degrees = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                    val heading = (degrees + 360f) % 360f

                    // Smooth filter: update if heading changes by more than 1.5 degrees
                    if (Math.abs(heading - lastEmittedHeading) > 1.5f) {
                        lastEmittedHeading = heading
                        trySend(heading)
                    }
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, lastAccel, 0, 3)
                    hasAccel = true
                    checkBackupOrientation()
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, lastMagnet, 0, 3)
                    hasMagnet = true
                    checkBackupOrientation()
                }
            }

            private fun checkBackupOrientation() {
                if (hasAccel && hasMagnet) {
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, lastAccel, lastMagnet)) {
                        SensorManager.getOrientation(rotationMatrix, orientationValues)
                        val degrees = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                        val heading = (degrees + 360f) % 360f
                        if (Math.abs(heading - lastEmittedHeading) > 1.5f) {
                            lastEmittedHeading = heading
                            trySend(heading)
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            if (accelSensor != null) sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
            if (magnetSensor != null) sensorManager.registerListener(listener, magnetSensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }
}
