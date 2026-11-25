package com.motonav.data.repository

import android.content.Context
import android.location.Geocoder
import android.util.Log
import com.motonav.data.ApiConfig
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

        // 1. If Mapbox public access token is configured in ApiConfig.kt, query Mapbox Places API
        if (ApiConfig.isMapboxConfigured) {
            val mapboxResults = searchMapbox(trimmed, userLocation, ApiConfig.MAPBOX_ACCESS_TOKEN, limit)
            if (mapboxResults.isNotEmpty()) {
                results.addAll(mapboxResults)
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
                        osmKey == "shop" || osmVal in listOf("supermarket", "convenience", "general", "store", "mall") -> PoiCategory.STORE.name
                        osmVal in listOf("hospital", "pharmacy", "clinic") -> PoiCategory.HOSPITAL.name
                        osmKey == "place" -> PoiCategory.CITY.name
                        else -> PoiCategory.STORE.name
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
     * Mapbox Places Geocoding search when public access token is configured in ApiConfig.kt.
     */
    private fun searchMapbox(
        query: String,
        userLocation: LatLng?,
        token: String,
        limit: Int
    ): List<PoiEntity> {
        val list = mutableListOf<PoiEntity>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val proximity = if (userLocation != null) "&proximity=${userLocation.longitude},${userLocation.latitude}" else ""
            val urlStr = "https://api.mapbox.com/geocoding/v5/mapbox.places/$encodedQuery.json?access_token=$token$proximity&country=in&limit=$limit"

            val conn = URL(urlStr).openConnection() as HttpURLConnection
            conn.connectTimeout = 4000
            conn.readTimeout = 4000
            conn.setRequestProperty("User-Agent", "MotoNav/1.0 (Android Motorcycle Navigation)")

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val features = json.optJSONArray("features") ?: return emptyList()

                for (i in 0 until minOf(features.length(), limit)) {
                    val feature = features.getJSONObject(i)
                    val text = feature.optString("text", "Place")
                    val placeName = feature.optString("place_name", text)
                    val center = feature.optJSONArray("center") ?: continue
                    val lng = center.getDouble(0)
                    val lat = center.getDouble(1)

                    val properties = feature.optJSONObject("properties")
                    val categoryStr = properties?.optString("category", "")?.lowercase() ?: ""

                    val category = when {
                        categoryStr.contains("gas") || categoryStr.contains("fuel") || text.lowercase().contains("petrol") -> PoiCategory.PETROL.name
                        categoryStr.contains("restaurant") || categoryStr.contains("food") || text.lowercase().contains("dhaba") || categoryStr.contains("cafe") -> PoiCategory.DHABA.name
                        categoryStr.contains("repair") || categoryStr.contains("auto") || text.lowercase().contains("mechanic") -> PoiCategory.MECHANIC.name
                        categoryStr.contains("viewpoint") || categoryStr.contains("attraction") -> PoiCategory.VIEWPOINT.name
                        categoryStr.contains("shop") || categoryStr.contains("store") || categoryStr.contains("grocery") || categoryStr.contains("supermarket") || categoryStr.contains("mall") -> PoiCategory.STORE.name
                        categoryStr.contains("hospital") || categoryStr.contains("pharmacy") || categoryStr.contains("medical") -> PoiCategory.HOSPITAL.name
                        else -> PoiCategory.STORE.name
                    }

                    list.add(
                        PoiEntity(
                            osmId = System.currentTimeMillis() + i,
                            name = text,
                            category = category,
                            latitude = lat,
                            longitude = lng,
                            brand = "Mapbox",
                            address = placeName,
                            regionId = "mapbox"
                        )
                    )
                }
            } else if (responseCode == 402 || responseCode == 429 || responseCode == 401 || responseCode == 403) {
                Log.w(TAG, "Mapbox Geocoding HTTP $responseCode. Quota or rate limit exceeded. Auto-switching.")
                ApiConfig.markMapboxQuotaExceeded("HTTP $responseCode in Places Geocoding")
            } else {
                Log.d(TAG, "Mapbox geocoding returned HTTP $responseCode")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Mapbox geocoding query failed: ${e.message}")
        }
        return list
    }

    /**
     * Downloads and populates all real detailed stores, fuel hubs, dhabas, garages, and medical places
     * for a downloaded state using Mapbox Places API, Overpass OSM, and regional datasets.
     */
    suspend fun populatePoisForRegion(
        region: com.motonav.data.model.MapRegion,
        onProgress: (String) -> Unit = {}
    ): Int = withContext(Dispatchers.IO) {
        val downloadedPois = mutableListOf<PoiEntity>()

        // 1. If West Bengal, seed the comprehensive regional dataset first
        if (region.id == "west_bengal") {
            onProgress("Populating verified West Bengal district stores & hubs...")
            try {
                val seed = com.motonav.data.local.db.MotoNavDatabase.getWestBengalInitialPois()
                poiDao.insertAll(seed)
                downloadedPois.addAll(seed)
            } catch (e: Exception) {
                Log.w(TAG, "Seed POI insert error: ${e.message}")
            }
        }

        // 2. Query Mapbox Places API for real stores & amenities across the state bounding box
        if (ApiConfig.isMapboxConfigured) {
            onProgress("Fetching detailed Mapbox stores & amenities for ${region.displayName}...")
            val searchCategories = listOf("supermarket store", "grocery store", "gas station petrol", "dhaba restaurant", "motorcycle repair", "pharmacy hospital")
            val token = ApiConfig.MAPBOX_ACCESS_TOKEN
            for (queryTerm in searchCategories) {
                try {
                    val encoded = URLEncoder.encode(queryTerm, "UTF-8")
                    val bboxStr = "${region.minLng},${region.minLat},${region.maxLng},${region.maxLat}"
                    val urlStr = "https://api.mapbox.com/geocoding/v5/mapbox.places/$encoded.json?bbox=$bboxStr&types=poi,address&limit=25&access_token=$token"
                    val conn = URL(urlStr).openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    conn.setRequestProperty("User-Agent", "MotoNav/1.0 (Android Motorcycle Navigation)")

                    if (conn.responseCode == 200) {
                        val body = conn.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(body)
                        val features = json.optJSONArray("features") ?: continue
                        for (i in 0 until features.length()) {
                            val feat = features.getJSONObject(i)
                            val center = feat.optJSONArray("center") ?: continue
                            val lng = center.getDouble(0)
                            val lat = center.getDouble(1)
                            val text = feat.optString("text")
                            val placeName = feat.optString("place_name")

                            val cat = when {
                                queryTerm.contains("petrol") || queryTerm.contains("gas") -> PoiCategory.PETROL.name
                                queryTerm.contains("dhaba") || queryTerm.contains("restaurant") -> PoiCategory.DHABA.name
                                queryTerm.contains("repair") || queryTerm.contains("motorcycle") -> PoiCategory.MECHANIC.name
                                queryTerm.contains("hospital") || queryTerm.contains("pharmacy") -> PoiCategory.HOSPITAL.name
                                else -> PoiCategory.STORE.name
                            }

                            val poi = PoiEntity(
                                osmId = 9000000L + downloadedPois.size + i,
                                name = text,
                                category = cat,
                                latitude = lat,
                                longitude = lng,
                                brand = "Mapbox Verified",
                                address = placeName,
                                regionId = region.id
                            )
                            downloadedPois.add(poi)
                        }
                    } else if (conn.responseCode in listOf(402, 429, 401, 403)) {
                        ApiConfig.markMapboxQuotaExceeded("State download HTTP ${conn.responseCode}")
                        break
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "Mapbox bbox query error for $queryTerm: ${e.message}")
                }
            }
        }

        // 3. Query OpenStreetMap Overpass API for real retail stores and amenities in the bounding box
        onProgress("Fetching OpenStreetMap shops, fuel, and roadside amenities...")
        try {
            val overpassQuery = """
                [out:json][timeout:8];
                (
                  node["shop"](${region.minLat},${region.minLng},${region.maxLat},${region.maxLng});
                  node["amenity"~"fuel|restaurant|cafe|pharmacy|hospital"](${region.minLat},${region.minLng},${region.maxLat},${region.maxLng});
                );
                out body 100;
            """.trimIndent()

            val conn = URL(OVERPASS_API).openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 6000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            conn.outputStream.use { os ->
                os.write("data=${URLEncoder.encode(overpassQuery, "UTF-8")}".toByteArray())
            }

            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val elements = json.optJSONArray("elements")
                if (elements != null) {
                    for (i in 0 until elements.length()) {
                        val elem = elements.getJSONObject(i)
                        val id = elem.optLong("id")
                        val lat = elem.optDouble("lat")
                        val lon = elem.optDouble("lon")
                        val tags = elem.optJSONObject("tags") ?: continue

                        val name = tags.optString("name").takeIf { it.isNotBlank() }
                            ?: tags.optString("brand").takeIf { it.isNotBlank() }
                            ?: tags.optString("operator").takeIf { it.isNotBlank() }
                            ?: continue

                        val shopVal = tags.optString("shop")
                        val amenityVal = tags.optString("amenity")

                        val cat = when {
                            amenityVal == "fuel" -> PoiCategory.PETROL.name
                            amenityVal in listOf("restaurant", "cafe", "fast_food") -> PoiCategory.DHABA.name
                            amenityVal in listOf("pharmacy", "hospital", "clinic") -> PoiCategory.HOSPITAL.name
                            shopVal in listOf("motorcycle", "car_repair", "tyres") -> PoiCategory.MECHANIC.name
                            shopVal.isNotBlank() -> PoiCategory.STORE.name
                            else -> PoiCategory.STORE.name
                        }

                        val brand = tags.optString("brand").takeIf { it.isNotBlank() }
                            ?: tags.optString("operator").takeIf { it.isNotBlank() }
                        val street = tags.optString("addr:street")
                        val city = tags.optString("addr:city")
                        val phone = tags.optString("phone").takeIf { it.isNotBlank() }
                            ?: tags.optString("contact:phone")
                        val hours = tags.optString("opening_hours")

                        val address = listOfNotNull(street.takeIf { it.isNotBlank() }, city.takeIf { it.isNotBlank() }).joinToString(", ")
                            .ifBlank { brand ?: "${region.displayName}, India" }

                        downloadedPois.add(
                            PoiEntity(
                                osmId = id,
                                name = name,
                                category = cat,
                                latitude = lat,
                                longitude = lon,
                                brand = brand,
                                address = address,
                                phone = phone,
                                openingHours = hours,
                                regionId = region.id
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Overpass bbox query error: ${e.message}")
        }

        // 4. Batch insert all collected POIs into Room SQLite DB
        if (downloadedPois.isNotEmpty()) {
            onProgress("Saving ${downloadedPois.size} places into local offline storage...")
            try {
                poiDao.insertAll(downloadedPois)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to insert downloaded POIs: ${e.message}")
            }
        }

        val totalCount = poiDao.getPoiCountByRegion(region.id)
        onProgress("Ready! $totalCount stores & amenities available offline.")
        totalCount
    }

    suspend fun getPoiCountForRegion(regionId: String): Int = withContext(Dispatchers.IO) {
        poiDao.getPoiCountByRegion(regionId)
    }

    fun observePoisForRegion(regionId: String): Flow<List<PoiEntity>> {
        return poiDao.observePoisByRegion(regionId)
    }
}
