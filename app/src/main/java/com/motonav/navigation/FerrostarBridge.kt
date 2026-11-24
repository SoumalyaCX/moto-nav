package com.motonav.navigation

import com.motonav.data.model.LatLng
import com.motonav.data.model.NavigationRoute
import com.motonav.data.model.RouteManeuver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class NavigationStatus {
    object Idle : NavigationStatus()
    data class Navigating(
        val currentStepIndex: Int,
        val currentManeuver: RouteManeuver,
        val distanceToNextManeuverMeters: Double,
        val remainingDistanceTotalMeters: Double,
        val remainingTimeTotalMillis: Long,
        val currentSpeedKmh: Float,
        val currentBearing: Float,
        val isOffRoute: Boolean = false,
        val route: NavigationRoute? = null,
        val riderLocation: LatLng? = null
    ) : NavigationStatus()
    object Arrived : NavigationStatus()
}

@Singleton
class FerrostarBridge @Inject constructor(
    private val locationTracker: LocationTracker,
    private val voiceGuidance: VoiceGuidanceManager,
    private val graphHopperEngine: GraphHopperEngine,
    private val scope: CoroutineScope
) {
    private val _navigationState = MutableStateFlow<NavigationStatus>(NavigationStatus.Idle)
    val navigationState: StateFlow<NavigationStatus> = _navigationState.asStateFlow()

    private var activeRoute: NavigationRoute? = null
    private var currentManeuverIndex = 0
    private var lastAnnouncedUpcomingManeuverIndex = -1
    private var lastAnnouncedImmediateManeuverIndex = -1

    init {
        // Observe live GPS updates when navigating
        locationTracker.getLocationUpdates().onEach { userLoc ->
            handleLocationUpdate(userLoc)
        }.launchIn(scope)
    }

    fun startNavigation(route: NavigationRoute) {
        activeRoute = route
        currentManeuverIndex = 0
        lastAnnouncedUpcomingManeuverIndex = -1
        lastAnnouncedImmediateManeuverIndex = -1

        val initialManeuver = route.maneuvers.firstOrNull() ?: return
        val startLoc = route.points.firstOrNull() ?: initialManeuver.coordinate
        val initialBearing = if (route.points.size >= 2) {
            calculateBearing(route.points[0], route.points[1])
        } else {
            0f
        }

        _navigationState.value = NavigationStatus.Navigating(
            currentStepIndex = 0,
            currentManeuver = initialManeuver,
            distanceToNextManeuverMeters = initialManeuver.distanceMeters,
            remainingDistanceTotalMeters = route.totalDistanceMeters,
            remainingTimeTotalMillis = route.totalTimeMillis,
            currentSpeedKmh = 0f,
            currentBearing = initialBearing,
            route = route,
            riderLocation = startLoc
        )

        // Announce first maneuver
        voiceGuidance.announceImmediateAction(initialManeuver)
    }

    fun stopNavigation() {
        activeRoute = null
        _navigationState.value = NavigationStatus.Idle
    }

    private fun handleLocationUpdate(userLoc: UserLocationState) {
        val route = activeRoute ?: return
        if (currentManeuverIndex >= route.maneuvers.size) {
            _navigationState.value = NavigationStatus.Arrived
            return
        }

        val targetManeuver = route.maneuvers[currentManeuverIndex]
        val distanceToManeuver = userLoc.coordinate.distanceTo(targetManeuver.coordinate)

        // Check for step advance (when within 35 meters of maneuver turn for motorcycle speeds)
        if (distanceToManeuver < 35.0 && currentManeuverIndex < route.maneuvers.size - 1) {
            currentManeuverIndex++
            val nextManeuver = route.maneuvers[currentManeuverIndex]
            voiceGuidance.announceImmediateAction(nextManeuver)
        }

        // Voice instruction thresholds: 300m upcoming, 50m immediate
        if (distanceToManeuver in 50.0..300.0 && lastAnnouncedUpcomingManeuverIndex != currentManeuverIndex) {
            voiceGuidance.announceUpcomingManeuver(targetManeuver, distanceToManeuver)
            lastAnnouncedUpcomingManeuverIndex = currentManeuverIndex
        } else if (distanceToManeuver < 50.0 && lastAnnouncedImmediateManeuverIndex != currentManeuverIndex) {
            voiceGuidance.announceImmediateAction(targetManeuver)
            lastAnnouncedImmediateManeuverIndex = currentManeuverIndex
        }

        // Check off-route threshold (> 45 meters away from entire route polyline)
        val minDistanceToPolyline = route.points.minOfOrNull { it.distanceTo(userLoc.coordinate) } ?: 0.0
        val isOffRoute = minDistanceToPolyline > 45.0

        if (isOffRoute) {
            // Trigger automatic recalculation
            scope.launch {
                voiceGuidance.announceRerouting()
                val dest = route.points.last()
                val recalculated = graphHopperEngine.calculateRoute(userLoc.coordinate, dest)
                recalculated.onSuccess { newRoute ->
                    startNavigation(newRoute)
                }
            }
        }

        val remainingTotal = route.maneuvers.drop(currentManeuverIndex + 1).sumOf { it.distanceMeters } + distanceToManeuver
        val remainingMillis = if (route.totalDistanceMeters > 0) {
            ((remainingTotal / route.totalDistanceMeters) * route.totalTimeMillis).toLong()
        } else 0L

        _navigationState.value = NavigationStatus.Navigating(
            currentStepIndex = currentManeuverIndex,
            currentManeuver = route.maneuvers[currentManeuverIndex],
            distanceToNextManeuverMeters = distanceToManeuver,
            remainingDistanceTotalMeters = remainingTotal,
            remainingTimeTotalMillis = remainingMillis,
            currentSpeedKmh = userLoc.speedKmh,
            currentBearing = userLoc.bearing,
            isOffRoute = isOffRoute,
            route = route,
            riderLocation = userLoc.coordinate
        )
    }

    private fun calculateBearing(from: LatLng, to: LatLng): Float {
        val lat1 = Math.toRadians(from.latitude)
        val lon1 = Math.toRadians(from.longitude)
        val lat2 = Math.toRadians(to.latitude)
        val lon2 = Math.toRadians(to.longitude)
        val dLon = lon2 - lon1
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
        val brng = Math.toDegrees(Math.atan2(y, x))
        return ((brng + 360) % 360).toFloat()
    }
}
