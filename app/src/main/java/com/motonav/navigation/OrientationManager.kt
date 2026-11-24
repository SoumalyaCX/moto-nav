package com.motonav.navigation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max

data class TelemetryData(
    val heading: Float = 0f,
    val leanAngle: Float = 0f, // in degrees: negative = Left lean, positive = Right lean
    val maxLeftLean: Float = 0f,
    val maxRightLean: Float = 0f,
    val pitch: Float = 0f,
    val lateralG: Float = 0f
)

@Singleton
class OrientationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager

    private var sessionMaxLeftLean = 0f
    private var sessionMaxRightLean = 0f

    fun resetMaxLean() {
        sessionMaxLeftLean = 0f
        sessionMaxRightLean = 0f
    }

    /**
     * Emits real-time hardware compass heading in degrees (0..360, where 0 is North, 90 East, 180 South, 270 West).
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
        val remappedMatrix = FloatArray(9)
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
                    remapForDisplayRotation(rotationMatrix, remappedMatrix)
                    SensorManager.getOrientation(remappedMatrix, orientationValues)

                    val degrees = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                    val heading = (degrees + 360f) % 360f

                    if (abs(heading - lastEmittedHeading) > 1.5f) {
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
                        remapForDisplayRotation(rotationMatrix, remappedMatrix)
                        SensorManager.getOrientation(remappedMatrix, orientationValues)
                        val degrees = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
                        val heading = (degrees + 360f) % 360f
                        if (abs(heading - lastEmittedHeading) > 1.5f) {
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

    /**
     * Emits full motorcycle cockpit telemetry: Heading, Lean Angle (roll), Pitch, and Lateral G-Force.
     */
    fun getTelemetryUpdates(): Flow<TelemetryData> = callbackFlow {
        if (sensorManager == null) {
            trySend(TelemetryData())
            close()
            return@callbackFlow
        }

        val rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val rotationMatrix = FloatArray(9)
        val remappedMatrix = FloatArray(9)
        val orientationValues = FloatArray(3)

        var smoothedLean = 0f
        var lastEmittedLean = 0f
        var lastEmittedHeading = 0f
        var currentLateralG = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    // Lateral acceleration: values[0] in portrait
                    val gX = event.values[0] / 9.80665f
                    currentLateralG = 0.8f * currentLateralG + 0.2f * gX
                }

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    remapForDisplayRotation(rotationMatrix, remappedMatrix)
                    SensorManager.getOrientation(remappedMatrix, orientationValues)

                    val headingDeg = ((Math.toDegrees(orientationValues[0].toDouble()).toFloat()) + 360f) % 360f
                    val pitchDeg = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
                    val rawRollDeg = Math.toDegrees(orientationValues[2].toDouble()).toFloat()

                    // Low-pass exponential smoothing filter (alpha = 0.25) to eliminate bike engine vibration jitter
                    smoothedLean = 0.75f * smoothedLean + 0.25f * rawRollDeg

                    // Update session max lean angles
                    if (smoothedLean < -2f) {
                        sessionMaxLeftLean = max(sessionMaxLeftLean, abs(smoothedLean))
                    } else if (smoothedLean > 2f) {
                        sessionMaxRightLean = max(sessionMaxRightLean, smoothedLean)
                    }

                    if (abs(smoothedLean - lastEmittedLean) > 1.0f || abs(headingDeg - lastEmittedHeading) > 1.5f) {
                        lastEmittedLean = smoothedLean
                        lastEmittedHeading = headingDeg

                        trySend(
                            TelemetryData(
                                heading = headingDeg,
                                leanAngle = smoothedLean,
                                maxLeftLean = sessionMaxLeftLean,
                                maxRightLean = sessionMaxRightLean,
                                pitch = pitchDeg,
                                lateralG = currentLateralG
                            )
                        )
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationSensor != null) {
            sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)
        }
        if (accelSensor != null) {
            sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_UI)
        }

        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    private fun remapForDisplayRotation(inR: FloatArray, outR: FloatArray) {
        val rotation = windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        val (axisX, axisY) = when (rotation) {
            Surface.ROTATION_90 -> Pair(SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X)
            Surface.ROTATION_180 -> Pair(SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Y)
            Surface.ROTATION_270 -> Pair(SensorManager.AXIS_MINUS_Y, SensorManager.AXIS_X)
            else -> Pair(SensorManager.AXIS_X, SensorManager.AXIS_Y)
        }
        SensorManager.remapCoordinateSystem(inR, axisX, axisY, outR)
    }
}
