package com.motonav

import com.motonav.data.local.MapFileManager
import com.motonav.data.local.OfflineStateNotifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineRegionDetectionTest {

    @Test
    fun testRegionDetectionForMajorIndianCities() {
        // Kolkata -> West Bengal
        val kolkataRegion = MapFileManager.AVAILABLE_REGIONS.find { r ->
            22.5726 in r.minLat..r.maxLat && 88.3639 in r.minLng..r.maxLng
        }
        assertNotNull(kolkataRegion)
        assertEquals("west_bengal", kolkataRegion?.id)

        // Bengaluru -> Karnataka
        val bengaluruRegion = MapFileManager.AVAILABLE_REGIONS.find { r ->
            12.9716 in r.minLat..r.maxLat && 77.5946 in r.minLng..r.maxLng
        }
        assertNotNull(bengaluruRegion)
        assertEquals("karnataka", bengaluruRegion?.id)

        // Mumbai -> Maharashtra
        val mumbaiRegion = MapFileManager.AVAILABLE_REGIONS.find { r ->
            19.0760 in r.minLat..r.maxLat && 72.8777 in r.minLng..r.maxLng
        }
        assertNotNull(mumbaiRegion)
        assertEquals("maharashtra", mumbaiRegion?.id)

        // Delhi -> Delhi NCR
        val delhiRegion = MapFileManager.AVAILABLE_REGIONS.find { r ->
            28.6139 in r.minLat..r.maxLat && 77.2090 in r.minLng..r.maxLng
        }
        assertNotNull(delhiRegion)
        assertEquals("delhi_ncr", delhiRegion?.id)

        // Manali -> Himachal Pradesh
        val manaliRegion = MapFileManager.AVAILABLE_REGIONS.find { r ->
            32.2432 in r.minLat..r.maxLat && 77.1892 in r.minLng..r.maxLng
        }
        assertNotNull(manaliRegion)
        assertEquals("himachal_pradesh", manaliRegion?.id)
    }

    @Test
    fun testNotificationAutoDismissTimingConstant() {
        // Notification must auto-dismiss after exactly 5 seconds
        assertEquals(5000L, OfflineStateNotifier.AUTO_DISMISS_DURATION_MS)
    }

    @Test
    fun testLightweightInitialRegionStatus() {
        // All regions must ship as NOT_DOWNLOADED by default so APK stays lightweight
        for (region in MapFileManager.AVAILABLE_REGIONS) {
            assertTrue(
                "Region ${region.displayName} should not be prepackaged",
                !region.isPrepackaged
            )
        }
    }

    @Test
    fun testCategoryResolutionForDiversePois() {
        // Temples & Places of Worship
        assertEquals(
            com.motonav.data.local.db.entities.PoiCategory.TEMPLE.name,
            com.motonav.data.model.CategoryResolver.resolve("Dakshineswar Kali Temple")
        )
        assertEquals(
            com.motonav.data.local.db.entities.PoiCategory.TEMPLE.name,
            com.motonav.data.model.CategoryResolver.resolve("Belur Math")
        )

        // Local Shops, Bazaars & Sweets
        assertEquals(
            com.motonav.data.local.db.entities.PoiCategory.STORE.name,
            com.motonav.data.model.CategoryResolver.resolve("New Market")
        )
        assertEquals(
            com.motonav.data.local.db.entities.PoiCategory.STORE.name,
            com.motonav.data.model.CategoryResolver.resolve("Bhim Chandra Nag Sweets")
        )

        // Popular Landmarks & Attractions
        assertEquals(
            com.motonav.data.local.db.entities.PoiCategory.ATTRACTION.name,
            com.motonav.data.model.CategoryResolver.resolve("Victoria Memorial")
        )
        assertEquals(
            com.motonav.data.local.db.entities.PoiCategory.ATTRACTION.name,
            com.motonav.data.model.CategoryResolver.resolve("Howrah Bridge")
        )

        // Viewpoints
        assertEquals(
            com.motonav.data.local.db.entities.PoiCategory.VIEWPOINT.name,
            com.motonav.data.model.CategoryResolver.resolve("Tiger Hill Sunrise Point")
        )

        // Streets & Avenues
        assertEquals(
            com.motonav.data.local.db.entities.PoiCategory.CITY.name,
            com.motonav.data.model.CategoryResolver.resolve("Park Street")
        )
        assertEquals(
            com.motonav.data.local.db.entities.PoiCategory.CITY.name,
            com.motonav.data.model.CategoryResolver.resolve("EM Bypass Expressway")
        )
    }
}
