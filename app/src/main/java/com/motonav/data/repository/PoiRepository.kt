package com.motonav.data.repository

import android.content.Context
import android.location.Address
import android.location.Geocoder
import com.motonav.data.local.db.PoiDao
import com.motonav.data.local.db.entities.PoiCategory
import com.motonav.data.local.db.entities.PoiEntity
import com.motonav.data.model.LatLng
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoiRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val poiDao: PoiDao
) {
    suspend fun getNearbyPois(
        userLocation: LatLng,
        category: PoiCategory? = null,
        radiusKm: Double = 15.0,
        limit: Int = 30
    ): List<PoiEntity> = withContext(Dispatchers.IO) {
        // Approximate 1 deg lat ~ 111 km, 1 deg lng ~ 111 km * cos(lat)
        val latDelta = radiusKm / 111.0
        val lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(userLocation.latitude)))

        val minLat = userLocation.latitude - latDelta
        val maxLat = userLocation.latitude + latDelta
        val minLng = userLocation.longitude - lngDelta
        val maxLng = userLocation.longitude + lngDelta

        if (category != null) {
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
        }
    }

    suspend fun searchPois(query: String, limit: Int = 20): List<PoiEntity> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return@withContext emptyList()

        val results = poiDao.searchByName(trimmed, limit).toMutableList()

        // If geocoder is available on device and online, fetch matches for towns/landmarks
        try {
            if (Geocoder.isPresent()) {
                val geocoder = Geocoder(context, Locale("en", "IN"))
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(trimmed, 5)
                if (!addresses.isNullOrEmpty()) {
                    var genId = 990000L
                    for (addr in addresses) {
                        val placeName = addr.featureName ?: addr.locality ?: trimmed
                        val exists = results.any {
                            it.name.equals(placeName, ignoreCase = true) ||
                            (Math.abs(it.latitude - addr.latitude) < 0.02 && Math.abs(it.longitude - addr.longitude) < 0.02)
                        }
                        if (!exists) {
                            results.add(
                                PoiEntity(
                                    osmId = genId++,
                                    name = placeName,
                                    category = PoiCategory.CITY.name,
                                    latitude = addr.latitude,
                                    longitude = addr.longitude,
                                    brand = addr.locality ?: addr.adminArea ?: "West Bengal",
                                    address = addr.getAddressLine(0) ?: "$placeName, West Bengal",
                                    regionId = "west_bengal"
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Offline fallback - use database results
        }

        results.take(limit)
    }

    suspend fun getPoiCountForRegion(regionId: String): Int = withContext(Dispatchers.IO) {
        poiDao.getPoiCountByRegion(regionId)
    }

    fun observePoisForRegion(regionId: String): Flow<List<PoiEntity>> {
        return poiDao.observePoisByRegion(regionId)
    }
}
