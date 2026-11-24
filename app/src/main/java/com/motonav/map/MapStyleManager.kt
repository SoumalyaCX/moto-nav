package com.motonav.map

import android.content.Context
import com.motonav.R
import com.motonav.data.local.MapFileManager
import com.motonav.data.local.PreferencesStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MapStyleManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mapFileManager: MapFileManager,
    private val preferencesStore: PreferencesStore
) {
    companion object {
        const val ONLINE_STYLE_LIBERTY = "https://tiles.openfreemap.org/styles/liberty"
        const val ONLINE_STYLE_BRIGHT = "https://tiles.openfreemap.org/styles/bright"
        const val ONLINE_STYLE_DARK = "https://tiles.openfreemap.org/styles/dark"
        const val ONLINE_FALLBACK = "https://demotiles.maplibre.org/style.json"

        /**
         * Standard OpenStreetMap raster style fallback JSON.
         * 100% free, guaranteed to render on any MapLibre client without external vector sprites/fonts.
         */
        const val OSM_RASTER_FALLBACK = """
        {
          "version": 8,
          "name": "OSM Raster",
          "sources": {
            "osm-raster-tiles": {
              "type": "raster",
              "tiles": [
                "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
              ],
              "tileSize": 256,
              "attribution": "© OpenStreetMap contributors"
            }
          },
          "layers": [
            {
              "id": "osm-raster-layer",
              "type": "raster",
              "source": "osm-raster-tiles",
              "minzoom": 0,
              "maxzoom": 19
            }
          ]
        }
        """
    }

    /**
     * Resolves MapLibre style JSON or URL based on connectivity, region, and dark mode.
     */
    suspend fun resolveActiveMapStyle(
        regionId: String = MapFileManager.PREPACKAGED_REGION_ID,
        forceOffline: Boolean = false,
        isDarkTheme: Boolean = true
    ): String = withContext(Dispatchers.IO) {
        val region = MapFileManager.AVAILABLE_REGIONS.find { it.id == regionId }
            ?: MapFileManager.AVAILABLE_REGIONS.first()

        val pmtilesFile = region.getLocalPmtilesFile(mapFileManager.mapsDirectory)

        if (pmtilesFile.exists() && pmtilesFile.length() > 0) {
            // Load offline vector style with local PMTiles source
            buildLocalPmtilesStyle(pmtilesFile, isDarkTheme)
        } else if (!forceOffline) {
            // Fallback to free OpenFreeMap without API keys
            if (isDarkTheme) ONLINE_STYLE_DARK else ONLINE_STYLE_LIBERTY
        } else {
            // Offline fallback template or OSM raster
            OSM_RASTER_FALLBACK
        }
    }

    private fun buildLocalPmtilesStyle(pmtilesFile: File, isDark: Boolean): String {
        val bgColor = if (isDark) "#121212" else "#f8f9fa"
        val waterColor = if (isDark) "#1e293b" else "#93c5fd"
        val primaryRoadColor = if (isDark) "#3b82f6" else "#2563eb"
        val secondaryRoadColor = if (isDark) "#f59e0b" else "#d97706"
        val textColor = if (isDark) "#f8fafc" else "#0f172a"
        val textHalo = if (isDark) "#000000" else "#ffffff"

        val pmtilesUri = "pmtiles://file://${pmtilesFile.absolutePath}"

        return """
        {
          "version": 8,
          "name": "MotoNav Dynamic ${if (isDark) "Dark" else "Light"}",
          "sources": {
            "openmaptiles": {
              "type": "vector",
              "url": "$pmtilesUri"
            }
          },
          "glyphs": "https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf",
          "layers": [
            {
              "id": "background",
              "type": "background",
              "paint": {
                "background-color": "$bgColor"
              }
            },
            {
              "id": "water",
              "type": "fill",
              "source": "openmaptiles",
              "source-layer": "water",
              "paint": {
                "fill-color": "$waterColor"
              }
            },
            {
              "id": "road_secondary_tertiary",
              "type": "line",
              "source": "openmaptiles",
              "source-layer": "transportation",
              "filter": ["in", "class", "secondary", "tertiary"],
              "paint": {
                "line-color": "$secondaryRoadColor",
                "line-width": ["interpolate", ["linear"], ["zoom"], 8, 2, 16, 8]
              }
            },
            {
              "id": "road_primary",
              "type": "line",
              "source": "openmaptiles",
              "source-layer": "transportation",
              "filter": ["==", "class", "primary"],
              "paint": {
                "line-color": "$primaryRoadColor",
                "line-width": ["interpolate", ["linear"], ["zoom"], 7, 2, 16, 10]
              }
            },
            {
              "id": "road_labels",
              "type": "symbol",
              "source": "openmaptiles",
              "source-layer": "transportation_name",
              "layout": {
                "symbol-placement": "line",
                "text-field": "{name}",
                "text-size": ["interpolate", ["linear"], ["zoom"], 10, 11, 16, 15]
              },
              "paint": {
                "text-color": "$textColor",
                "text-halo-color": "$textHalo",
                "text-halo-width": 2
              }
            },
            {
              "id": "poi_petrol_pumps",
              "type": "circle",
              "source": "openmaptiles",
              "source-layer": "poi",
              "filter": ["==", "subclass", "fuel"],
              "paint": {
                "circle-radius": 7,
                "circle-color": "#10b981",
                "circle-stroke-width": 2,
                "circle-stroke-color": "#ffffff"
              }
            }
          ]
        }
        """.trimIndent()
    }
}
