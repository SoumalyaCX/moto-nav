package com.motonav.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.motonav.data.local.db.entities.PoiEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PoiDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pois: List<PoiEntity>)

    @Query("""
        SELECT * FROM pois 
        WHERE latitude BETWEEN :minLat AND :maxLat 
          AND longitude BETWEEN :minLng AND :maxLng
        ORDER BY ((:userLat - latitude) * (:userLat - latitude) + (:userLng - longitude) * (:userLng - longitude)) ASC
        LIMIT :limit
    """)
    suspend fun getNearbyPois(
        userLat: Double,
        userLng: Double,
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        limit: Int = 30
    ): List<PoiEntity>

    @Query("""
        SELECT * FROM pois 
        WHERE category = :category
          AND latitude BETWEEN :minLat AND :maxLat 
          AND longitude BETWEEN :minLng AND :maxLng
        ORDER BY ((:userLat - latitude) * (:userLat - latitude) + (:userLng - longitude) * (:userLng - longitude)) ASC
        LIMIT :limit
    """)
    suspend fun getNearbyByCategory(
        category: String,
        userLat: Double,
        userLng: Double,
        minLat: Double,
        maxLat: Double,
        minLng: Double,
        maxLng: Double,
        limit: Int = 20
    ): List<PoiEntity>

    @Query("""
        SELECT * FROM pois 
        WHERE name LIKE '%' || :query || '%' 
           OR brand LIKE '%' || :query || '%'
           OR address LIKE '%' || :query || '%'
        ORDER BY 
            CASE 
                WHEN name LIKE :query || '%' THEN 1 
                WHEN name LIKE '%' || :query || '%' THEN 2 
                ELSE 3 
            END,
            name ASC
        LIMIT :limit
    """)
    suspend fun searchByName(query: String, limit: Int = 25): List<PoiEntity>

    @Query("""
        SELECT * FROM pois 
        WHERE name LIKE '%' || :query || '%' 
           OR brand LIKE '%' || :query || '%'
           OR address LIKE '%' || :query || '%'
        ORDER BY 
            ((:userLat - latitude) * (:userLat - latitude) + (:userLng - longitude) * (:userLng - longitude)) ASC
        LIMIT :limit
    """)
    suspend fun searchByNameNear(
        query: String,
        userLat: Double,
        userLng: Double,
        limit: Int = 30
    ): List<PoiEntity>

    @Query("SELECT COUNT(*) FROM pois WHERE regionId = :regionId")
    suspend fun getPoiCountByRegion(regionId: String): Int

    @Query("SELECT * FROM pois WHERE regionId = :regionId LIMIT 100")
    fun observePoisByRegion(regionId: String): Flow<List<PoiEntity>>
}
