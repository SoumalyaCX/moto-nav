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
    @ApplicationContext private val context: Context
) {
    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    /**
     * Continuous GPS location updates optimized for motorcycle high-speed riding.
     * High accuracy, 1 second interval, lowest latency.
     */
    @SuppressLint("MissingPermission")
    fun getLocationUpdates(): Flow<UserLocationState> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(500L)
            .setMinUpdateDistanceMeters(1.0f)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    val state = UserLocationState(
                        coordinate = LatLng(loc.latitude, loc.longitude),
                        speedKmh = (loc.speed * 3.6f), // m/s to km/h
                        bearing = loc.bearing,
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
            fusedClient.removeLocationUpdates(callback)
        }
    }
}
