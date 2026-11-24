package com.motonav.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.motonav.data.local.db.entities.PoiCategory
import com.motonav.data.local.db.entities.PoiEntity
import com.motonav.data.local.db.entities.RideEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [PoiEntity::class, RideEntity::class],
    version = 1,
    exportSchema = false
)
abstract class MotoNavDatabase : RoomDatabase() {

    abstract fun poiDao(): PoiDao
    abstract fun rideDao(): RideDao

    companion object {
        const val DATABASE_NAME = "motonav.db"

        fun buildDatabase(context: Context, scope: CoroutineScope): MotoNavDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                MotoNavDatabase::class.java,
                DATABASE_NAME
            ).addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // Seed initial essential West Bengal motorcycle route POIs
                    scope.launch(Dispatchers.IO) {
                        seedWestBengalInitialPois(context)
                    }
                }
            }).build()
        }

        private suspend fun seedWestBengalInitialPois(context: Context) {
            val initialPois = listOf(
                PoiEntity(
                    osmId = 10001L,
                    name = "Indian Oil Petrol Pump (NH19 Dankuni)",
                    category = PoiCategory.PETROL.name,
                    latitude = 22.6845,
                    longitude = 88.2912,
                    brand = "Indian Oil",
                    address = "NH 19, Dankuni Toll Plaza, Hooghly",
                    openingHours = "24/7",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10002L,
                    name = "Bharat Petroleum (Kona Expressway)",
                    category = PoiCategory.PETROL.name,
                    latitude = 22.5856,
                    longitude = 88.2984,
                    brand = "Bharat Petroleum",
                    address = "NH 16, Kona Expressway, Howrah",
                    openingHours = "24/7",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10003L,
                    name = "HP Fuel Center (NH12 Barasat)",
                    category = PoiCategory.PETROL.name,
                    latitude = 22.7231,
                    longitude = 88.4820,
                    brand = "HP",
                    address = "Jessore Road, Barasat",
                    openingHours = "24/7",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10004L,
                    name = "Sher-e-Punjab Dhaba (Kolaghat)",
                    category = PoiCategory.DHABA.name,
                    latitude = 22.4278,
                    longitude = 87.8711,
                    brand = "Sher-e-Punjab",
                    address = "NH 16, Kolaghat Bypass, Purba Medinipur",
                    openingHours = "24/7",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10005L,
                    name = "Royal Enfield Service & Spares (Siliguri)",
                    category = PoiCategory.MECHANIC.name,
                    latitude = 26.7271,
                    longitude = 88.3953,
                    brand = "Royal Enfield",
                    address = "Sevoke Road, Siliguri",
                    openingHours = "09:30-19:00",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10006L,
                    name = "Darjeeling Hill Road Motorcycle Workshop",
                    category = PoiCategory.MECHANIC.name,
                    latitude = 27.0360,
                    longitude = 88.2627,
                    brand = "Independent Mechanic",
                    address = "Hill Cart Road, Kurseong",
                    openingHours = "08:00-20:00",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10007L,
                    name = "Tiger Hill Sunrise Viewpoint",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 27.0064,
                    longitude = 88.2858,
                    brand = "Scenic Stop",
                    address = "Tiger Hill Rd, Darjeeling",
                    openingHours = "04:00-18:00",
                    regionId = "west_bengal"
                )
            )
            // Insert directly
            val db = Room.databaseBuilder(context, MotoNavDatabase::class.java, DATABASE_NAME).build()
            db.poiDao().insertAll(initialPois)
        }
    }
}
