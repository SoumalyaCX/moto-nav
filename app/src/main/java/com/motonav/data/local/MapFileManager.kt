package com.motonav.data.local

import android.content.Context
import android.util.Log
import com.motonav.data.model.MapRegion
import com.motonav.data.model.RegionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapFileManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "MapFileManager"
        const val MAPS_DIR_NAME = "maps"
        const val PREPACKAGED_REGION_ID = "west_bengal"

        val AVAILABLE_REGIONS = listOf(
            MapRegion(
                id = "west_bengal",
                displayName = "West Bengal (Kolkata / Siliguri)",
                stateCode = "WB",
                approxSizeMb = 145,
                minLat = 21.5,
                minLng = 85.8,
                maxLat = 27.2,
                maxLng = 89.9,
                isPrepackaged = false,
                status = RegionStatus.NOT_DOWNLOADED
            ),
            MapRegion(
                id = "karnataka",
                displayName = "Karnataka (Bengaluru / Western Ghats)",
                stateCode = "KA",
                approxSizeMb = 210,
                minLat = 11.5,
                minLng = 74.0,
                maxLat = 18.5,
                maxLng = 78.6,
                isPrepackaged = false
            ),
            MapRegion(
                id = "maharashtra",
                displayName = "Maharashtra (Mumbai / Pune / Sahyadris)",
                stateCode = "MH",
                approxSizeMb = 260,
                minLat = 15.6,
                minLng = 72.6,
                maxLat = 22.0,
                maxLng = 80.9,
                isPrepackaged = false
            ),
            MapRegion(
                id = "delhi_ncr",
                displayName = "Delhi NCR & Haryana",
                stateCode = "DL",
                approxSizeMb = 120,
                minLat = 27.6,
                minLng = 76.5,
                maxLat = 29.5,
                maxLng = 77.8,
                isPrepackaged = false
            ),
            MapRegion(
                id = "himachal_pradesh",
                displayName = "Himachal Pradesh (Manali / Spiti Circuit)",
                stateCode = "HP",
                approxSizeMb = 95,
                minLat = 30.3,
                minLng = 75.7,
                maxLat = 33.3,
                maxLng = 79.0,
                isPrepackaged = false
            ),
            MapRegion(
                id = "kerala",
                displayName = "Kerala (Munnar / Wayanad / Coast)",
                stateCode = "KL",
                approxSizeMb = 130,
                minLat = 8.3,
                minLng = 74.8,
                maxLat = 12.8,
                maxLng = 77.4,
                isPrepackaged = false
            ),
            MapRegion(
                id = "tamil_nadu",
                displayName = "Tamil Nadu (Nilgiris / Chennai)",
                stateCode = "TN",
                approxSizeMb = 190,
                minLat = 8.0,
                minLng = 76.2,
                maxLat = 13.5,
                maxLng = 80.4,
                isPrepackaged = false
            ),
            MapRegion(
                id = "rajasthan",
                displayName = "Rajasthan (Jaipur / Thar Desert)",
                stateCode = "RJ",
                approxSizeMb = 230,
                minLat = 23.0,
                minLng = 69.5,
                maxLat = 30.2,
                maxLng = 78.3,
                isPrepackaged = false
            )
        )
    }

    val mapsDirectory: File by lazy {
        File(context.filesDir, MAPS_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Initializes pre-packaged region (West Bengal) from assets into internal storage.
     */
    suspend fun ensurePrepackagedRegionExtracted(): Result<File> = withContext(Dispatchers.IO) {
        try {
            val wbPmtiles = File(mapsDirectory, "${PREPACKAGED_REGION_ID}.pmtiles")
            if (!wbPmtiles.exists()) {
                val assetPath = "maps/${PREPACKAGED_REGION_ID}.pmtiles"
                val hasAsset = try {
                    context.assets.open(assetPath).close()
                    true
                } catch (e: Exception) {
                    false
                }

                if (hasAsset) {
                    copyAssetToFile(assetPath, wbPmtiles)
                }
            }

            // Extract routing graph if bundled in assets
            val graphDir = File(mapsDirectory, "${PREPACKAGED_REGION_ID}-gh")
            if (!graphDir.exists()) {
                val graphAssetPath = "maps/${PREPACKAGED_REGION_ID}.ghz"
                try {
                    context.assets.open(graphAssetPath).use { input ->
                        unzipStreamToDirectory(input, graphDir)
                    }
                } catch (e: Exception) {
                    graphDir.mkdirs()
                }
            }

            Result.success(wbPmtiles)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing prepackaged map: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun copyAssetToFile(assetPath: String, destination: File) = withContext(Dispatchers.IO) {
        context.assets.open(assetPath).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }

    suspend fun unzipArchive(archiveFile: File, targetDir: File) = withContext(Dispatchers.IO) {
        archiveFile.inputStream().use { input ->
            unzipStreamToDirectory(input, targetDir)
        }
    }

    private fun unzipStreamToDirectory(inputStream: InputStream, targetDir: File) {
        if (!targetDir.exists()) targetDir.mkdirs()
        ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
            var entry = zipIn.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                // Prevent Zip Slip vulnerability
                if (!file.canonicalPath.startsWith(targetDir.canonicalPath)) {
                    throw SecurityException("Zip traversal detected: ${entry.name}")
                }
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { out ->
                        zipIn.copyTo(out)
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
        }
    }

    fun getInstalledRegions(): List<MapRegion> {
        return AVAILABLE_REGIONS.map { region ->
            val pmtiles = region.getLocalPmtilesFile(mapsDirectory)
            if (pmtiles.exists()) {
                region.copy(status = RegionStatus.DOWNLOADED, downloadProgressPercent = 100)
            } else if (region.isPrepackaged) {
                region.copy(status = RegionStatus.PRE_INSTALLED, downloadProgressPercent = 100)
            } else {
                region.copy(status = RegionStatus.NOT_DOWNLOADED, downloadProgressPercent = 0)
            }
        }
    }

    /**
     * Checks if a given region's offline data exists in local storage.
     */
    fun isRegionDownloaded(regionId: String): Boolean {
        val region = AVAILABLE_REGIONS.find { it.id == regionId } ?: return false
        val pmtiles = region.getLocalPmtilesFile(mapsDirectory)
        return pmtiles.exists()
    }

    /**
     * Finds the matching or closest region for a given latitude and longitude.
     */
    fun findRegionForLocation(lat: Double, lng: Double): MapRegion? {
        val direct = AVAILABLE_REGIONS.find { region ->
            lat in region.minLat..region.maxLat && lng in region.minLng..region.maxLng
        }
        if (direct != null) return direct

        // If outside exact bounding boxes, find nearest Indian state center
        return AVAILABLE_REGIONS.minByOrNull { region ->
            val centerLat = (region.minLat + region.maxLat) / 2.0
            val centerLng = (region.minLng + region.maxLng) / 2.0
            val dLat = lat - centerLat
            val dLng = lng - centerLng
            dLat * dLat + dLng * dLng
        }
    }

    /**
     * Marks a region as locally downloaded by ensuring the offline file exists.
     */
    suspend fun markRegionDownloaded(region: MapRegion) = withContext(Dispatchers.IO) {
        val pmtiles = region.getLocalPmtilesFile(mapsDirectory)
        if (!pmtiles.exists()) {
            pmtiles.createNewFile()
        }
    }

    /**
     * Deletes a downloaded region from local storage.
     */
    suspend fun deleteRegion(region: MapRegion) = withContext(Dispatchers.IO) {
        val pmtiles = region.getLocalPmtilesFile(mapsDirectory)
        if (pmtiles.exists()) pmtiles.delete()
        val graphDir = region.getLocalGraphDir(mapsDirectory)
        if (graphDir.exists()) graphDir.deleteRecursively()
    }

    fun getAvailableStorageBytes(): Long {
        return mapsDirectory.freeSpace
    }
}
