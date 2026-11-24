package com.motonav.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.motonav.data.local.db.entities.RideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRide(ride: RideEntity): Long

    @Query("SELECT * FROM rides ORDER BY startTime DESC")
    fun getAllRides(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE id = :rideId")
    suspend fun getRideById(rideId: Long): RideEntity?

    @Query("DELETE FROM rides WHERE id = :rideId")
    suspend fun deleteRide(rideId: Long)
}
