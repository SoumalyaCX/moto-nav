package com.motonav.navigation

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.motonav.data.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class UserLocationState(
    val coordinate: LatLng,
    val speedKmh: Float,
    val bearing: Float,
    val altitude: Double,
    val accuracyMeters: Float,
    val timestamp: Long
)

@Singleton
class LocationTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val orientationManager: OrientationManager
) {
    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Continuous GPS location updates fused with hardware compass sensor.
     * High accuracy, lowest latency.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<UserLocationState> = callbackFlow {
        var lastLocation: Location? = null
        var currentHeading: Float = 0f

        // Continuously collect hardware compass heading
        val headingJob = launch {
            orientationManager.getHeadingUpdates().collect { heading ->
                currentHeading = heading
                lastLocation?.let { loc ->
                    // When stopped or moving slowly (< 3 km/h), emit compass heading updates
                    // so front-facing map rotation responds immediately to turning handlebars or phone
                    if (!loc.hasBearing() || (loc.speed * 3.6f) < 3.0f) {
                        trySend(
                            UserLocationState(
                                coordinate = LatLng(loc.latitude, loc.longitude),
                                speedKmh = (loc.speed * 3.6f),
                                bearing = heading,
                                altitude = loc.altitude,
                                accuracyMeters = loc.accuracy,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(1.0f)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    lastLocation = loc
                    // If moving at speed (> 3 km/h) and has GPS bearing, use GPS bearing; otherwise use compass heading
                    val effectiveBearing = if (loc.hasBearing() && (loc.speed * 3.6f) >= 3.0f) {
                        loc.bearing
                    } else if (currentHeading > 0f) {
                        currentHeading
                    } else {
                        loc.bearing
                    }

                    val state = UserLocationState(
                        coordinate = LatLng(loc.latitude, loc.longitude),
                        speedKmh = (loc.speed * 3.6f),
                        bearing = effectiveBearing,
                        altitude = loc.altitude,
                        accuracyMeters = loc.accuracy,
                        timestamp = loc.time
                    )
                    trySend(state)
                }
            }
        }

        try {
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            close(e)
        }

        awaitClose {
            headingJob.cancel()
            fusedClient.removeLocationUpdates(callback)
        }
    }
}
