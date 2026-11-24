package com.motonav.data.model

data class LatLng(
    val latitude: Double,
    val longitude: Double
) {
    fun distanceTo(other: LatLng): Double {
        val r = 6371000.0 // Earth radius in meters
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dLat = Math.toRadians(other.latitude - latitude)
        val dLng = Math.toRadians(other.longitude - longitude)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
}

enum class ManeuverType {
    DEPART,
    STRAIGHT,
    SLIGHT_RIGHT,
    RIGHT,
    SHARP_RIGHT,
    U_TURN,
    SHARP_LEFT,
    LEFT,
    SLIGHT_LEFT,
    ROUNDABOUT,
    REACH_VIA,
    ARRIVE
}

data class RouteManeuver(
    val stepIndex: Int,
    val type: ManeuverType,
    val instruction: String,
    val streetName: String,
    val distanceMeters: Double,
    val timeMillis: Long,
    val coordinate: LatLng
) {
    val distanceFormatted: String
        get() = if (distanceMeters >= 1000) {
            String.format("%.1f km", distanceMeters / 1000)
        } else {
            "${distanceMeters.toInt()} m"
        }
}

data class NavigationRoute(
    val id: String = java.util.UUID.randomUUID().toString(),
    val points: List<LatLng>,
    val maneuvers: List<RouteManeuver>,
    val totalDistanceMeters: Double,
    val totalTimeMillis: Long,
    val totalAscentMeters: Double = 0.0,
    val totalDescentMeters: Double = 0.0,
    val ridingStyle: String = "SPORT"
) {
    val distanceKmFormatted: String
        get() = String.format("%.1f km", totalDistanceMeters / 1000)

    val etaDurationFormatted: String
        get() {
            val totalMinutes = (totalTimeMillis / 1000 / 60).toInt()
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
        }
}
