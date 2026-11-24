package com.motonav.data

import android.util.Log

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
     * If left blank ("") or if Mapbox hits 402 (Payment Required) or 429 (Rate Limit),
     * MotoNav automatically and seamlessly operates in 100% free, keyless mode
     * using OpenFreeMap vector tiles + Photon by Komoot + Overpass OSM live amenities!
     */
    const val MAPBOX_ACCESS_TOKEN: String = ""

    @Volatile
    var isMapboxQuotaExceeded: Boolean = false
        private set

    val isMapboxConfigured: Boolean
        get() = MAPBOX_ACCESS_TOKEN.isNotBlank() &&
                MAPBOX_ACCESS_TOKEN.startsWith("pk.") &&
                !isMapboxQuotaExceeded

    /**
     * Called when Mapbox returns 402 (Payment Required) or 429 (Too Many Requests),
     * or when Mapbox styles/tiles fail authentication.
     */
    fun markMapboxQuotaExceeded(reason: String = "") {
        if (!isMapboxQuotaExceeded) {
            isMapboxQuotaExceeded = true
            Log.w("ApiConfig", "⚠️ Mapbox Quota/Rate Limit Exceeded ($reason). Automatically switching to 100% Free OpenFreeMap + Photon.")
        }
    }

    /**
     * Resets quota state if new session or token is applied.
     */
    fun resetMapboxQuota() {
        isMapboxQuotaExceeded = false
    }
}
