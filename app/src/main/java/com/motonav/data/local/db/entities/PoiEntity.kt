package com.motonav.data.local.db.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PoiCategory {
    PETROL,
    RESTAURANT,
    DHABA,
    MECHANIC,
    ATM,
    HOSPITAL,
    HOTEL,
    VIEWPOINT,
    WATER,
    STORE,
    CITY
}

@Entity(
    tableName = "pois",
    indices = [
        Index(value = ["category"]),
        Index(value = ["latitude", "longitude"]),
        Index(value = ["regionId"]),
        Index(value = ["name"])
    ]
)
data class PoiEntity(
    @PrimaryKey
    val osmId: Long,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val brand: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val openingHours: String? = null,
    val regionId: String = "west_bengal",
    val imageUrl: String? = null
)
