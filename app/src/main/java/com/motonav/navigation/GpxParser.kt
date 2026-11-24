package com.motonav.navigation

import android.util.Log
import android.util.Xml
import com.motonav.data.model.LatLng
import com.motonav.data.model.ManeuverType
import com.motonav.data.model.NavigationRoute
import com.motonav.data.model.RouteManeuver
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.util.UUID

object GpxParser {
    private const val TAG = "GpxParser"

    fun parseGpx(inputStream: InputStream): NavigationRoute? {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(inputStream, "UTF-8")

        val points = mutableListOf<LatLng>()
        var trackName = "Imported GPX Trail"
        var currentTag: String? = null

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    currentTag = parser.name
                    if (currentTag.equals("trkpt", ignoreCase = true) ||
                        currentTag.equals("rtept", ignoreCase = true) ||
                        currentTag.equals("wpt", ignoreCase = true)
                    ) {
                        val latStr = parser.getAttributeValue(null, "lat")
                        val lonStr = parser.getAttributeValue(null, "lon")
                        if (latStr != null && lonStr != null) {
                            try {
                                val lat = latStr.toDouble()
                                val lon = lonStr.toDouble()
                                points.add(LatLng(lat, lon))
                            } catch (ignored: Exception) {}
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (currentTag.equals("name", ignoreCase = true) && parser.text.isNotBlank()) {
                        if (trackName == "Imported GPX Trail") {
                            trackName = parser.text.trim()
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    currentTag = null
                }
            }
            eventType = parser.next()
        }

        if (points.size < 2) {
            Log.w(TAG, "GPX file contains fewer than 2 valid coordinates")
            return null
        }

        // Compute total distance along track points
        var totalDistanceMeters = 0.0
        for (i in 0 until points.size - 1) {
            totalDistanceMeters += points[i].distanceTo(points[i + 1])
        }

        val totalTimeMillis = ((totalDistanceMeters / 1000.0) / 45.0 * 3600.0 * 1000.0).toLong()

        // Generate synthetic start, waypoint, and finish maneuvers
        val maneuvers = mutableListOf<RouteManeuver>()
        maneuvers.add(
            RouteManeuver(
                stepIndex = 0,
                type = ManeuverType.DEPART,
                instruction = "Follow GPX track: $trackName",
                streetName = trackName,
                distanceMeters = totalDistanceMeters,
                timeMillis = totalTimeMillis,
                coordinate = points.first()
            )
        )
        maneuvers.add(
            RouteManeuver(
                stepIndex = 1,
                type = ManeuverType.ARRIVE,
                instruction = "Arrive at end of GPX trail",
                streetName = trackName,
                distanceMeters = 0.0,
                timeMillis = 0L,
                coordinate = points.last()
            )
        )

        return NavigationRoute(
            id = UUID.randomUUID().toString(),
            points = points,
            maneuvers = maneuvers,
            totalDistanceMeters = totalDistanceMeters,
            totalTimeMillis = totalTimeMillis,
            totalAscentMeters = 150.0,
            totalDescentMeters = 120.0,
            ridingStyle = "GPX TRAIL"
        )
    }
}
