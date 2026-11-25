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
    version = 3,
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
            )
            .fallbackToDestructiveMigration()
            .build()
        }

        suspend fun ensureInitialPoisSeeded(context: Context, database: MotoNavDatabase) {
            try {
                val count = database.poiDao().getPoiCountByRegion("west_bengal")
                if (count >= 30) return // Already seeded
                database.poiDao().insertAll(getWestBengalInitialPois())
            } catch (e: Exception) {
                // Ignore seeding errors
            }
        }

        fun getWestBengalInitialPois(): List<PoiEntity> {
            return listOf(
                // Major Towns & Motorcycle Hubs (including Purulia, Kolaghat, etc.)
                PoiEntity(
                    osmId = 10001L,
                    name = "Purulia",
                    category = PoiCategory.CITY.name,
                    latitude = 23.3322,
                    longitude = 86.3652,
                    brand = "District Headquarters",
                    address = "Purulia Town, West Bengal",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10002L,
                    name = "Ayodhya Hills (Purulia)",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 23.2100,
                    longitude = 86.1000,
                    brand = "Scenic Twisties",
                    address = "Ayodhya Hills, Baghmundi, Purulia",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10003L,
                    name = "Kolaghat",
                    category = PoiCategory.CITY.name,
                    latitude = 22.4278,
                    longitude = 87.8711,
                    brand = "NH16 Highway Hub",
                    address = "Kolaghat, Purba Medinipur, West Bengal",
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
                    name = "Kolkata (Park Street)",
                    category = PoiCategory.CITY.name,
                    latitude = 22.5529,
                    longitude = 88.3534,
                    brand = "State Capital",
                    address = "Park Street, Kolkata, West Bengal",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10006L,
                    name = "Howrah Station",
                    category = PoiCategory.CITY.name,
                    latitude = 22.5855,
                    longitude = 88.3426,
                    brand = "Transport Hub",
                    address = "Station Road, Howrah, West Bengal",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10007L,
                    name = "Digha (Sea Beach)",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 21.6266,
                    longitude = 87.5074,
                    brand = "Coastal Ride",
                    address = "Old Digha Beach, Purba Medinipur",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10008L,
                    name = "Mandarmani Beach",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 21.6667,
                    longitude = 87.7000,
                    brand = "Beach Highway",
                    address = "Mandarmani, Purba Medinipur",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10009L,
                    name = "Durgapur (City Centre)",
                    category = PoiCategory.CITY.name,
                    latitude = 23.5204,
                    longitude = 87.3119,
                    brand = "Steel City",
                    address = "City Centre, Durgapur, Paschim Bardhaman",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10010L,
                    name = "Asansol",
                    category = PoiCategory.CITY.name,
                    latitude = 23.6739,
                    longitude = 86.9524,
                    brand = "Industrial Hub",
                    address = "Asansol, Paschim Bardhaman",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10011L,
                    name = "Kharagpur (IIT)",
                    category = PoiCategory.CITY.name,
                    latitude = 22.3398,
                    longitude = 87.3253,
                    brand = "Railway & Tech Hub",
                    address = "Kharagpur, Paschim Medinipur",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10012L,
                    name = "Midnapore (Medinipur)",
                    category = PoiCategory.CITY.name,
                    latitude = 22.4257,
                    longitude = 87.3199,
                    brand = "District Headquarters",
                    address = "Midnapore Town, Paschim Medinipur",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10013L,
                    name = "Bankura",
                    category = PoiCategory.CITY.name,
                    latitude = 23.2324,
                    longitude = 87.0715,
                    brand = "District Headquarters",
                    address = "Bankura Town, West Bengal",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10014L,
                    name = "Bishnupur (Terracotta Temples)",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 23.0700,
                    longitude = 87.3200,
                    brand = "Heritage Ride",
                    address = "Bishnupur, Bankura",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10015L,
                    name = "Mukutmanipur Dam",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 22.9600,
                    longitude = 86.7800,
                    brand = "Scenic Reservoir",
                    address = "Mukutmanipur, Bankura",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10016L,
                    name = "Bolpur (Shantiniketan)",
                    category = PoiCategory.CITY.name,
                    latitude = 23.6800,
                    longitude = 87.6900,
                    brand = "Cultural Hub",
                    address = "Shantiniketan, Birbhum",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10017L,
                    name = "Bardhaman (Curzon Gate)",
                    category = PoiCategory.CITY.name,
                    latitude = 23.2324,
                    longitude = 87.8615,
                    brand = "Historic City",
                    address = "GT Road, Bardhaman",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10018L,
                    name = "Krishnanagar",
                    category = PoiCategory.CITY.name,
                    latitude = 23.4057,
                    longitude = 88.5028,
                    brand = "District Headquarters",
                    address = "Krishnanagar, Nadia",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10019L,
                    name = "Baharampur (Murshidabad)",
                    category = PoiCategory.CITY.name,
                    latitude = 24.0957,
                    longitude = 88.2520,
                    brand = "Historic City",
                    address = "Baharampur, Murshidabad",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10020L,
                    name = "Malda Town",
                    category = PoiCategory.CITY.name,
                    latitude = 25.0108,
                    longitude = 88.1411,
                    brand = "Mango City",
                    address = "English Bazar, Malda",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10021L,
                    name = "Siliguri (Sevoke Road)",
                    category = PoiCategory.CITY.name,
                    latitude = 26.7271,
                    longitude = 88.3953,
                    brand = "Gateway to Himalayas",
                    address = "Sevoke Road, Siliguri, Darjeeling",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10022L,
                    name = "Darjeeling (Mall Road)",
                    category = PoiCategory.CITY.name,
                    latitude = 27.0410,
                    longitude = 88.2663,
                    brand = "Queen of Hills",
                    address = "Chowrasta Mall, Darjeeling",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10023L,
                    name = "Kurseong (Hill Cart Road)",
                    category = PoiCategory.CITY.name,
                    latitude = 26.8833,
                    longitude = 88.2833,
                    brand = "Mountain Passes",
                    address = "Hill Cart Road, Kurseong",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10024L,
                    name = "Kalimpong",
                    category = PoiCategory.CITY.name,
                    latitude = 27.0667,
                    longitude = 88.4667,
                    brand = "Himalayan Ridge",
                    address = "Kalimpong Town, West Bengal",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10025L,
                    name = "Mirik Lake",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 26.8900,
                    longitude = 88.1800,
                    brand = "Tea Gardens & Lake",
                    address = "Sumendu Lake, Mirik",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10026L,
                    name = "Jhargram",
                    category = PoiCategory.CITY.name,
                    latitude = 22.4500,
                    longitude = 86.9800,
                    brand = "Jungle Mahal",
                    address = "Jhargram Town, West Bengal",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10027L,
                    name = "Haldia Port",
                    category = PoiCategory.CITY.name,
                    latitude = 22.0667,
                    longitude = 88.0667,
                    brand = "Port City",
                    address = "Haldia, Purba Medinipur",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10028L,
                    name = "Tarapith Temple",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 24.1100,
                    longitude = 87.8000,
                    brand = "Pilgrimage Stop",
                    address = "Tarapith, Rampurhat, Birbhum",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10029L,
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
                    osmId = 10030L,
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
                    osmId = 10031L,
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
                    osmId = 10032L,
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
                    osmId = 10033L,
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
                    osmId = 10034L,
                    name = "Tiger Hill Sunrise Viewpoint",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 27.0064,
                    longitude = 88.2858,
                    brand = "Scenic Stop",
                    address = "Tiger Hill Rd, Darjeeling",
                    openingHours = "04:00-18:00",
                    regionId = "west_bengal"
                ),
                // Detailed Stores, Supermarkets & Roadside General Shops in West Bengal
                PoiEntity(
                    osmId = 10035L,
                    name = "Spencer's Hyper Store (South City Mall)",
                    category = PoiCategory.STORE.name,
                    latitude = 22.5020,
                    longitude = 88.3620,
                    brand = "Spencer's Retail",
                    address = "South City Mall, Prince Anwar Shah Rd, Kolkata",
                    openingHours = "10:00 - 22:00",
                    phone = "+91 33 4007 2000",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10036L,
                    name = "More Supermarket (Salt Lake Sector 1)",
                    category = PoiCategory.STORE.name,
                    latitude = 22.5890,
                    longitude = 88.4080,
                    brand = "More Retail",
                    address = "Block BD, Sector 1, Bidhannagar, Kolkata",
                    openingHours = "08:00 - 22:00",
                    phone = "+91 33 2337 1100",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10037L,
                    name = "Smart Bazaar (City Centre 2, Rajarhat)",
                    category = PoiCategory.STORE.name,
                    latitude = 22.6240,
                    longitude = 88.4520,
                    brand = "Reliance Retail",
                    address = "City Centre 2, New Town, Rajarhat",
                    openingHours = "09:00 - 22:30",
                    phone = "+91 33 2526 8000",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10038L,
                    name = "Dankuni Highway Convenience & Departmental Store",
                    category = PoiCategory.STORE.name,
                    latitude = 22.6820,
                    longitude = 88.2930,
                    brand = "Highway Stop Mart",
                    address = "NH 19, Dankuni Crossing, Hooghly",
                    openingHours = "24/7",
                    phone = "+91 98301 23456",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10039L,
                    name = "Kolaghat NH16 Express Mart & Daily Needs",
                    category = PoiCategory.STORE.name,
                    latitude = 22.4290,
                    longitude = 87.8720,
                    brand = "Bikers Mart",
                    address = "NH 16 Kolaghat Bypass, Purba Medinipur",
                    openingHours = "24/7",
                    phone = "+91 94340 88211",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10040L,
                    name = "Keventers Heritage Cafe & Store (Darjeeling)",
                    category = PoiCategory.DHABA.name,
                    latitude = 27.0425,
                    longitude = 88.2650,
                    brand = "Heritage Biker Cafe",
                    address = "1 Nehru Road, Chowrasta, Darjeeling",
                    openingHours = "07:30 - 19:30",
                    phone = "+91 354 225 2026",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10041L,
                    name = "Glenary's Bakery & Cafe (Mall Road)",
                    category = PoiCategory.DHABA.name,
                    latitude = 27.0430,
                    longitude = 88.2655,
                    brand = "Glenary's",
                    address = "Nehru Road, Darjeeling",
                    openingHours = "07:00 - 21:00",
                    phone = "+91 354 225 4059",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10042L,
                    name = "Siliguri Sevoke Road Central Mart & Spares",
                    category = PoiCategory.STORE.name,
                    latitude = 26.7290,
                    longitude = 88.3970,
                    brand = "Sevoke Mart",
                    address = "Sevoke Road, Siliguri, Darjeeling",
                    openingHours = "08:30 - 22:00",
                    phone = "+91 353 253 1001",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10043L,
                    name = "Durgapur Junction Mall & Reliance Mart",
                    category = PoiCategory.STORE.name,
                    latitude = 23.5350,
                    longitude = 87.2920,
                    brand = "Junction Mall",
                    address = "City Centre, Durgapur, Paschim Bardhaman",
                    openingHours = "10:00 - 21:30",
                    phone = "+91 343 254 9900",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10044L,
                    name = "Asansol Galaxy Mall & Superstore",
                    category = PoiCategory.STORE.name,
                    latitude = 23.6820,
                    longitude = 86.9610,
                    brand = "Galaxy Mall",
                    address = "Burnpur Road, Asansol",
                    openingHours = "10:00 - 22:00",
                    phone = "+91 341 225 3300",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10045L,
                    name = "Purulia District Supermarket & Store",
                    category = PoiCategory.STORE.name,
                    latitude = 23.3340,
                    longitude = 86.3680,
                    brand = "Town Bazaar",
                    address = "Main Road, Purulia Town",
                    openingHours = "08:00 - 21:00",
                    phone = "+91 97320 11223",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10046L,
                    name = "Baghmundi Hills Daily Needs & Tea Stall",
                    category = PoiCategory.STORE.name,
                    latitude = 23.2050,
                    longitude = 86.0520,
                    brand = "Ayodhya Foothills",
                    address = "Baghmundi Bazar, Purulia",
                    openingHours = "06:00 - 21:00",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10047L,
                    name = "Apollo Pharmacy 24/7 (Kolkata Park Street)",
                    category = PoiCategory.HOSPITAL.name,
                    latitude = 22.5535,
                    longitude = 88.3540,
                    brand = "Apollo Pharmacy",
                    address = "Park Street, Kolkata",
                    openingHours = "24/7",
                    phone = "+91 33 2229 1111",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10048L,
                    name = "Midnapore City Bazaar & Groceries",
                    category = PoiCategory.STORE.name,
                    latitude = 22.4265,
                    longitude = 87.3210,
                    brand = "Local Store",
                    address = "Golkuan Chowk, Midnapore Town",
                    openingHours = "08:00 - 21:30",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10049L,
                    name = "Kharagpur Technology Market (IIT Campus)",
                    category = PoiCategory.STORE.name,
                    latitude = 22.3180,
                    longitude = 87.3090,
                    brand = "Tech Market",
                    address = "IIT Kharagpur Campus, Paschim Medinipur",
                    openingHours = "08:00 - 22:00",
                    phone = "+91 3222 282 000",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10050L,
                    name = "Burdwan Curzon Gate Departmental Store",
                    category = PoiCategory.STORE.name,
                    latitude = 23.2350,
                    longitude = 87.8630,
                    brand = "Heritage Store",
                    address = "GT Road, Curzon Gate, Bardhaman",
                    openingHours = "09:00 - 21:00",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10051L,
                    name = "Azad Hind Dhaba (NH16 Uluberia)",
                    category = PoiCategory.DHABA.name,
                    latitude = 22.4820,
                    longitude = 88.1120,
                    brand = "Azad Hind",
                    address = "NH 16, Uluberia Highway, Howrah",
                    openingHours = "24/7",
                    phone = "+91 33 2661 5000",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10052L,
                    name = "Hindustan Dhaba & Family Restaurant (NH19)",
                    category = PoiCategory.DHABA.name,
                    latitude = 23.2100,
                    longitude = 87.8900,
                    brand = "Hindustan Dhaba",
                    address = "NH 19, Palsit Toll Plaza, Bardhaman",
                    openingHours = "24/7",
                    phone = "+91 94341 55667",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10053L,
                    name = "Hero MotoCorp Authorized Workshop & Spares",
                    category = PoiCategory.MECHANIC.name,
                    latitude = 22.6100,
                    longitude = 88.3750,
                    brand = "Hero MotoCorp",
                    address = "Belgachia Road, Kolkata",
                    openingHours = "09:00 - 19:00",
                    phone = "+91 33 2556 4000",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10054L,
                    name = "Bajaj & KTM Service Point (Durgapur)",
                    category = PoiCategory.MECHANIC.name,
                    latitude = 23.5180,
                    longitude = 87.3150,
                    brand = "Bajaj / KTM",
                    address = "Nachan Road, Benachity, Durgapur",
                    openingHours = "09:30 - 19:30",
                    phone = "+91 343 258 7766",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10055L,
                    name = "Old Digha Sea Beach Roadside Mart & Fish Stalls",
                    category = PoiCategory.STORE.name,
                    latitude = 21.6280,
                    longitude = 87.5090,
                    brand = "Beachside Market",
                    address = "Sea Beach Road, Digha",
                    openingHours = "07:00 - 23:00",
                    regionId = "west_bengal"
                )
            )
        }
    }
}
