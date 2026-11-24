package com.motonav

import android.Manifest
import android.content.Intent
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.motonav.data.local.MapFileManager
import com.motonav.data.local.PreferencesStore
import com.motonav.data.model.LatLng
import com.motonav.integrations.battery.BatteryOptimizer
import com.motonav.integrations.spotify.SpotifyRemoteManager
import com.motonav.integrations.whatsapp.LocationLinkParser
import com.motonav.integrations.whatsapp.WANotificationService
import com.motonav.navigation.FerrostarBridge
import com.motonav.service.NavigationService
import com.motonav.ui.screens.downloads.DownloadScreen
import com.motonav.ui.screens.map.MapScreen
import com.motonav.ui.screens.map.MapViewModel
import com.motonav.ui.screens.navigation.NavigationScreen
import com.motonav.ui.screens.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

enum class Screen {
    MAP,
    ACTIVE_NAVIGATION,
    SETTINGS,
    DOWNLOADS
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

        // Parse external geo link if app was launched via shared location link
        handleIncomingGeoIntent(intent)

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

            MaterialTheme(colorScheme = colorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var currentScreen by remember { mutableStateOf(Screen.MAP) }

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
                                onOpenDownloads = { currentScreen = Screen.DOWNLOADS }
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
                                onTogglePlayPause = { spotifyManager.togglePlayPause() },
                                onSkipNext = { spotifyManager.skipNext() },
                                onSkipPrevious = { spotifyManager.skipPrevious() },
                                onOpenSpotify = { spotifyManager.launchSpotifyApp() },
                                onEndRide = {
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
                                onBack = { currentScreen = Screen.MAP }
                            )
                        }

                        Screen.DOWNLOADS -> {
                            DownloadScreen(
                                mapFileManager = mapFileManager,
                                preferencesStore = preferencesStore,
                                onBack = { currentScreen = Screen.MAP }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIncomingGeoIntent(intent)
    }

    private fun handleIncomingGeoIntent(intent: Intent?) {
        val data = intent?.dataString ?: return
        val coords = LocationLinkParser.parseCoordinates(data)
        if (coords != null) {
            mapViewModel.calculateRouteTo(coords)
        }
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
