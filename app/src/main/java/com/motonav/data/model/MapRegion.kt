package com.motonav.data.model

import java.io.File

enum class RegionStatus {
    PRE_INSTALLED,
    DOWNLOADED,
    DOWNLOADING,
    NOT_DOWNLOADED
}

data class MapRegion(
    val id: String,
    val displayName: String,
    val stateCode: String,
    val approxSizeMb: Int,
    val minLat: Double,
    val minLng: Double,
    val maxLat: Double,
    val maxLng: Double,
    val pmtilesFileName: String = "$id.pmtiles",
    val graphArchiveName: String = "$id.ghz",
    val poiDbFileName: String = "${id}_poi.sqlite",
    val isPrepackaged: Boolean = false,
    val status: RegionStatus = if (isPrepackaged) RegionStatus.PRE_INSTALLED else RegionStatus.NOT_DOWNLOADED,
    val downloadProgressPercent: Int = if (isPrepackaged) 100 else 0
) {
    fun getLocalPmtilesFile(baseDir: File): File = File(baseDir, pmtilesFileName)
    fun getLocalGraphDir(baseDir: File): File = File(baseDir, "${id}-gh")
    fun getLocalPoiDbFile(baseDir: File): File = File(baseDir, poiDbFileName)

    fun isInstalledLocally(baseDir: File): Boolean {
        return getLocalPmtilesFile(baseDir).exists()
    }
}
