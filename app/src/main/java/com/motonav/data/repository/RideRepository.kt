package com.motonav.data.repository

import com.motonav.data.local.db.RideDao
import com.motonav.data.local.db.entities.RideEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRepository @Inject constructor(
    private val rideDao: RideDao
) {
    fun getAllRides(): Flow<List<RideEntity>> = rideDao.getAllRides()

    suspend fun getRideById(id: Long): RideEntity? = withContext(Dispatchers.IO) {
        rideDao.getRideById(id)
    }

    suspend fun saveRide(
        title: String,
        startTime: Long,
        endTime: Long,
        distanceKm: Double,
        maxSpeedKmh: Double,
        avgSpeedKmh: Double,
        encodedPolyline: String = "",
        startAddress: String? = null,
        endAddress: String? = null,
        maxLeanLeft: Int = 0,
        maxLeanRight: Int = 0
    ): Long = withContext(Dispatchers.IO) {
        val durationSeconds = (endTime - startTime) / 1000
        val ride = RideEntity(
            title = title,
            startTime = startTime,
            endTime = endTime,
            distanceKm = distanceKm,
            durationSeconds = if (durationSeconds > 0) durationSeconds else 1,
            maxSpeedKmh = maxSpeedKmh,
            avgSpeedKmh = avgSpeedKmh,
            encodedPolyline = encodedPolyline,
            startAddress = startAddress,
            endAddress = endAddress,
            maxLeanLeft = maxLeanLeft,
            maxLeanRight = maxLeanRight
        )
        rideDao.insertRide(ride)
    }

    suspend fun deleteRide(id: Long) = withContext(Dispatchers.IO) {
        rideDao.deleteRide(id)
    }
}
