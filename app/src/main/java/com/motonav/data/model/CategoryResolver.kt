package com.motonav.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.motonav.data.local.db.entities.PoiCategory
import com.motonav.ui.theme.MotoNavIcons

data class CategoryInfo(
    val category: String,
    val title: String,
    val emoji: String,
    val color: Color
)

/**
 * Intelligent POI Category Resolver
 * Categorizes vector features, Mapbox places, OpenStreetMap tags, and tapped map points
 * into accurate, granular motorcycle cockpit categories.
 */
object CategoryResolver {

    fun resolve(
        name: String,
        rawClass: String = "",
        subclass: String = "",
        category: String = "",
        type: String = "",
        maki: String = "",
        amenity: String = "",
        shop: String = "",
        tourism: String = "",
        historic: String = "",
        leisure: String = ""
    ): String {
        val n = name.lowercase().trim()
        val c = rawClass.lowercase().trim()
        val sub = subclass.lowercase().trim()
        val cat = category.lowercase().trim()
        val t = type.lowercase().trim()
        val m = maki.lowercase().trim()
        val a = amenity.lowercase().trim()
        val s = shop.lowercase().trim()
        val tour = tourism.lowercase().trim()
        val hist = historic.lowercase().trim()
        val leis = leisure.lowercase().trim()

        val all = "$n $c $sub $cat $t $m $a $s $tour $hist $leis"

        return when {
            // 1. Fuel / Petrol / Gas Station
            a == "fuel" || m == "fuel" || c.contains("fuel") || c.contains("gas") ||
            all.contains("petrol") || all.contains("diesel") || all.contains("cng") ||
            all.contains("iocl") || all.contains("indian oil") || all.contains("bharat petroleum") ||
            all.contains("bpcl") || all.contains("hpcl") || all.contains("hindustan petroleum") ||
            all.contains("shell") || all.contains("nayara") || all.contains("reliance petroleum") ||
            all.contains("essar") -> PoiCategory.PETROL.name

            // 2. Highway Dhaba & Local Roadside Food
            all.contains("dhaba") || all.contains("sher-e-punjab") || all.contains("tadka") ||
            all.contains("line hotel") -> PoiCategory.DHABA.name

            // 3. Cafe, Bakery & Coffee Bar
            a == "cafe" || m == "cafe" || s == "bakery" || s == "coffee" || s == "tea" ||
            c == "cafe" || sub == "cafe" ||
            all.contains("cafe") || all.contains("coffee") || all.contains("bakery") ||
            all.contains("tea post") || all.contains("chai") || all.contains("chaayos") ||
            all.contains("starbucks") || all.contains("barista") || all.contains("cafe coffee day") ||
            all.contains("ccd") || all.contains("blue tokai") || all.contains("flurys") -> PoiCategory.CAFE.name

            // 4. Restaurant & Dining
            a in listOf("restaurant", "fast_food", "food_court") || m in listOf("restaurant", "fast-food") ||
            c in listOf("restaurant", "food") ||
            all.contains("restaurant") || all.contains("dining") || all.contains("biryani") ||
            all.contains("bhojanalaya") || all.contains("kitchen") || all.contains("fast food") ||
            all.contains("peter cat") || all.contains("arsalan") || all.contains("aminia") ||
            all.contains("oudh 1590") || all.contains("bhojohori manna") || all.contains("kasturi") ->
                PoiCategory.RESTAURANT.name

            // 5. Hotel, Resort & Lodging
            tour in listOf("hotel", "motel", "guest_house", "hostel", "resort") ||
            m in listOf("lodging", "hotel") || c in listOf("hotel", "lodging") ||
            all.contains("hotel") || all.contains("resort") || all.contains("lodge") ||
            all.contains("lodging") || all.contains("guest house") || all.contains("homestay") ||
            all.contains("inn") || all.contains("hostel") || all.contains("taj bengal") ||
            all.contains("oberoi") || all.contains("itc sonar") || all.contains("itc royal") ||
            all.contains("marriott") || all.contains("westin") || all.contains("novotel") ||
            all.contains("oyo") -> PoiCategory.HOTEL.name

            // 6. Motorcycle & Auto Workshop / Mechanic
            s in listOf("motorcycle", "car_repair", "tyres", "car_parts") ||
            all.contains("mechanic") || all.contains("garage") || all.contains("workshop") ||
            all.contains("bike repair") || all.contains("puncture") || all.contains("tyre") ||
            all.contains("tires") || all.contains("service center") || all.contains("royal enfield") ||
            all.contains("yamaha") || all.contains("ktm") || all.contains("bajaj") ||
            all.contains("hero moto") || all.contains("tvs") || all.contains("honda service") ||
            all.contains("auto repair") -> PoiCategory.MECHANIC.name

            // 7. Pharmacy & Chemist
            a == "pharmacy" || s == "chemist" || m == "pharmacy" ||
            all.contains("pharmacy") || all.contains("chemist") || all.contains("medicine") ||
            all.contains("druggist") || all.contains("apollo pharmacy") || all.contains("medplus") ||
            all.contains("frank ross") || all.contains("dawa") -> PoiCategory.PHARMACY.name

            // 8. Hospital & Emergency Medical
            a in listOf("hospital", "clinic", "doctors") || m in listOf("hospital", "doctor") ||
            c == "hospital" ||
            all.contains("hospital") || all.contains("clinic") || all.contains("nursing home") ||
            all.contains("medical college") || all.contains("apollo gleneagles") ||
            all.contains("fortis") || all.contains("medica") || all.contains("amri") ||
            all.contains("ruby hospital") || all.contains("sskm") || all.contains("tata medical") ->
                PoiCategory.HOSPITAL.name

            // 9. ATM & Banking
            a in listOf("atm", "bank") || m in listOf("bank", "atm") ||
            all.contains("atm") || all.contains("bank") || all.contains("sbi") ||
            all.contains("hdfc") || all.contains("icici") || all.contains("axis bank") ||
            all.contains("punjab national") -> PoiCategory.ATM.name

            // 10. Viewpoint, Peaks & Scenic Passes
            tour == "viewpoint" || m == "viewpoint" || c == "viewpoint" ||
            all.contains("viewpoint") || all.contains("view point") || all.contains("ghat") ||
            all.contains("peak") || all.contains("cliff") || all.contains("sunrise point") ||
            all.contains("sunset point") || all.contains("tiger hill") || all.contains("batasia loop") ||
            all.contains("dam") || all.contains("falls") || all.contains("waterfall") ||
            all.contains("ayodhya hills") || all.contains("susunia") || all.contains("biharinath") ||
            all.contains("mukutmanipur") || all.contains("maithon") || all.contains("panchet") ||
            all.contains("massanjore") || all.contains("sandakphu") || all.contains("panoramic") ->
                PoiCategory.VIEWPOINT.name

            // 11. Monument & Heritage Attraction (Bridges, Memorials, Forts, Museums)
            tour in listOf("attraction", "museum") || hist.isNotBlank() || m in listOf("monument", "museum") ||
            all.contains("monument") || all.contains("museum") || all.contains("heritage") ||
            all.contains("palace") || all.contains("fort") || all.contains("statue") ||
            all.contains("memorial") || all.contains("victoria memorial") || all.contains("howrah bridge") ||
            all.contains("vidyasagar setu") || all.contains("bridge") || all.contains("setu") ||
            all.contains("ghat") || all.contains("rajbari") || all.contains("clock tower") ||
            all.contains("planetarium") || all.contains("stadium") || all.contains("science city") ||
            all.contains("princep ghat") || all.contains("babu ghat") || all.contains("millennium park") ||
            all.contains("eden gardens") || all.contains("salt lake stadium") -> PoiCategory.ATTRACTION.name

            // 12. Park, Nature, Lakes & Beaches
            leis in listOf("park", "garden", "nature_reserve") || m in listOf("park", "garden") ||
            all.contains("park") || all.contains("garden") || all.contains("lake") ||
            all.contains("beach") || all.contains("sanctuary") || all.contains("forest") ||
            all.contains("waterfall") || all.contains("falls") || all.contains("eco park") ||
            all.contains("nicco park") || all.contains("alipore zoo") || all.contains("digha") ||
            all.contains("mandarmani") || all.contains("tajpur") || all.contains("bakkhali") ||
            all.contains("mirik") || all.contains("gorumara") || all.contains("jaldapara") ->
                PoiCategory.PARK.name

            // 13. Temple & Place of Worship (Mandirs, Mosques, Churches, Monasteries, Gurdwaras)
            a == "place_of_worship" || m in listOf("place-of-worship", "religious-christian", "religious-jewish", "religious-muslim") ||
            all.contains("temple") || all.contains("mandir") || all.contains("masjid") ||
            all.contains("mosque") || all.contains("church") || all.contains("cathedral") ||
            all.contains("gurudwara") || all.contains("dakshineswar") || all.contains("kalighat") ||
            all.contains("belur math") || all.contains("ashram") || all.contains("monastery") ||
            all.contains("pagoda") || all.contains("stupa") || all.contains("shrine") ||
            all.contains("dargah") || all.contains("iskcon") || all.contains("kali bari") ||
            all.contains("tarapith") || all.contains("mayapur") || all.contains("kapil muni") ||
            all.contains("hangseshwari") || all.contains("tarakeswar") || all.contains("bakreshwar") ||
            all.contains("furfura sharif") || all.contains("jalpesh") || all.contains("basilica") ->
                PoiCategory.TEMPLE.name

            // 14. Police & Highway Help
            a == "police" || m == "police" ||
            all.contains("police") || all.contains("thana") || all.contains("highway patrol") ||
            all.contains("outpost") -> PoiCategory.POLICE.name

            // 15. Toll Plaza
            all.contains("toll") || all.contains("toll plaza") || all.contains("fastag") ||
            all.contains("check post") -> PoiCategory.TOLL.name

            // 16. Local Shops, Bazaars, Sweets & Malls (Real retail & markets)
            s.isNotBlank() || m in listOf("grocery", "clothing-store", "shop", "convenience") ||
            c in listOf("shop", "grocery", "mall") ||
            all.contains("decathlon") || all.contains("supermarket") || all.contains("hypermarket") ||
            all.contains("grocery") || all.contains("mall") || all.contains("spencer") ||
            all.contains("smart bazaar") || all.contains("reliance smart") || all.contains("dmart") ||
            all.contains("departmental store") || all.contains("convenience") || all.contains("bazaar") ||
            all.contains("market") || all.contains("shopping") || all.contains("retail") ||
            all.contains("store") || all.contains("shop") || all.contains("sweet") ||
            all.contains("sweets") || all.contains("sandesh") || all.contains("mithai") ||
            all.contains("haat") || all.contains("bazar") || all.contains("jewellers") ||
            all.contains("jewellery") || all.contains("cloth") || all.contains("textile") ||
            all.contains("handicraft") || all.contains("book store") || all.contains("bookseller") ||
            all.contains("stationery") || all.contains("sports") || all.contains("kirana") ||
            all.contains("new market") || all.contains("college street") || all.contains("gariahat") ||
            all.contains("burrabazar") || all.contains("hatibagan") || all.contains("quest mall") ||
            all.contains("south city") || all.contains("city centre") || all.contains("acropolis") ||
            all.contains("mani square") || all.contains("flurys") || all.contains("nahoum") ||
            all.contains("kc das") || all.contains("bhim chandra nag") || all.contains("balaram mullick") ||
            all.contains("nakur") || all.contains("ganguram") -> PoiCategory.STORE.name

            // 17. Street Names, Avenues, Expressways, City Hubs & Crossroads
            c == "place" || t == "place" ||
            all.contains("city") || all.contains("town") || all.contains("village") ||
            all.contains("district") || all.contains("municipality") || all.contains("locality") ||
            all.contains("street") || all.contains("road") || all.contains("sarani") ||
            all.contains("avenue") || all.contains("expressway") || all.contains("highway") ||
            all.contains("bypass") || all.contains("flyover") || all.contains("connector") ||
            all.contains("corridor") || all.contains("lane") || all.contains("chowk") ||
            all.contains("crossing") || all.contains("more") || all.contains("park street") ||
            all.contains("em bypass") || all.contains("vip road") || all.contains("gt road") ||
            all.contains("grand trunk") -> PoiCategory.CITY.name

            // 18. General map location fallback (NOT a store!)
            else -> PoiCategory.LOCATION.name
        }
    }

