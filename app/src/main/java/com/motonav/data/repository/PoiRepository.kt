package com.motonav.data.repository

import com.motonav.data.local.db.PoiDao
import com.motonav.data.local.db.entities.PoiCategory
import com.motonav.data.local.db.entities.PoiEntity
import com.motonav.data.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoiRepository @Inject constructor(
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
        if (query.isBlank()) return@withContext emptyList()
        poiDao.searchByName(query.trim(), limit)
    }

    suspend fun getPoiCountForRegion(regionId: String): Int = withContext(Dispatchers.IO) {
        poiDao.getPoiCountByRegion(regionId)
    }

    fun observePoisForRegion(regionId: String): Flow<List<PoiEntity>> {
        return poiDao.observePoisByRegion(regionId)
    }
}
