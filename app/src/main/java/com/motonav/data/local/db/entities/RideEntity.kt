package com.motonav.data.local.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rides")
data class RideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val distanceKm: Double,
    val durationSeconds: Long,
    val maxSpeedKmh: Double,
    val avgSpeedKmh: Double,
    val encodedPolyline: String,
    val startAddress: String? = null,
    val endAddress: String? = null
)