    fun getCategoryInfo(category: String): CategoryInfo {
        return when (category.uppercase().trim()) {
            "PETROL" -> CategoryInfo("PETROL", "24/7 Fuel Station", "⛽", Color(0xFFF59E0B))
            "DHABA" -> CategoryInfo("DHABA", "Highway Dhaba & Food", "🍛", Color(0xFFF97316))
            "RESTAURANT" -> CategoryInfo("RESTAURANT", "Restaurant & Dining", "🍽️", Color(0xFFFB923C))
            "CAFE" -> CategoryInfo("CAFE", "Cafe & Bakery", "☕", Color(0xFFD97706))
            "HOTEL" -> CategoryInfo("HOTEL", "Hotel & Lodging", "🏨", Color(0xFF6366F1))
            "MECHANIC" -> CategoryInfo("MECHANIC", "Motorcycle Workshop", "🔧", Color(0xFF38BDF8))
            "HOSPITAL" -> CategoryInfo("HOSPITAL", "Hospital & Emergency", "🏥", Color(0xFFEF4444))
            "PHARMACY" -> CategoryInfo("PHARMACY", "Pharmacy & Chemist", "💊", Color(0xFFF43F5E))
            "ATM" -> CategoryInfo("ATM", "ATM & Banking", "💳", Color(0xFF06B6D4))
            "VIEWPOINT" -> CategoryInfo("VIEWPOINT", "Scenic Viewpoint", "🏔️", Color(0xFF8B5CF6))
            "ATTRACTION" -> CategoryInfo("ATTRACTION", "Monument & Heritage", "🏛️", Color(0xFFA855F7))
            "PARK" -> CategoryInfo("PARK", "Park & Nature", "🌲", Color(0xFF22C55E))
            "TEMPLE" -> CategoryInfo("TEMPLE", "Place of Worship", "🛕", Color(0xFFEAB308))
            "POLICE" -> CategoryInfo("POLICE", "Police & Highway Help", "👮", Color(0xFF3B82F6))
            "TOLL" -> CategoryInfo("TOLL", "Toll Plaza", "🛂", Color(0xFF64748B))
            "STORE" -> CategoryInfo("STORE", "Store & Shopping", "🛒", Color(0xFF10B981))
            "CITY" -> CategoryInfo("CITY", "City & Town Hub", "🏙️", Color(0xFF0284C7))
            else -> CategoryInfo("LOCATION", "Location on Map", "📍", Color(0xFF94A3B8))
        }
    }

    fun getCategoryIcon(category: String): ImageVector {
        return when (category.uppercase().trim()) {
            "PETROL" -> MotoNavIcons.Fuel
            "DHABA", "RESTAURANT" -> MotoNavIcons.Dhaba
            "CAFE" -> MotoNavIcons.Cafe
            "HOTEL" -> MotoNavIcons.Hotel
            "MECHANIC" -> MotoNavIcons.Mechanic
            "HOSPITAL", "PHARMACY" -> MotoNavIcons.Hospital
            "ATM" -> MotoNavIcons.Atm
            "VIEWPOINT" -> MotoNavIcons.Viewpoint
            "ATTRACTION" -> MotoNavIcons.Landmark
            "PARK" -> MotoNavIcons.Park
            "TEMPLE" -> MotoNavIcons.Temple
            "CITY" -> MotoNavIcons.City
            "TOLL" -> MotoNavIcons.Toll
            "STORE" -> MotoNavIcons.Store
            else -> MotoNavIcons.LocationPin
        }
    }
}
