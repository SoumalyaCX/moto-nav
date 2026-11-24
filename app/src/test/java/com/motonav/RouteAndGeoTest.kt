package com.motonav

import com.motonav.data.model.LatLng
import com.motonav.data.model.ManeuverType
import com.motonav.data.model.NavigationRoute
import com.motonav.data.model.RouteManeuver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RouteAndGeoTest {

    @Test
    fun testDistanceBetweenCoordinates() {
        // Kolkata (Park Street) to Howrah Station
        val kolkata = LatLng(22.5529, 88.3534)
        val howrah = LatLng(22.5855, 88.3426)

        val distanceMeters = kolkata.distanceTo(howrah)
        // Distance should be approximately 3.7 km - 4.0 km
        assertTrue("Distance should be around 3.8 km", distanceMeters in 3500.0..4200.0)
    }

    @Test
    fun testManeuverFormatting() {
        val maneuverNear = RouteManeuver(
            stepIndex = 1,
            type = ManeuverType.RIGHT,
            instruction = "Turn right onto NH 19",
            streetName = "NH 19",
            distanceMeters = 250.0,
            timeMillis = 15000,
            coordinate = LatLng(22.5, 88.3)
        )
        assertEquals("250 m", maneuverNear.distanceFormatted)

        val maneuverFar = maneuverNear.copy(distanceMeters = 2400.0)
        assertEquals("2.4 km", maneuverFar.distanceFormatted)
    }

    @Test
    fun testNavigationRouteEtaFormatting() {
        val route = NavigationRoute(
            points = listOf(LatLng(22.5, 88.3), LatLng(22.6, 88.4)),
            maneuvers = emptyList(),
            totalDistanceMeters = 45200.0, // 45.2 km
            totalTimeMillis = 3900000L     // 65 minutes = 1h 5m
        )

        assertEquals("45.2 km", route.distanceKmFormatted)
        assertEquals("1h 5m", route.etaDurationFormatted)
    }
}
