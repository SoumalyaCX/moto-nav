package com.motonav.data.repository

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.motonav.data.local.PreferencesStore
import com.motonav.data.local.db.PoiDao
import com.motonav.data.local.db.entities.PoiCategory
import com.motonav.data.local.db.entities.PoiEntity
import com.motonav.data.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoiRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val poiDao: PoiDao,
    private val preferencesStore: PreferencesStore
) {
    companion object {
        private const val TAG = "PoiRepository"
        private const val PHOTON_API_BASE = "https://photon.komoot.io/api/"
        private const val NOMINATIM_API_BASE = "https://nominatim.openstreetmap.org/search"
        private const val OVERPASS_API = "https://overpass-api.de/api/interpreter"
    }

    suspend fun getNearbyPois(
        userLocation: LatLng,
        category: PoiCategory? = null,
        radiusKm: Double = 20.0,
        limit: Int = 30
    ): List<PoiEntity> = withContext(Dispatchers.IO) {
        val latDelta = radiusKm / 111.0
        val lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(userLocation.latitude)))

        val minLat = userLocation.latitude - latDelta
        val maxLat = userLocation.latitude + latDelta
        val minLng = userLocation.longitude - lngDelta
        val maxLng = userLocation.longitude + lngDelta

        // 1. Fetch from local SQLite DB first
        val localPois = if (category != null) {
            poiDao.getNearbyByCategory(
                category = category.name,
                userLat = userLocation.latitude,
                userLng = userLocation.longitude,
                minLat = minLat,
                maxLat = maxLat,
                minLng = minLng,
                maxLng = maxLng,
                limit = limit
            )
        } else {
            poiDao.getNearbyPois(
                userLat = userLocation.latitude,
                userLng = userLocation.longitude,
                minLat = minLat,
                maxLat = maxLat,
                minLng = minLng,
                maxLng = maxLng,
                limit = limit
            )
        }.toMutableList()

        // 2. If a specific roadside category is selected (Petrol, Dhaba, Mechanic, Viewpoint), query live Overpass
        if (category != null && category in setOf(PoiCategory.PETROL, PoiCategory.DHABA, PoiCategory.MECHANIC, PoiCategory.VIEWPOINT)) {
            val livePois = fetchLiveOverpassAmenities(userLocation, category, (radiusKm * 1000).toInt(), limit)
            if (livePois.isNotEmpty()) {
                // Cache into Room DB so they remain available offline
                try {
                    poiDao.insertAll(livePois)
                } catch (ignored: Exception) {}

                // Merge with local results, avoiding duplicates
                for (live in livePois) {
                    val exists = localPois.any {
                        it.name.equals(live.name, ignoreCase = true) ||
                        (Math.abs(it.latitude - live.latitude) < 0.005 && Math.abs(it.longitude - live.longitude) < 0.005)
                    }
                    if (!exists) {
                        localPois.add(live)
                    }
                }
            }
        }

        // Return sorted by proximity
        localPois.sortedBy {
            val dLat = it.latitude - userLocation.latitude
            val dLng = it.longitude - userLocation.longitude
            dLat * dLat + dLng * dLng
        }.take(limit)
    }

    suspend fun searchPois(
        query: String,
        userLocation: LatLng? = null,
        limit: Int = 20
    ): List<PoiEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val results = mutableListOf<PoiEntity>()

        // 1. Check if user configured a Google Places API key in Settings
        val googleApiKey = preferencesStore.googlePlacesApiKeyFlow.first()
        if (googleApiKey.isNotBlank()) {
            val googleResults = searchGooglePlaces(trimmed, userLocation, googleApiKey, limit)
            if (googleResults.isNotEmpty()) {
                results.addAll(googleResults)
            }
        }

        // 2. If Google Places gave no results or no key is configured, query Photon by Komoot (Free global OSM geocoder with shop/POI detail)
        if (results.isEmpty()) {
            val photonResults = searchPhoton(trimmed, userLocation, limit)
            if (photonResults.isNotEmpty()) {
                results.addAll(photonResults)
            }
        }

        // 3. Nominatim OpenStreetMap fallback if still empty
        if (results.isEmpty()) {
            val nominatimResults = searchNominatim(trimmed, limit)
            if (nominatimResults.isNotEmpty()) {
                results.addAll(nominatimResults)
            }
        }

        // 4. Query local database for offline or pre-seeded results
        val localMatches = poiDao.searchByName(trimmed, limit)
        for (local in localMatches) {
            val exists = results.any {
                it.name.equals(local.name, ignoreCase = true) ||
                (Math.abs(it.latitude - local.latitude) < 0.01 && Math.abs(it.longitude - local.longitude) < 0.01)
            }
            if (!exists) {
                results.add(local)
            }
        }

        // 5. System Geocoder fallback for Indian towns/landmarks
        if (results.isEmpty() && Geocoder.isPresent()) {
            try {
                val geocoder = Geocoder(context, Locale("en", "IN"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(trimmed, 5)
                if (!addresses.isNullOrEmpty()) {
                    var genId = 880000L
                    for (addr in addresses) {
                        val placeName = addr.featureName ?: addr.locality ?: trimmed
                        results.add(
                            PoiEntity(
                                osmId = genId++,
                                name = placeName,
                                category = PoiCategory.CITY.name,
                                latitude = addr.latitude,
                                longitude = addr.longitude,
                                brand = addr.locality ?: addr.adminArea ?: "India",
                                address = addr.getAddressLine(0) ?: "$placeName, India",
                                regionId = "west_bengal"
                            )
                        )
                    }
                }
            } catch (ignored: Exception) {}
        }

        // Cache newly discovered remote POIs into Room DB for future offline access
        if (results.isNotEmpty()) {
            try {
                poiDao.insertAll(results)
            } catch (ignored: Exception) {}
        }

        results.take(limit)
    }

    /**
     * Photon by Komoot: High-performance, typo-tolerant search across all OSM shops, businesses, and places.
     */
    private fun searchPhoton(query: String, userLocation: LatLng?, limit: Int): List<PoiEntity> {
        val list = mutableListOf<PoiEntity>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlBuilder = StringBuilder("$PHOTON_API_BASE?q=$encodedQuery&limit=$limit")
            if (userLocation != null) {
                urlBuilder.append("&lat=${userLocation.latitude}&lon=${userLocation.longitude}")
            }

            val conn = URL(urlBuilder.toString()).openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "MotoNav/1.0")

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val features = json.optJSONArray("features") ?: return emptyList()

                for (i in 0 until features.length()) {
                    val feat = features.getJSONObject(i)
                    val geom = feat.optJSONObject("geometry") ?: continue
                    val coords = geom.optJSONArray("coordinates") ?: continue
                    val lng = coords.getDouble(0)
                    val lat = coords.getDouble(1)

                    val props = feat.optJSONObject("properties") ?: continue
                    val name = props.optString("name").takeIf { it.isNotBlank() }
                        ?: props.optString("street").takeIf { it.isNotBlank() }
                        ?: props.optString("city").takeIf { it.isNotBlank() }
                        ?: continue

                    val osmId = props.optLong("osm_id", System.currentTimeMillis() + i)
                    val osmKey = props.optString("osm_key")
                    val osmVal = props.optString("osm_value")

                    val category = when {
                        osmVal == "fuel" -> PoiCategory.PETROL.name
                        osmVal in listOf("restaurant", "fast_food", "cafe", "dhaba") -> PoiCategory.DHABA.name
                        osmVal in listOf("motorcycle", "car_repair", "tyres", "repair") -> PoiCategory.MECHANIC.name
                        osmVal in listOf("viewpoint", "attraction", "peak") -> PoiCategory.VIEWPOINT.name
                        osmKey == "place" -> PoiCategory.CITY.name
                        else -> "POI"
                    }

                    val street = props.optString("street")
                    val city = props.optString("city").takeIf { it.isNotBlank() }
                        ?: props.optString("district").takeIf { it.isNotBlank() }
                        ?: props.optString("state")
                    val fullAddress = listOfNotNull(
                        street.takeIf { it.isNotBlank() },
                        city.takeIf { it.isNotBlank() },
                        props.optString("country").takeIf { it.isNotBlank() }
                    ).joinToString(", ")

                    list.add(
                        PoiEntity(
                            osmId = osmId,
                            name = name,
                            category = category,
                            latitude = lat,
                            longitude = lng,
                            brand = props.optString("brand").takeIf { it.isNotBlank() } ?: city,
                            address = fullAddress.ifBlank { name },
                            regionId = "online"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Photon search failed: ${e.message}")
        }
        return list
    }

    /**
     * Nominatim OpenStreetMap Search for Indian towns, landmarks, and highways.
     */
    private fun searchNominatim(query: String, limit: Int): List<PoiEntity> {
        val list = mutableListOf<PoiEntity>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlStr = "$NOMINATIM_API_BASE?format=jsonv2&q=$encodedQuery&addressdetails=1&countrycodes=in&limit=$limit"
            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "MotoNav/1.0 (Android Motorcycle Navigation)")

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val array = org.json.JSONArray(body)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val placeId = obj.optLong("place_id", System.currentTimeMillis() + i)
                    val displayName = obj.optString("display_name")
                    val lat = obj.optDouble("lat")
                    val lon = obj.optDouble("lon")
                    val shortName = displayName.substringBefore(",")

                    val category = when (obj.optString("type")) {
                        "fuel" -> PoiCategory.PETROL.name
                        "restaurant", "cafe", "fast_food" -> PoiCategory.DHABA.name
                        else -> PoiCategory.CITY.name
                    }

                    list.add(
                        PoiEntity(
                            osmId = placeId,
                            name = shortName,
                            category = category,
                            latitude = lat,
                            longitude = lon,
                            brand = null,
                            address = displayName,
                            regionId = "nominatim"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Nominatim search failed: ${e.message}")
        }
        return list
    }

    /**
     * Live Overpass API querying live amenities (fuel, dhabas, mechanics) around user location.
     */
    private fun fetchLiveOverpassAmenities(
        userLocation: LatLng,
        category: PoiCategory,
        radiusMeters: Int,
        limit: Int
    ): List<PoiEntity> {
        val list = mutableListOf<PoiEntity>()
        try {
            val tagFilter = when (category) {
                PoiCategory.PETROL -> """["amenity"="fuel"]"""
                PoiCategory.DHABA -> """["amenity"~"restaurant|fast_food|cafe"]"""
                PoiCategory.MECHANIC -> """["shop"~"motorcycle|car_repair|tyres|repair"]"""
                PoiCategory.VIEWPOINT -> """["tourism"="viewpoint"]"""
                else -> return emptyList()
            }

            val query = "[out:json][timeout:4];node(around:$radiusMeters,${userLocation.latitude},${userLocation.longitude})$tagFilter;out body $limit;"
            val conn = URL(OVERPASS_API).openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")

            conn.outputStream.use { os ->
                os.write("data=${URLEncoder.encode(query, "UTF-8")}".toByteArray())
            }

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val elements = json.optJSONArray("elements") ?: return emptyList()

                for (i in 0 until elements.length()) {
                    val elem = elements.getJSONObject(i)
                    val id = elem.optLong("id")
                    val lat = elem.optDouble("lat")
                    val lon = elem.optDouble("lon")
                    val tags = elem.optJSONObject("tags") ?: continue

                    val name = tags.optString("name").takeIf { it.isNotBlank() }
                        ?: tags.optString("brand").takeIf { it.isNotBlank() }
                        ?: tags.optString("operator").takeIf { it.isNotBlank() }
                        ?: "${category.name} Station"

                    val brand = tags.optString("brand").takeIf { it.isNotBlank() }
                        ?: tags.optString("operator").takeIf { it.isNotBlank() }
                    val street = tags.optString("addr:street")
                    val hours = tags.optString("opening_hours")
                    val phone = tags.optString("phone").takeIf { it.isNotBlank() }
                        ?: tags.optString("contact:phone")

                    list.add(
                        PoiEntity(
                            osmId = id,
                            name = name,
                            category = category.name,
                            latitude = lat,
                            longitude = lon,
                            brand = brand,
                            address = street.ifBlank { brand ?: "Nearby roadside amenity" },
                            phone = phone,
                            openingHours = hours,
                            regionId = "overpass"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Overpass live query failed: ${e.message}")
        }
        return list
    }

    /**
     * Optional Google Places API Text Search when API Key is provided by user in Settings.
     */
    private fun searchGooglePlaces(
        query: String,
        userLocation: LatLng?,
        apiKey: String,
        limit: Int
    ): List<PoiEntity> {
        val list = mutableListOf<PoiEntity>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val urlBuilder = StringBuilder(
                "https://maps.googleapis.com/maps/api/place/textsearch/json?query=$encodedQuery&key=$apiKey"
            )
            if (userLocation != null) {
                urlBuilder.append("&location=${userLocation.latitude},${userLocation.longitude}&radius=35000")
            }

            val conn = URL(urlBuilder.toString()).openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val results = json.optJSONArray("results") ?: return emptyList()

                for (i in 0 until minOf(results.length(), limit)) {
                    val place = results.getJSONObject(i)
                    val name = place.optString("name")
                    val geom = place.optJSONObject("geometry") ?: continue
                    val loc = geom.optJSONObject("location") ?: continue
                    val lat = loc.getDouble("lat")
                    val lng = loc.getDouble("lng")
                    val address = place.optString("formatted_address")
                    val rating = place.optDouble("rating", 0.0)

                    val types = place.optJSONArray("types")?.toString() ?: ""
                    val category = when {
                        types.contains("gas_station") -> PoiCategory.PETROL.name
                        types.contains("restaurant") || types.contains("food") || types.contains("cafe") -> PoiCategory.DHABA.name
                        types.contains("car_repair") || types.contains("motorcycle") -> PoiCategory.MECHANIC.name
                        else -> PoiCategory.CITY.name
                    }

                    list.add(
                        PoiEntity(
                            osmId = System.currentTimeMillis() + i,
                            name = name,
                            category = category,
                            latitude = lat,
                            longitude = lng,
                            brand = if (rating > 0.0) "⭐ $rating" else null,
                            address = address,
                            regionId = "google_places"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Google Places query failed: ${e.message}")
        }
        return list
    }

    suspend fun getPoiCountForRegion(regionId: String): Int = withContext(Dispatchers.IO) {
        poiDao.getPoiCountByRegion(regionId)
    }

    fun observePoisForRegion(regionId: String): Flow<List<PoiEntity>> {
        return poiDao.observePoisByRegion(regionId)
    }
}
