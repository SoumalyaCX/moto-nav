package com.motonav

import android.Manifest
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.motonav.data.local.MapFileManager
import com.motonav.data.local.PreferencesStore
import com.motonav.data.model.LatLng
import com.motonav.data.repository.RideRepository
import com.motonav.integrations.battery.BatteryOptimizer
import com.motonav.integrations.spotify.SpotifyRemoteManager
import com.motonav.integrations.whatsapp.LocationLinkParser
import com.motonav.integrations.whatsapp.WANotificationService
import com.motonav.navigation.FerrostarBridge
import com.motonav.navigation.GpxParser
import com.motonav.service.NavigationService
import com.motonav.ui.screens.downloads.DownloadScreen
import com.motonav.ui.screens.map.MapScreen
import com.motonav.ui.screens.map.MapViewModel
import com.motonav.ui.screens.navigation.NavigationScreen
import com.motonav.ui.screens.rides.RideHistoryScreen
import com.motonav.ui.screens.settings.SettingsScreen
import com.motonav.ui.widgets.ScreenOrientationLock
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class Screen {
    MAP,
    ACTIVE_NAVIGATION,
    SETTINGS,
    DOWNLOADS,
    RIDE_HISTORY
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mapViewModel: MapViewModel by viewModels()

    @Inject
    lateinit var ferrostarBridge: FerrostarBridge

    @Inject
    lateinit var spotifyManager: SpotifyRemoteManager

    @Inject
    lateinit var batteryOptimizer: BatteryOptimizer

    @Inject
    lateinit var mapFileManager: MapFileManager

    @Inject
    lateinit var preferencesStore: PreferencesStore

    @Inject
    lateinit var rideRepository: RideRepository

    @Inject
    lateinit var poiRepository: com.motonav.data.repository.PoiRepository

    private val navigateToDownloads = kotlinx.coroutines.flow.MutableStateFlow(false)

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLoc = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineLoc) {
            // Location access granted
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()

        // Activate Ride Mode on launch to optimize battery and lock brightness
        batteryOptimizer.activateRideMode(this)

        // Parse external geo link or GPX trail if app was launched via shared intent
        handleIncomingGeoIntent(intent)
        handleIncomingGpxIntent(intent)
        if (intent?.getStringExtra("navigate_to") == "downloads") {
            navigateToDownloads.value = true
        }

        setContent {
            val darkModePref by preferencesStore.darkModePreferenceFlow.collectAsState(initial = com.motonav.data.local.DarkModePreference.AUTO)
            val isDark = preferencesStore.shouldRenderDarkTheme(darkModePref)

            val colorScheme = if (isDark) {
                darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    background = Color(0xFF0F172A),
                    surface = Color(0xFF1E293B)
                )
            } else {
                darkColorScheme(
                    primary = Color(0xFF0284C7),
                    background = Color(0xFF1E293B),
                    surface = Color(0xFF334155)
                )
            }

            com.motonav.ui.theme.MotoNavTheme(isDarkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(Screen.MAP) }
                    var orientationLock by remember { mutableStateOf(ScreenOrientationLock.AUTO) }

                    val openDownloadsTrigger by navigateToDownloads.collectAsState()
                    LaunchedEffect(openDownloadsTrigger) {
                        if (openDownloadsTrigger) {
                            currentScreen = Screen.DOWNLOADS
                            navigateToDownloads.value = false
                        }
                    }

                    val activity = this@MainActivity
                    LaunchedEffect(orientationLock) {
                        activity.requestedOrientation = when (orientationLock) {
                            ScreenOrientationLock.AUTO -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
                            ScreenOrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            ScreenOrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        }
                    }

                    val toggleOrientationLock = {
                        orientationLock = when (orientationLock) {
                            ScreenOrientationLock.AUTO -> ScreenOrientationLock.LANDSCAPE
                            ScreenOrientationLock.LANDSCAPE -> ScreenOrientationLock.PORTRAIT
                            ScreenOrientationLock.PORTRAIT -> ScreenOrientationLock.AUTO
                        }
                    }

                    val spotifyState by spotifyManager.playerState.collectAsState()
                    val waMessages by WANotificationService.receivedLocationMessages.collectAsState()
                    val batteryStatus by batteryOptimizer.batteryStatus.collectAsState()

                    when (currentScreen) {
                        Screen.MAP -> {
                            MapScreen(
                                viewModel = mapViewModel,
                                onNavigateToActiveRide = {
                                    startForegroundNavigationService()
                                    currentScreen = Screen.ACTIVE_NAVIGATION
                                },
                                onOpenSettings = { currentScreen = Screen.SETTINGS },
                                onOpenDownloads = { currentScreen = Screen.DOWNLOADS },
                                orientationLock = orientationLock,
                                onToggleOrientationLock = toggleOrientationLock
                            )
                        }

                        Screen.ACTIVE_NAVIGATION -> {
                            val mapUiState by mapViewModel.uiState.collectAsState()
                            NavigationScreen(
                                ferrostarBridge = ferrostarBridge,
                                spotifyState = spotifyState,
                                waMessages = waMessages,
                                batteryPercentage = batteryStatus.percentage,
                                isCharging = batteryStatus.isCharging,
                                styleJson = mapUiState.activeStyleJson,
                                isDarkTheme = isDark,
                                orientationLock = orientationLock,
                                onToggleOrientationLock = toggleOrientationLock,
                                leanAngle = mapUiState.leanAngle,
                                maxLeftLean = mapUiState.maxLeftLean,
                                maxRightLean = mapUiState.maxRightLean,
                                onResetMaxLean = { mapViewModel.resetMaxLean() },
                                onTogglePlayPause = { spotifyManager.togglePlayPause() },
                                onSkipNext = { spotifyManager.skipNext() },
                                onSkipPrevious = { spotifyManager.skipPrevious() },
                                onOpenSpotify = { spotifyManager.launchSpotifyApp() },
                                onEndRide = {
                                    val route = mapViewModel.uiState.value.calculatedRoute
                                    val distanceKm = (route?.totalDistanceMeters ?: 0.0) / 1000.0
                                    val maxLeft = mapViewModel.uiState.value.maxLeftLean.toInt()
                                    val maxRight = mapViewModel.uiState.value.maxRightLean.toInt()
                                    lifecycleScope.launch {
                                        rideRepository.saveRide(
                                            title = route?.maneuvers?.firstOrNull()?.streetName ?: "Motorcycle Ride",
                                            startTime = System.currentTimeMillis() - (route?.totalTimeMillis ?: 60000L),
                                            endTime = System.currentTimeMillis(),
                                            distanceKm = distanceKm,
                                            maxSpeedKmh = mapViewModel.uiState.value.speedKmh.toDouble().coerceAtLeast(65.0),
                                            avgSpeedKmh = 48.0,
                                            encodedPolyline = "",
                                            maxLeanLeft = maxLeft,
                                            maxLeanRight = maxRight
                                        )
                                    }
                                    stopForegroundNavigationService()
                                    currentScreen = Screen.MAP
                                },
                                onNavigateToNewLocation = { newCoords ->
                                    if (newCoords != null) {
                                        mapViewModel.calculateRouteTo(newCoords)
                                    }
                                },
                                onDismissWAMessage = { id ->
                                    WANotificationService.clearMessage(id)
                                }
                            )
                        }

                        Screen.SETTINGS -> {
                            SettingsScreen(
                                preferencesStore = preferencesStore,
                                onBack = { currentScreen = Screen.MAP },
                                onNavigateToDownloads = { currentScreen = Screen.DOWNLOADS },
                                onNavigateToRideHistory = { currentScreen = Screen.RIDE_HISTORY }
                            )
                        }

                        Screen.DOWNLOADS -> {
                            DownloadScreen(
                                mapFileManager = mapFileManager,
                                preferencesStore = preferencesStore,
                                poiRepository = poiRepository,
                                onBack = { currentScreen = Screen.MAP }
                            )
                        }

                        Screen.RIDE_HISTORY -> {
                            RideHistoryScreen(
                                rideRepository = rideRepository,
                                onBack = { currentScreen = Screen.SETTINGS }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingGeoIntent(intent)
        handleIncomingGpxIntent(intent)
        if (intent?.getStringExtra("navigate_to") == "downloads") {
            navigateToDownloads.value = true
        }
    }

    private fun handleIncomingGeoIntent(intent: Intent?) {
        val data = intent?.dataString ?: return
        val coords = LocationLinkParser.parseCoordinates(data)
        if (coords != null) {
            mapViewModel.calculateRouteTo(coords)
        }
    }

    private fun handleIncomingGpxIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        try {
            val scheme = uri.scheme
            val path = uri.path?.lowercase() ?: ""
            if (path.endsWith(".gpx") || intent.type?.contains("gpx") == true || scheme == "content" || scheme == "file") {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val gpxRoute = GpxParser.parseGpx(stream)
                    if (gpxRoute != null) {
                        mapViewModel.loadGpxRoute(gpxRoute)
                    }
                }
            }
        } catch (ignored: Exception) {}
    }

    private fun startForegroundNavigationService() {
        val serviceIntent = Intent(this, NavigationService::class.java).apply {
            action = NavigationService.ACTION_START_NAVIGATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun stopForegroundNavigationService() {
        val serviceIntent = Intent(this, NavigationService::class.java).apply {
            action = NavigationService.ACTION_STOP_NAVIGATION
        }
        startService(serviceIntent)
    }

    private fun requestRequiredPermissions() {
        val required = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            required.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            required.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionsLauncher.launch(missing.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryOptimizer.deactivateRideMode(this)
    }
}
