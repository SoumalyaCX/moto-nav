package com.motonav.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motonav.data.local.PreferencesStore
import com.motonav.data.local.db.entities.PoiCategory
import com.motonav.data.local.db.entities.PoiEntity
import com.motonav.data.model.LatLng
import com.motonav.data.model.NavigationRoute
import com.motonav.data.repository.PoiRepository
import com.motonav.integrations.battery.BatteryOptimizer
import com.motonav.integrations.spotify.SpotifyPlayerState
import com.motonav.integrations.spotify.SpotifyRemoteManager
import com.motonav.integrations.whatsapp.WALocationMessage
import com.motonav.integrations.whatsapp.WANotificationService
import com.motonav.map.MapStyleManager
import com.motonav.navigation.FerrostarBridge
import com.motonav.navigation.GraphHopperEngine
import com.motonav.navigation.LocationTracker
import com.motonav.navigation.MotorcycleRoutingOptions
import com.motonav.navigation.OrientationManager
import com.motonav.navigation.RidingStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val userLocation: LatLng = LatLng(22.5726, 88.3639), // Default to Kolkata, WB
    val speedKmh: Float = 0f,
    val bearing: Float = 0f,
    val leanAngle: Float = 0f,
    val maxLeftLean: Float = 0f,
    val maxRightLean: Float = 0f,
    val activeStyleJson: String = "",
    val nearbyPois: List<PoiEntity> = emptyList(),
    val selectedPoi: PoiEntity? = null,
    val selectedCategory: PoiCategory? = null,
    val searchQuery: String = "",
    val calculatedRoute: NavigationRoute? = null,
    val availableRoutes: List<NavigationRoute> = emptyList(),
    val selectedRouteFlavorIndex: Int = 0,
    val isCalculatingRoute: Boolean = false,
    val activeRegionId: String = "west_bengal",
    val isDarkTheme: Boolean = true
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val locationTracker: LocationTracker,
    private val orientationManager: OrientationManager,
    private val mapStyleManager: MapStyleManager,
    private val poiRepository: PoiRepository,
    private val graphHopperEngine: GraphHopperEngine,
    private val ferrostarBridge: FerrostarBridge,
    private val spotifyManager: SpotifyRemoteManager,
    private val batteryOptimizer: BatteryOptimizer,
    private val preferencesStore: PreferencesStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    val spotifyState: StateFlow<SpotifyPlayerState> = spotifyManager.playerState
    val waMessages: StateFlow<List<WALocationMessage>> = WANotificationService.receivedLocationMessages
    val batteryStatus = batteryOptimizer.batteryStatus

    init {
        // Track live GPS location & speed
        viewModelScope.launch {
            locationTracker.getLocationUpdates().collect { loc ->
                _uiState.value = _uiState.value.copy(
                    userLocation = loc.coordinate,
                    speedKmh = loc.speedKmh
                )
            }
        }

        // Track live motorcycle cockpit telemetry (Heading, lean angle, lateral G, max lean)
        viewModelScope.launch {
            orientationManager.getTelemetryUpdates().collect { telemetry ->
                _uiState.value = _uiState.value.copy(
                    bearing = telemetry.heading,
                    leanAngle = telemetry.leanAngle,
                    maxLeftLean = telemetry.maxLeftLean,
                    maxRightLean = telemetry.maxRightLean
                )
            }
        }

        // Observe dark mode preference continuously so map responds dynamically to Settings toggle
        viewModelScope.launch {
            preferencesStore.darkModePreferenceFlow.collect { pref ->
                val isDark = preferencesStore.shouldRenderDarkTheme(pref)
                val regionId = _uiState.value.activeRegionId
                val style = mapStyleManager.resolveActiveMapStyle(regionId = regionId, isDarkTheme = isDark)
                _uiState.value = _uiState.value.copy(
                    isDarkTheme = isDark,
                    activeStyleJson = style
                )
            }
        }

        // Observe region changes continuously
        viewModelScope.launch {
            preferencesStore.activeRegionIdFlow.collect { regionId ->
                val isDark = _uiState.value.isDarkTheme
                val style = mapStyleManager.resolveActiveMapStyle(regionId = regionId, isDarkTheme = isDark)
                _uiState.value = _uiState.value.copy(
                    activeRegionId = regionId,
                    activeStyleJson = style
                )
            }
        }

        // Initial POI load
        viewModelScope.launch {
            loadNearbyPois()
        }
    }

    fun onCategoryFilterSelected(category: PoiCategory?) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        loadNearbyPois()
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        if (query.isNotBlank()) {
            viewModelScope.launch {
                val results = poiRepository.searchPois(query, _uiState.value.userLocation)
                _uiState.value = _uiState.value.copy(nearbyPois = results)
            }
        } else {
            loadNearbyPois()
        }
    }

    fun onPoiSelected(poi: PoiEntity) {
        _uiState.value = _uiState.value.copy(selectedPoi = poi)
    }

    fun dismissPoiDetails() {
        _uiState.value = _uiState.value.copy(selectedPoi = null)
    }

    fun rideToPoi(poi: PoiEntity) {
        _uiState.value = _uiState.value.copy(selectedPoi = null)
        calculateRouteTo(LatLng(poi.latitude, poi.longitude))
    }

    fun calculateRouteTo(destination: LatLng) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCalculatingRoute = true)
            val ridingStyleStr = preferencesStore.ridingStyleFlow.first()
            val style = try { RidingStyle.valueOf(ridingStyleStr) } catch (e: Exception) { RidingStyle.SPORT }
            val avoidHighways = preferencesStore.avoidHighwaysFlow.first()
            val avoidTolls = preferencesStore.avoidTollsFlow.first()

            val options = MotorcycleRoutingOptions(
                style = style,
                avoidHighways = avoidHighways,
                avoidTolls = avoidTolls
            )

            val result = graphHopperEngine.calculateRouteOptions(
                origin = _uiState.value.userLocation,
                destination = destination,
                options = options
            )

            result.onSuccess { routes ->
                _uiState.value = _uiState.value.copy(
                    availableRoutes = routes,
                    calculatedRoute = routes.firstOrNull(),
                    selectedRouteFlavorIndex = 0,
                    isCalculatingRoute = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(isCalculatingRoute = false)
            }
        }
    }

    fun selectRouteFlavor(index: Int) {
        val routes = _uiState.value.availableRoutes
        if (index in routes.indices) {
            _uiState.value = _uiState.value.copy(
                selectedRouteFlavorIndex = index,
                calculatedRoute = routes[index]
            )
        }
    }

    fun loadGpxRoute(route: NavigationRoute) {
        _uiState.value = _uiState.value.copy(
            availableRoutes = listOf(route),
            calculatedRoute = route,
            selectedRouteFlavorIndex = 0,
            selectedPoi = null
        )
    }

    fun resetMaxLean() {
        orientationManager.resetMaxLean()
        _uiState.value = _uiState.value.copy(maxLeftLean = 0f, maxRightLean = 0f)
    }

    fun startNavigation(onStarted: () -> Unit) {
        val route = _uiState.value.calculatedRoute ?: return
        ferrostarBridge.startNavigation(route)
        onStarted()
    }

    fun clearRoute() {
        _uiState.value = _uiState.value.copy(
            calculatedRoute = null,
            availableRoutes = emptyList(),
            selectedPoi = null,
            selectedRouteFlavorIndex = 0
        )
    }

    // Spotify controls
    fun toggleMusic() = spotifyManager.togglePlayPause()
    fun nextTrack() = spotifyManager.skipNext()
    fun prevTrack() = spotifyManager.skipPrevious()
    fun openSpotify() = spotifyManager.launchSpotifyApp()

    // WhatsApp peek dismiss
    fun dismissWAMessage(id: String) = WANotificationService.clearMessage(id)

    private fun loadNearbyPois() {
        viewModelScope.launch {
            val pois = poiRepository.getNearbyPois(
                userLocation = _uiState.value.userLocation,
                category = _uiState.value.selectedCategory,
                radiusKm = 20.0
            )
            _uiState.value = _uiState.value.copy(nearbyPois = pois)
        }
    }
}
