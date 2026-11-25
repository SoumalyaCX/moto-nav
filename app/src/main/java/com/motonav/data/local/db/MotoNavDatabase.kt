package com.motonav.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.motonav.data.local.db.entities.PoiCategory
import com.motonav.data.local.db.entities.PoiEntity
import com.motonav.data.local.db.entities.RideEntity
import kotlinx.coroutines.CoroutineScope

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
                val initialList = getWestBengalInitialPois()
                database.poiDao().insertAll(initialList)
            } catch (e: Exception) {
                // Ignore seeding errors
            }
        }

        fun getWestBengalInitialPois(): List<PoiEntity> {
            return listOf(
                // ==============================================================
                // 1. MAJOR TOWNS & REGIONAL HUBS (CITY)
                // ==============================================================
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

                // ==============================================================
                // 2. SCENIC VIEWPOINTS & MOUNTAIN PASSES (VIEWPOINT)
                // ==============================================================
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
                    osmId = 10034L,
                    name = "Tiger Hill Sunrise Viewpoint",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 27.0064,
                    longitude = 88.2858,
                    brand = "Scenic Sunrise",
                    address = "Tiger Hill Rd, Darjeeling",
                    openingHours = "04:00 - 18:00",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10015L,
                    name = "Mukutmanipur Dam & Reservoir",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 22.9600,
                    longitude = 86.7800,
                    brand = "Scenic Dam Ride",
                    address = "Mukutmanipur, Bankura",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10070L,
                    name = "Bamni Falls (Ayodhya Hills)",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 23.1950,
                    longitude = 86.0820,
                    brand = "Waterfall Ride",
                    address = "Baghmundi, Purulia",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10071L,
                    name = "Joychandi Pahar (Raghunathpur)",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 23.5510,
                    longitude = 86.6620,
                    brand = "Rock Climbing & View",
                    address = "Raghunathpur, Purulia",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10072L,
                    name = "Maithon Dam & Lake Viewpoint",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 23.7820,
                    longitude = 86.8120,
                    brand = "Lake Panorama",
                    address = "Maithon, Paschim Bardhaman",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10073L,
                    name = "Batasia Loop Panoramic View",
                    category = PoiCategory.VIEWPOINT.name,
                    latitude = 27.0167,
                    longitude = 88.2500,
                    brand = "Himalayan Railway Loop",
                    address = "Ghoom, Darjeeling",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 3. PARKS, LAKES & BEACHES (PARK)
                // ==============================================================
                PoiEntity(
                    osmId = 10007L,
                    name = "Digha Sea Beach",
                    category = PoiCategory.PARK.name,
                    latitude = 21.6266,
                    longitude = 87.5074,
                    brand = "Coastal Promenade",
                    address = "Old Digha Beach, Purba Medinipur",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10008L,
                    name = "Mandarmani Beach",
                    category = PoiCategory.PARK.name,
                    latitude = 21.6667,
                    longitude = 87.7000,
                    brand = "Beach Highway",
                    address = "Mandarmani Beach, Purba Medinipur",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10025L,
                    name = "Mirik Lake (Sumendu Lake)",
                    category = PoiCategory.PARK.name,
                    latitude = 26.8900,
                    longitude = 88.1800,
                    brand = "Tea Gardens & Boating",
                    address = "Sumendu Lake, Mirik, Darjeeling",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10074L,
                    name = "Eco Park (New Town)",
                    category = PoiCategory.PARK.name,
                    latitude = 22.6025,
                    longitude = 88.4665,
                    brand = "Urban Nature Park",
                    address = "Major Arterial Road, Action Area II, New Town",
                    openingHours = "12:00 - 20:30",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10075L,
                    name = "Nicco Park",
                    category = PoiCategory.PARK.name,
                    latitude = 22.5714,
                    longitude = 88.4230,
                    brand = "Theme & Water Park",
                    address = "HM Block, Sector IV, Bidhannagar, Kolkata",
                    openingHours = "10:30 - 18:30",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10076L,
                    name = "Alipore Zoological Gardens",
                    category = PoiCategory.PARK.name,
                    latitude = 22.5350,
                    longitude = 88.3325,
                    brand = "Nature Zoo",
                    address = "Alipore Road, Kolkata",
                    openingHours = "09:00 - 17:00",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 4. HISTORIC MONUMENTS & HERITAGE (ATTRACTION)
                // ==============================================================
                PoiEntity(
                    osmId = 10077L,
                    name = "Victoria Memorial",
                    category = PoiCategory.ATTRACTION.name,
                    latitude = 22.5448,
                    longitude = 88.3426,
                    brand = "Historic Marble Monument",
                    address = "Queens Way, Maidan, Kolkata 700071",
                    openingHours = "10:00 - 18:00",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10078L,
                    name = "Howrah Bridge (Rabindra Setu)",
                    category = PoiCategory.ATTRACTION.name,
                    latitude = 22.5851,
                    longitude = 88.3468,
                    brand = "Iconic Cantilever Bridge",
                    address = "Howrah Bridge Approach Rd, Kolkata",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10079L,
                    name = "Princep Ghat",
                    category = PoiCategory.ATTRACTION.name,
                    latitude = 22.5562,
                    longitude = 88.3302,
                    brand = "Historic Riverfront Ghat",
                    address = "Strand Road, Maidan, Kolkata",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10080L,
                    name = "Science City Kolkata",
                    category = PoiCategory.ATTRACTION.name,
                    latitude = 22.5401,
                    longitude = 88.3962,
                    brand = "Science Centre & Planetarium",
                    address = "JBS Haldane Avenue, EM Bypass, Kolkata",
                    openingHours = "09:00 - 20:00",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10081L,
                    name = "Indian Museum Kolkata",
                    category = PoiCategory.ATTRACTION.name,
                    latitude = 22.5579,
                    longitude = 88.3511,
                    brand = "National Museum",
                    address = "27 Jawaharlal Nehru Road, Colootola, Kolkata",
                    openingHours = "10:00 - 18:00",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 5. TEMPLES & PLACES OF WORSHIP (TEMPLE)
                // ==============================================================
                PoiEntity(
                    osmId = 10082L,
                    name = "Dakshineswar Kali Temple",
                    category = PoiCategory.TEMPLE.name,
                    latitude = 22.6550,
                    longitude = 88.3575,
                    brand = "Historic Kali Temple",
                    address = "Dakshineswar, Kolkata 700076",
                    openingHours = "06:00 - 20:30",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10083L,
                    name = "Belur Math",
                    category = PoiCategory.TEMPLE.name,
                    latitude = 22.6319,
                    longitude = 88.3562,
                    brand = "Ramakrishna Math Headquarters",
                    address = "Belur, Howrah 711202",
                    openingHours = "06:30 - 20:30",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10084L,
                    name = "Kalighat Temple",
                    category = PoiCategory.TEMPLE.name,
                    latitude = 22.5201,
                    longitude = 88.3427,
                    brand = "Shakti Peeth",
                    address = "Anami Sangha, Kalighat, Kolkata",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10014L,
                    name = "Bishnupur (Terracotta Temples)",
                    category = PoiCategory.TEMPLE.name,
                    latitude = 23.0700,
                    longitude = 87.3200,
                    brand = "17th Century Terracotta Architecture",
                    address = "Bishnupur, Bankura",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10028L,
                    name = "Tarapith Temple",
                    category = PoiCategory.TEMPLE.name,
                    latitude = 24.1100,
                    longitude = 87.8000,
                    brand = "Tantric Pilgrimage Temple",
                    address = "Tarapith, Rampurhat, Birbhum",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 6. HIGHWAY DHABAS & MOTORCYCLE FOOD (DHABA)
                // ==============================================================
                PoiEntity(
                    osmId = 10004L,
                    name = "Sher-e-Punjab Dhaba (Kolaghat)",
                    category = PoiCategory.DHABA.name,
                    latitude = 22.4278,
                    longitude = 87.8711,
                    brand = "Sher-e-Punjab",
                    address = "NH 16, Kolaghat Bypass, Purba Medinipur",
                    openingHours = "24/7",
                    phone = "+91 98310 11223",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10085L,
                    name = "Azad Hind Dhaba (NH19 Dankuni)",
                    category = PoiCategory.DHABA.name,
                    latitude = 22.6850,
                    longitude = 88.2930,
                    brand = "Azad Hind",
                    address = "NH 19 Highway Stop, Dankuni, Hooghly",
                    openingHours = "24/7",
                    phone = "+91 33 2659 1234",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10086L,
                    name = "Balwant Singh Dhaba (Bhawanipore)",
                    category = PoiCategory.DHABA.name,
                    latitude = 22.5342,
                    longitude = 88.3490,
                    brand = "Late Night Biker Dhaba",
                    address = "Harish Mukherjee Rd, Bhawanipore, Kolkata",
                    openingHours = "24/7",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10087L,
                    name = "Shaktigarh Langcha & Highway Dhaba Hub",
                    category = PoiCategory.DHABA.name,
                    latitude = 23.2120,
                    longitude = 87.9620,
                    brand = "Langcha Mahal",
                    address = "NH 19, Shaktigarh, Purba Bardhaman",
                    openingHours = "06:00 - 23:30",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10088L,
                    name = "Kolaghat Express Food Plaza",
                    category = PoiCategory.DHABA.name,
                    latitude = 22.4295,
                    longitude = 87.8725,
                    brand = "Express Highway Stop",
                    address = "NH 16 Bypass, Kolaghat",
                    openingHours = "24/7",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 7. RESTAURANTS & FINE DINING (RESTAURANT)
                // ==============================================================
                PoiEntity(
                    osmId = 10089L,
                    name = "Peter Cat (Park Street)",
                    category = PoiCategory.RESTAURANT.name,
                    latitude = 22.5520,
                    longitude = 88.3545,
                    brand = "Chelo Kebab Heritage",
                    address = "18A Park Street, Kolkata 700016",
                    openingHours = "11:30 - 23:00",
                    phone = "+91 33 2229 8841",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10090L,
                    name = "Arsalan (Park Circus 7-Point)",
                    category = PoiCategory.RESTAURANT.name,
                    latitude = 22.5442,
                    longitude = 88.3685,
                    brand = "Kolkata Biryani",
                    address = "191 Marina Park Circus, Kolkata 700017",
                    openingHours = "11:00 - 23:30",
                    phone = "+91 33 2284 9191",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10091L,
                    name = "Oudh 1590 (Deshapriya Park)",
                    category = PoiCategory.RESTAURANT.name,
                    latitude = 22.5180,
                    longitude = 88.3570,
                    brand = "Awadhi Dining",
                    address = "23 Deshapriya Park West, Kolkata",
                    openingHours = "12:00 - 22:30",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10092L,
                    name = "Aminia Restaurant (New Market)",
                    category = PoiCategory.RESTAURANT.name,
                    latitude = 22.5600,
                    longitude = 88.3525,
                    brand = "Mughlai & Biryani",
                    address = "6A SN Banerjee Rd, New Market, Kolkata",
                    openingHours = "11:30 - 23:00",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 8. CAFES & BAKERIES (CAFE)
                // ==============================================================
                PoiEntity(
                    osmId = 10040L,
                    name = "Keventers Heritage Cafe (Darjeeling)",
                    category = PoiCategory.CAFE.name,
                    latitude = 27.0425,
                    longitude = 88.2650,
                    brand = "Heritage Open-Air Terrace",
                    address = "1 Nehru Road, Chowrasta, Darjeeling",
                    openingHours = "07:30 - 19:30",
                    phone = "+91 354 225 2026",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10041L,
                    name = "Glenary's Bakery & Cafe (Darjeeling)",
                    category = PoiCategory.CAFE.name,
                    latitude = 27.0430,
                    longitude = 88.2655,
                    brand = "Glenary's",
                    address = "Nehru Road, Darjeeling",
                    openingHours = "07:00 - 21:00",
                    phone = "+91 354 225 4059",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10093L,
                    name = "Indian Coffee House (College Street)",
                    category = PoiCategory.CAFE.name,
                    latitude = 22.5746,
                    longitude = 88.3630,
                    brand = "Historic Intellectual Cafe",
                    address = "15 Bankim Chatterjee St, College Square, Kolkata",
                    openingHours = "09:00 - 21:00",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10094L,
                    name = "Flurys Tea Room (Park Street)",
                    category = PoiCategory.CAFE.name,
                    latitude = 22.5528,
                    longitude = 88.3538,
                    brand = "European Tea & Bakery",
                    address = "18 Park Street, Kolkata 700071",
                    openingHours = "07:30 - 22:30",
                    phone = "+91 33 4000 7453",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10095L,
                    name = "Blue Tokai Coffee Roasters (Park Street)",
                    category = PoiCategory.CAFE.name,
                    latitude = 22.5515,
                    longitude = 88.3540,
                    brand = "Specialty Coffee Roastery",
                    address = "Park Mansions, Park Street, Kolkata",
                    openingHours = "08:00 - 22:00",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10096L,
                    name = "Cafe Ekante (Eco Park Island)",
                    category = PoiCategory.CAFE.name,
                    latitude = 22.6030,
                    longitude = 88.4680,
                    brand = "Island Lake Cafe",
                    address = "Eco Park Island, New Town, Kolkata",
                    openingHours = "13:00 - 22:00",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 9. HOTELS & LODGING (HOTEL)
                // ==============================================================
                PoiEntity(
                    osmId = 10097L,
                    name = "The Oberoi Grand Kolkata",
                    category = PoiCategory.HOTEL.name,
                    latitude = 22.5620,
                    longitude = 88.3518,
                    brand = "Luxury 5-Star Heritage Hotel",
                    address = "15 Jawaharlal Nehru Rd, New Market, Kolkata",
                    openingHours = "24/7",
                    phone = "+91 33 2249 2323",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10098L,
                    name = "ITC Royal Bengal & ITC Sonar",
                    category = PoiCategory.HOTEL.name,
                    latitude = 22.5408,
                    longitude = 88.3980,
                    brand = "ITC Luxury Collection",
                    address = "1 JBS Haldane Avenue, EM Bypass, Kolkata",
                    openingHours = "24/7",
                    phone = "+91 33 4446 4646",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10099L,
                    name = "Taj Bengal Kolkata",
                    category = PoiCategory.HOTEL.name,
                    latitude = 22.5358,
                    longitude = 88.3320,
                    brand = "Taj Luxury",
                    address = "34B Belvedere Road, Alipore, Kolkata",
                    openingHours = "24/7",
                    phone = "+91 33 6612 3939",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10100L,
                    name = "The Westin Kolkata Rajarhat",
                    category = PoiCategory.HOTEL.name,
                    latitude = 22.6120,
                    longitude = 88.4720,
                    brand = "Marriott International",
                    address = "International Financial Hub, New Town, Rajarhat",
                    openingHours = "24/7",
                    phone = "+91 33 4037 1234",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 10. MOTORCYCLE WORKSHOPS & MECHANICS (MECHANIC)
                // ==============================================================
                PoiEntity(
                    osmId = 10032L,
                    name = "Royal Enfield Service & Spares (Siliguri)",
                    category = PoiCategory.MECHANIC.name,
                    latitude = 26.7271,
                    longitude = 88.3953,
                    brand = "Royal Enfield",
                    address = "Sevoke Road, Siliguri",
                    openingHours = "09:30 - 19:00",
                    phone = "+91 353 254 3210",
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
                    openingHours = "08:00 - 20:00",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10053L,
                    name = "Kolkata Central Bike Doctor & Tyre Hub",
                    category = PoiCategory.MECHANIC.name,
                    latitude = 22.5640,
                    longitude = 88.3510,
                    brand = "Multibrand 2-Wheeler Garage",
                    address = "Lenin Sarani, Chandni Chowk, Kolkata",
                    openingHours = "09:00 - 20:30",
                    phone = "+91 98315 44321",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10054L,
                    name = "Durgapur GT Road Two Wheeler Service",
                    category = PoiCategory.MECHANIC.name,
                    latitude = 23.5180,
                    longitude = 87.3220,
                    brand = "Bajaj / KTM Authorized",
                    address = "Nachan Road, Benachity, Durgapur",
                    openingHours = "09:30 - 19:30",
                    phone = "+91 343 258 7766",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10101L,
                    name = "Royal Enfield Company Service (VIP Road)",
                    category = PoiCategory.MECHANIC.name,
                    latitude = 22.5980,
                    longitude = 88.4280,
                    brand = "Royal Enfield Official",
                    address = "VIP Road, Teghoria, Kolkata 700157",
                    openingHours = "09:00 - 19:00",
                    phone = "+91 33 2570 9988",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10102L,
                    name = "KTM & Husqvarna Service (Chinar Park)",
                    category = PoiCategory.MECHANIC.name,
                    latitude = 22.6280,
                    longitude = 88.4410,
                    brand = "KTM / Husqvarna",
                    address = "Chinar Park, Rajarhat Main Rd, Kolkata",
                    openingHours = "09:30 - 19:30",
                    phone = "+91 33 4004 5566",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10103L,
                    name = "Triumph Motorcycles Service (AJC Bose Rd)",
                    category = PoiCategory.MECHANIC.name,
                    latitude = 22.5410,
                    longitude = 88.3580,
                    brand = "Triumph Official",
                    address = "AJC Bose Road, Elgin, Kolkata",
                    openingHours = "10:00 - 18:30",
                    phone = "+91 33 2289 1100",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 11. FUEL STATIONS & HIGHWAY PUMPS (PETROL)
                // ==============================================================
                PoiEntity(
                    osmId = 10029L,
                    name = "Indian Oil Petrol Pump (NH19 Dankuni)",
                    category = PoiCategory.PETROL.name,
                    latitude = 22.6845,
                    longitude = 88.2912,
                    brand = "Indian Oil COCO",
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
                    brand = "Hindustan Petroleum",
                    address = "Jessore Road, Barasat",
                    openingHours = "24/7",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10104L,
                    name = "Indian Oil Swagat COCO (NH16 Kolaghat)",
                    category = PoiCategory.PETROL.name,
                    latitude = 22.4285,
                    longitude = 87.8718,
                    brand = "Indian Oil Swagat",
                    address = "NH 16, Kolaghat Bypass, Purba Medinipur",
                    openingHours = "24/7",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10105L,
                    name = "Shell Petrol Pump (New Town AA-1)",
                    category = PoiCategory.PETROL.name,
                    latitude = 22.5870,
                    longitude = 88.4620,
                    brand = "Shell",
                    address = "Action Area 1, Major Arterial Road, New Town",
                    openingHours = "24/7",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 12. HOSPITALS & EMERGENCY (HOSPITAL)
                // ==============================================================
                PoiEntity(
                    osmId = 10051L,
                    name = "Apollo Gleneagles Hospital (EM Bypass)",
                    category = PoiCategory.HOSPITAL.name,
                    latitude = 22.5690,
                    longitude = 88.4040,
                    brand = "Apollo Hospitals",
                    address = "58 Canal Circular Rd, Kadapara, Kolkata 700054",
                    openingHours = "24/7",
                    phone = "+91 33 2320 3040",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10052L,
                    name = "Fortis Hospital (Anandapur EM Bypass)",
                    category = PoiCategory.HOSPITAL.name,
                    latitude = 22.5180,
                    longitude = 88.4010,
                    brand = "Fortis Healthcare",
                    address = "730 Anandapur, EM Bypass, Kolkata 700107",
                    openingHours = "24/7",
                    phone = "+91 33 6628 4444",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10106L,
                    name = "SSKM Hospital & IPGMER",
                    category = PoiCategory.HOSPITAL.name,
                    latitude = 22.5390,
                    longitude = 88.3440,
                    brand = "Government Apex Hospital",
                    address = "244 AJC Bose Road, Bhowanipore, Kolkata",
                    openingHours = "24/7 Emergency",
                    phone = "+91 33 2223 1589",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10107L,
                    name = "Tata Medical Center (New Town)",
                    category = PoiCategory.HOSPITAL.name,
                    latitude = 22.5930,
                    longitude = 88.4890,
                    brand = "Specialty Hospital",
                    address = "14 MAR, Action Area II, New Town, Kolkata",
                    openingHours = "24/7",
                    phone = "+91 33 6605 7000",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10108L,
                    name = "North Bengal Medical College (Siliguri)",
                    category = PoiCategory.HOSPITAL.name,
                    latitude = 26.6980,
                    longitude = 88.3750,
                    brand = "Apex Hospital North Bengal",
                    address = "Sushrutanagar, Siliguri, Darjeeling",
                    openingHours = "24/7 Emergency",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 13. PHARMACIES & CHEMISTS (PHARMACY)
                // ==============================================================
                PoiEntity(
                    osmId = 10047L,
                    name = "Apollo Pharmacy 24/7 (Park Street)",
                    category = PoiCategory.PHARMACY.name,
                    latitude = 22.5535,
                    longitude = 88.3540,
                    brand = "Apollo Pharmacy",
                    address = "Park Street, Kolkata 700016",
                    openingHours = "24/7",
                    phone = "+91 33 2229 9900",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10048L,
                    name = "Medplus Pharmacy (Salt Lake Sector 1)",
                    category = PoiCategory.PHARMACY.name,
                    latitude = 22.5895,
                    longitude = 88.4090,
                    brand = "Medplus",
                    address = "Block BD, Salt Lake, Bidhannagar",
                    openingHours = "07:00 - 23:00",
                    phone = "+91 33 2334 1122",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10109L,
                    name = "Frank Ross Pharmacy (Gariahat)",
                    category = PoiCategory.PHARMACY.name,
                    latitude = 22.5185,
                    longitude = 88.3650,
                    brand = "Frank Ross",
                    address = "Rashbehari Avenue, Gariahat, Kolkata",
                    openingHours = "24/7",
                    phone = "+91 33 2464 1234",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 14. ATMS & BANKING HUBS (ATM)
                // ==============================================================
                PoiEntity(
                    osmId = 10110L,
                    name = "SBI 24/7 E-Corner & ATM (Park Street)",
                    category = PoiCategory.ATM.name,
                    latitude = 22.5530,
                    longitude = 88.3536,
                    brand = "State Bank of India",
                    address = "Park Street, Kolkata",
                    openingHours = "24/7",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10111L,
                    name = "HDFC Bank ATM (EM Bypass Ruby)",
                    category = PoiCategory.ATM.name,
                    latitude = 22.5140,
                    longitude = 88.4020,
                    brand = "HDFC Bank",
                    address = "EM Bypass, Kasba, Kolkata",
                    openingHours = "24/7",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 15. HIGHWAY TOLL PLAZAS & POLICE (TOLL / POLICE)
                // ==============================================================
                PoiEntity(
                    osmId = 10112L,
                    name = "Dankuni Toll Plaza (NH 19)",
                    category = PoiCategory.TOLL.name,
                    latitude = 22.6840,
                    longitude = 88.2905,
                    brand = "NHAI FASTag Toll",
                    address = "NH 19, Dankuni, Hooghly",
                    openingHours = "24/7",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10113L,
                    name = "Palsit Toll Plaza (NH 19)",
                    category = PoiCategory.TOLL.name,
                    latitude = 23.1840,
                    longitude = 88.0420,
                    brand = "NHAI FASTag Toll",
                    address = "NH 19 Durgapur Expressway, Palsit",
                    openingHours = "24/7",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10114L,
                    name = "Kolkata Police Lalbazar HQ",
                    category = PoiCategory.POLICE.name,
                    latitude = 22.5710,
                    longitude = 88.3510,
                    brand = "Police Headquarters",
                    address = "18 Lalbazar Street, BBD Bagh, Kolkata",
                    openingHours = "24/7",
                    phone = "100",
                    regionId = "west_bengal"
                ),

                // ==============================================================
                // 16. SHOPPING, STORES, MALLS & DECATHLON (STORE)
                // ==============================================================
                PoiEntity(
                    osmId = 10056L,
                    name = "Decathlon Salt Lake (Sector V)",
                    category = PoiCategory.STORE.name,
                    latitude = 22.5833,
                    longitude = 88.4312,
                    brand = "Decathlon Sports",
                    address = "Block GP, Sector V, Bidhannagar, Kolkata 700091",
                    openingHours = "10:00 - 21:30",
                    phone = "+91 80 6902 4444",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10057L,
                    name = "Decathlon Newtown",
                    category = PoiCategory.STORE.name,
                    latitude = 22.5958,
                    longitude = 88.4712,
                    brand = "Decathlon Sports",
                    address = "Major Arterial Road, Action Area II, New Town, Kolkata 700156",
                    openingHours = "10:00 - 22:00",
                    phone = "+91 80 6902 4444",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10058L,
                    name = "Decathlon Shakespeare Sarani",
                    category = PoiCategory.STORE.name,
                    latitude = 22.5447,
                    longitude = 88.3582,
                    brand = "Decathlon Sports",
                    address = "Astha City Centre, Shakespeare Sarani, Kolkata 700017",
                    openingHours = "10:00 - 21:00",
                    phone = "+91 80 6902 4444",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10059L,
                    name = "Decathlon Howrah (Kona Expressway)",
                    category = PoiCategory.STORE.name,
                    latitude = 22.5850,
                    longitude = 88.2910,
                    brand = "Decathlon Sports",
                    address = "NH 16, Kona Expressway Junction, Howrah 711113",
                    openingHours = "10:00 - 21:30",
                    phone = "+91 80 6902 4444",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10060L,
                    name = "Decathlon Siliguri (Vega Circle Mall)",
                    category = PoiCategory.STORE.name,
                    latitude = 26.7450,
                    longitude = 88.4360,
                    brand = "Decathlon Sports",
                    address = "Vega Circle Mall, 3rd Mile, Sevoke Road, Siliguri 734008",
                    openingHours = "10:00 - 21:00",
                    phone = "+91 80 6902 4444",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10061L,
                    name = "South City Mall & Hyperstore",
                    category = PoiCategory.STORE.name,
                    latitude = 22.5015,
                    longitude = 88.3618,
                    brand = "Shopping Mall",
                    address = "375 Prince Anwar Shah Road, South City, Kolkata 700068",
                    openingHours = "10:00 - 22:00",
                    phone = "+91 33 4007 2181",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10062L,
                    name = "City Centre 1 (Salt Lake)",
                    category = PoiCategory.STORE.name,
                    latitude = 22.5898,
                    longitude = 88.4087,
                    brand = "Shopping Mall",
                    address = "DC Block, Sector 1, Bidhannagar, Kolkata 700064",
                    openingHours = "10:30 - 21:30",
                    phone = "+91 33 2358 1010",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10035L,
                    name = "Spencer's Hyper Store (South City)",
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
                    osmId = 10115L,
                    name = "Quest Mall (Ballygunge)",
                    category = PoiCategory.STORE.name,
                    latitude = 22.5390,
                    longitude = 88.3650,
                    brand = "Luxury Shopping Mall",
                    address = "33 Syed Amir Ali Ave, Park Circus, Kolkata",
                    openingHours = "11:00 - 22:00",
                    phone = "+91 33 3984 5555",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10116L,
                    name = "Acropolis Mall (Kasba)",
                    category = PoiCategory.STORE.name,
                    latitude = 22.5160,
                    longitude = 88.3930,
                    brand = "Shopping Mall",
                    address = "1858 Rajdanga Main Road, Kasba, Kolkata",
                    openingHours = "11:00 - 22:00",
                    phone = "+91 33 4010 7000",
                    regionId = "west_bengal"
                ),
                PoiEntity(
                    osmId = 10117L,
                    name = "Mani Square Mall (EM Bypass)",
                    category = PoiCategory.STORE.name,
                    latitude = 22.5740,
                    longitude = 88.4020,
                    brand = "Shopping Mall",
                    address = "164/1 Maniktala Main Rd, EM Bypass, Kolkata",
                    openingHours = "11:00 - 22:00",
                    phone = "+91 33 2320 1888",
                    regionId = "west_bengal"
                )
            )
        }
    }
}
