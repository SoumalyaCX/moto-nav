package com.motonav.map

import android.content.Context
import com.motonav.R
import com.motonav.data.ApiConfig
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
            if (ApiConfig.isMapboxConfigured) {
                // High-density Mapbox Navigation styles with highway shields and 3D buildings
                if (isDarkTheme) {
                    "https://api.mapbox.com/styles/v1/mapbox/navigation-night-v1?access_token=${ApiConfig.MAPBOX_ACCESS_TOKEN}"
                } else {
                    "https://api.mapbox.com/styles/v1/mapbox/navigation-day-v1?access_token=${ApiConfig.MAPBOX_ACCESS_TOKEN}"
                }
            } else {
                // Fallback to free OpenFreeMap without API keys
                if (isDarkTheme) ONLINE_STYLE_DARK else ONLINE_STYLE_LIBERTY
            }
        } else {
            // Offline fallback template or OSM raster
            OSM_RASTER_FALLBACK
        }
    }

    private fun buildLocalPmtilesStyle(pmtilesFile: File, isDark: Boolean): String {
        // High-density motorcycle cockpit map palette with greenery, buildings, and highway shields
        val bgColor = if (isDark) "#080C14" else "#F8FAFC"
        val waterColor = if (isDark) "#0B1628" else "#BAE6FD"
        val parkColor = if (isDark) "#0E2419" else "#DCFCE7"
        val buildingColor = if (isDark) "#111827" else "#E2E8F0"
        val buildingBorder = if (isDark) "#1E293B" else "#CBD5E1"
        val expresswayColor = if (isDark) "#F59E0B" else "#D97706"
        val primaryRoadColor = if (isDark) "#0284C7" else "#0369A1"
        val secondaryRoadColor = if (isDark) "#334155" else "#94A3B8"
        val minorRoadColor = if (isDark) "#1E293B" else "#E2E8F0"
        val textColor = if (isDark) "#F1F5F9" else "#0F172A"
        val textHalo = if (isDark) "#080C14" else "#FFFFFF"

        val pmtilesUri = "pmtiles://file://${pmtilesFile.absolutePath}"

        return """
        {
          "version": 8,
          "name": "MotoNav Dense Biker ${if (isDark) "Night" else "Day"}",
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
              "id": "landuse_park",
              "type": "fill",
              "source": "openmaptiles",
              "source-layer": "landuse",
              "filter": ["in", "class", "park", "forest", "grass", "recreation_ground"],
              "paint": {
                "fill-color": "$parkColor",
                "fill-opacity": 0.85
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
              "id": "building_footprints",
              "type": "fill",
              "source": "openmaptiles",
              "source-layer": "building",
              "paint": {
                "fill-color": "$buildingColor",
                "fill-outline-color": "$buildingBorder"
              }
            },
            {
              "id": "road_minor",
              "type": "line",
              "source": "openmaptiles",
              "source-layer": "transportation",
              "filter": ["in", "class", "minor", "service", "residential"],
              "minzoom": 12,
              "paint": {
                "line-color": "$minorRoadColor",
                "line-width": ["interpolate", ["linear"], ["zoom"], 12, 1.0, 16, 3.5]
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
                "line-width": ["interpolate", ["linear"], ["zoom"], 7, 1.5, 16, 6.0]
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
                "line-width": ["interpolate", ["linear"], ["zoom"], 6, 2.5, 16, 9.0]
              }
            },
            {
              "id": "road_motorway_trunk",
              "type": "line",
              "source": "openmaptiles",
              "source-layer": "transportation",
              "filter": ["in", "class", "motorway", "trunk"],
              "paint": {
                "line-color": "$expresswayColor",
                "line-width": ["interpolate", ["linear"], ["zoom"], 5, 2.8, 16, 12.0]
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
                "text-size": ["interpolate", ["linear"], ["zoom"], 10, 11, 16, 14]
              },
              "paint": {
                "text-color": "$textColor",
                "text-halo-color": "$textHalo",
                "text-halo-width": 2.5
              }
            },
            {
              "id": "place_labels",
              "type": "symbol",
              "source": "openmaptiles",
              "source-layer": "place",
              "layout": {
                "text-field": "{name}",
                "text-size": ["interpolate", ["linear"], ["zoom"], 6, 10, 14, 16]
              },
              "paint": {
                "text-color": "$textColor",
                "text-halo-color": "$textHalo",
                "text-halo-width": 2.0
              }
            },
            {
              "id": "poi_petrol_pumps",
              "type": "circle",
              "source": "openmaptiles",
              "source-layer": "poi",
              "filter": ["==", "subclass", "fuel"],
              "paint": {
                "circle-radius": 6,
                "circle-color": "#10B981",
                "circle-stroke-width": 2,
                "circle-stroke-color": "#FFFFFF"
              }
            }
          ]
        }
        """.trimIndent()
    }
}
