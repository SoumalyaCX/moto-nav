package com.motonav.integrations.whatsapp

import com.motonav.data.model.LatLng
import java.util.regex.Pattern

data class WALocationMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String,
    val rawText: String,
    val locationLink: String,
    val parsedCoordinate: LatLng?,
    val timestamp: Long = System.currentTimeMillis()
)

object LocationLinkParser {
    // Regex matching various maps & coordinate formats sent on WhatsApp
    private val GOOGLE_MAPS_REGEX = Pattern.compile(
        "(https?://)?(www\\.)?(maps\\.google\\.com|goo\\.gl/maps|maps\\.app\\.goo\\.gl|google\\.com/maps)[^\\s]+",
        Pattern.CASE_INSENSITIVE
    )

    private val GEO_URI_REGEX = Pattern.compile(
        "geo:([-+]?\\d*\\.?\\d+),([-+]?\\d*\\.?\\d+)",
        Pattern.CASE_INSENSITIVE
    )

    private val RAW_COORDINATES_REGEX = Pattern.compile(
        "([-+]?\\d{1,2}\\.\\d{4,})[\\s,]+([-+]?\\d{1,3}\\.\\d{4,})"
    )

    fun extractLocationLink(text: String): String? {
        val googleMatcher = GOOGLE_MAPS_REGEX.matcher(text)
        if (googleMatcher.find()) return googleMatcher.group(0)

        val geoMatcher = GEO_URI_REGEX.matcher(text)
        if (geoMatcher.find()) return geoMatcher.group(0)

        val coordsMatcher = RAW_COORDINATES_REGEX.matcher(text)
        if (coordsMatcher.find()) return coordsMatcher.group(0)

        return null
    }

    /**
     * Parses latitude and longitude from location text or link if available directly.
     */
    fun parseCoordinates(linkOrText: String): LatLng? {
        // Test geo URI
        val geoMatcher = GEO_URI_REGEX.matcher(linkOrText)
        if (geoMatcher.find()) {
            val lat = geoMatcher.group(1)?.toDoubleOrNull()
            val lng = geoMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lng != null) return LatLng(lat, lng)
        }

        // Test explicit coordinate numbers
        val coordMatcher = RAW_COORDINATES_REGEX.matcher(linkOrText)
        if (coordMatcher.find()) {
            val lat = coordMatcher.group(1)?.toDoubleOrNull()
            val lng = coordMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lng != null) return LatLng(lat, lng)
        }

        // Test Google Maps query params e.g. q=22.5726,88.3639 or @22.5726,88.3639
        val qPattern = Pattern.compile("[?&]q=([-+]?\\d*\\.?\\d+),([-+]?\\d*\\.?\\d+)")
        val qMatcher = qPattern.matcher(linkOrText)
        if (qMatcher.find()) {
            val lat = qMatcher.group(1)?.toDoubleOrNull()
            val lng = qMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lng != null) return LatLng(lat, lng)
        }

        val atPattern = Pattern.compile("@([-+]?\\d*\\.?\\d+),([-+]?\\d*\\.?\\d+)")
        val atMatcher = atPattern.matcher(linkOrText)
        if (atMatcher.find()) {
            val lat = atMatcher.group(1)?.toDoubleOrNull()
            val lng = atMatcher.group(2)?.toDoubleOrNull()
            if (lat != null && lng != null) return LatLng(lat, lng)
        }

        return null
    }
}
