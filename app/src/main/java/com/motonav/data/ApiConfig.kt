package com.motonav.data

/**
 * Global API configurations embedded in code.
 * Riders never need to enter API keys in the app interface.
 */
object ApiConfig {
    /**
     * Mapbox Public Access Token (starts with "pk.eyJ...").
     *
     * To use Mapbox:
     * 1. Create a free account at https://account.mapbox.com/ (no card, package name, or SHA certificate needed).
     * 2. Paste your public token below.
     *
     * If left blank (""), MotoNav automatically and seamlessly operates in 100% free,
     * keyless mode using OpenFreeMap vector tiles + Photon by Komoot + Overpass OSM live amenities!
     */
    const val MAPBOX_ACCESS_TOKEN: String = ""

    val isMapboxConfigured: Boolean
        get() = MAPBOX_ACCESS_TOKEN.isNotBlank() && MAPBOX_ACCESS_TOKEN.startsWith("pk.")
}
