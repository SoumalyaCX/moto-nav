package com.motonav.ui.screens.navigation

import android.content.res.Configuration
import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.motonav.data.model.LatLng
import com.motonav.integrations.spotify.MiniSpotifyPlayer
import com.motonav.integrations.spotify.SpotifyPlayerState
import com.motonav.integrations.whatsapp.WALocationMessage
import com.motonav.integrations.whatsapp.WALocationPeekWidget
import com.motonav.map.MapStyleManager
import com.motonav.navigation.FerrostarBridge
import com.motonav.navigation.NavigationStatus
import com.motonav.ui.screens.navigation.components.ETABar
import com.motonav.ui.screens.navigation.components.ManeuverBanner
import com.motonav.ui.widgets.BatteryWidget
import com.motonav.ui.widgets.CompassWidget
import com.motonav.ui.widgets.OrientationLockButton
import com.motonav.ui.widgets.ScreenOrientationLock
import com.motonav.ui.widgets.SpeedIndicator
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.geometry.LatLng as MapLibreLatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

@Composable
fun NavigationScreen(
    ferrostarBridge: FerrostarBridge,
    spotifyState: SpotifyPlayerState,
    waMessages: List<WALocationMessage>,
    batteryPercentage: Int,
    isCharging: Boolean,
    styleJson: String = "",
    isDarkTheme: Boolean = true,
    orientationLock: ScreenOrientationLock = ScreenOrientationLock.AUTO,
    onToggleOrientationLock: () -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onOpenSpotify: () -> Unit,
    onEndRide: () -> Unit,
    onNavigateToNewLocation: (com.motonav.data.model.LatLng?) -> Unit,
    onDismissWAMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navStatus by ferrostarBridge.navigationState.collectAsState()
    var isFrontFacing by remember { mutableStateOf(true) } // Motorcycle Front-Facing mode by default

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Deep OLED black for high-contrast motorcycle riding
    ) {
        when (val status = navStatus) {
            is NavigationStatus.Navigating -> {
                if (isLandscape) {
                    // ==============================================================
                    // LANDSCAPE MOTORCYCLE COCKPIT DUAL-PANE LAYOUT
                    // ==============================================================
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Left Dashboard Panel (320dp width, full height)
                        Column(
                            modifier = Modifier
                                .width(320.dp)
                                .fillMaxHeight()
                                .background(Color(0xF5020617))
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Turn Maneuver Header
                            ManeuverBanner(
                                maneuver = status.currentManeuver,
                                distanceToManeuverMeters = status.distanceToNextManeuverMeters,
                                isOffRoute = status.isOffRoute
                            )

                            // Cockpit Indicators Row: Speedometer + Compass + Orientation Lock + Battery
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SpeedIndicator(speedKmh = status.currentSpeedKmh)
                                CompassWidget(
                                    bearing = status.currentBearing,
                                    isFrontFacing = isFrontFacing,
                                    onClick = { isFrontFacing = !isFrontFacing }
                                )
                                OrientationLockButton(
                                    lockMode = orientationLock,
                                    onClick = onToggleOrientationLock
                                )
                                BatteryWidget(
                                    percentage = batteryPercentage,
                                    isCharging = isCharging
                                )
                            }

                            // WhatsApp Location Alert (if shared during ride)
                            AnimatedVisibility(visible = waMessages.isNotEmpty()) {
                                waMessages.firstOrNull()?.let { msg ->
                                    WALocationPeekWidget(
                                        message = msg,
                                        onNavigateTo = { coords, _ ->
                                            onNavigateToNewLocation(coords)
                                            onDismissWAMessage(msg.id)
                                        },
                                        onDismiss = { onDismissWAMessage(msg.id) }
                                    )
                                }
                            }

                            // ETA bar with compact End Ride button
                            ETABar(
                                remainingDistanceMeters = status.remainingDistanceTotalMeters,
                                remainingTimeMillis = status.remainingTimeTotalMillis,
                                onEndRide = onEndRide
                            )

                            // Spotify Glove-Friendly Player with album art
                            MiniSpotifyPlayer(
                                playerState = spotifyState,
                                onTogglePlayPause = onTogglePlayPause,
                                onSkipNext = onSkipNext,
                                onSkipPrevious = onSkipPrevious,
                                onOpenSpotify = onOpenSpotify
                            )
                        }

                        // Right 3D Navigation Map Viewport (Takes remaining screen width & 100% height)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        ) {
                            NavigationMapViewport(
                                status = status,
                                styleJson = styleJson,
                                isDarkTheme = isDarkTheme,
                                isFrontFacing = isFrontFacing,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                } else {
                    // ==============================================================
                    // PORTRAIT MOTORCYCLE COCKPIT LAYOUT
                    // ==============================================================
                    // 1. Fullscreen 3D Motorcycle Navigation Map Viewport
                    NavigationMapViewport(
                        status = status,
                        styleJson = styleJson,
                        isDarkTheme = isDarkTheme,
                        isFrontFacing = isFrontFacing,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 2. Top: Floating Turn-by-Turn Maneuver Instruction Banner
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        ManeuverBanner(
                            maneuver = status.currentManeuver,
                            distanceToManeuverMeters = status.distanceToNextManeuverMeters,
                            isOffRoute = status.isOffRoute
                        )
                    }

                    // 3. Right HUD: Orientation Lock, Compass, Speedometer, Battery Indicator
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OrientationLockButton(
                            lockMode = orientationLock,
                            onClick = onToggleOrientationLock
                        )
                        CompassWidget(
                            bearing = status.currentBearing,
                            isFrontFacing = isFrontFacing,
                            onClick = { isFrontFacing = !isFrontFacing }
                        )
                        SpeedIndicator(speedKmh = status.currentSpeedKmh)
                        BatteryWidget(percentage = batteryPercentage, isCharging = isCharging)
                    }

                    // 4. Bottom Controls: ETA Bar + WhatsApp Location Peek + Spotify Floating Bar
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnimatedVisibility(visible = waMessages.isNotEmpty()) {
                            waMessages.firstOrNull()?.let { msg ->
                                WALocationPeekWidget(
                                    message = msg,
                                    onNavigateTo = { coords, _ ->
                                        onNavigateToNewLocation(coords)
                                        onDismissWAMessage(msg.id)
                                    },
                                    onDismiss = { onDismissWAMessage(msg.id) }
                                )
                            }
                        }

                        // ETA Bar
                        ETABar(
                            remainingDistanceMeters = status.remainingDistanceTotalMeters,
                            remainingTimeMillis = status.remainingTimeTotalMillis,
                            onEndRide = onEndRide
                        )

                        // Spotify mini player (glove-friendly touch targets with album art)
                        MiniSpotifyPlayer(
                            playerState = spotifyState,
                            onTogglePlayPause = onTogglePlayPause,
                            onSkipNext = onSkipNext,
                            onSkipPrevious = onSkipPrevious,
                            onOpenSpotify = onOpenSpotify
                        )
                    }
                }
            }

            is NavigationStatus.Arrived -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "🏁 Destination Reached!",
                            color = Color(0xFF10B981),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Great ride! Hope you enjoyed the curves.",
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = onEndRide,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Back to Map", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            is NavigationStatus.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "No active navigation session.", color = Color.Gray)
                }
            }
        }
    }
}

/**
 * 3D Tilted MapLibre Navigation Viewport locked to rider bearing and location.
 */
@Composable
fun NavigationMapViewport(
    status: NavigationStatus.Navigating,
    styleJson: String,
    isDarkTheme: Boolean,
    isFrontFacing: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            onCreate(Bundle())
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyleKey by remember { mutableStateOf<String?>(null) }
    var isMapReady by remember { mutableStateOf(false) }

    val resolvedStyle = remember(styleJson, isDarkTheme) {
        if (styleJson.trim().startsWith("{")) {
            styleJson
        } else if (isDarkTheme) {
            MapStyleManager.ONLINE_STYLE_DARK
        } else {
            MapStyleManager.ONLINE_STYLE_LIBERTY
        }
    }

    val riderCoord = status.riderLocation ?: status.currentManeuver.coordinate

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    mapInstance = map
                    map.uiSettings.isCompassEnabled = false
                    map.uiSettings.isLogoEnabled = false
                    map.uiSettings.isAttributionEnabled = false

                    loadNavMapStyle(map, mapView, resolvedStyle) {
                        loadedStyleKey = resolvedStyle
                        isMapReady = true
                        updateNavCameraAndRoute(map, status, riderCoord, isFrontFacing)
                    }
                }
            }
        },
        modifier = modifier,
        update = { _ ->
            val map = mapInstance ?: return@AndroidView

            if (loadedStyleKey != resolvedStyle) {
                isMapReady = false
                loadNavMapStyle(map, mapView, resolvedStyle) {
                    loadedStyleKey = resolvedStyle
                    isMapReady = true
                    updateNavCameraAndRoute(map, status, riderCoord, isFrontFacing)
                }
            } else if (isMapReady) {
                updateNavCameraAndRoute(map, status, riderCoord, isFrontFacing)
            }
        }
    )
}

private fun loadNavMapStyle(
    map: MapLibreMap,
    mapView: MapView,
    styleTarget: String,
    onLoaded: () -> Unit
) {
    val builder = if (styleTarget.trim().startsWith("{")) {
        Style.Builder().fromJson(styleTarget)
    } else {
        Style.Builder().fromUri(styleTarget)
    }

    mapView.addOnDidFailLoadingMapListener { error ->
        android.util.Log.e("NavMapViewport", "Failed to load style $styleTarget: $error, falling back to OSM raster")
        try {
            map.setStyle(Style.Builder().fromJson(MapStyleManager.OSM_RASTER_FALLBACK)) {
                onLoaded()
            }
        } catch (ignored: Exception) {}
    }

    try {
        map.setStyle(builder) {
            onLoaded()
        }
    } catch (e: Exception) {
        try {
            map.setStyle(Style.Builder().fromJson(MapStyleManager.OSM_RASTER_FALLBACK)) {
                onLoaded()
            }
        } catch (ignored: Exception) {}
    }
}

private fun updateNavCameraAndRoute(
    map: MapLibreMap,
    status: NavigationStatus.Navigating,
    riderCoord: LatLng,
    isFrontFacing: Boolean
) {
    val riderPos = MapLibreLatLng(riderCoord.latitude, riderCoord.longitude)

    // Camera: Front-Facing (3D cockpit view oriented with motorcycle heading) vs North-Facing (2D top-down North up)
    val targetBearing = if (isFrontFacing) status.currentBearing.toDouble() else 0.0
    val targetTilt = if (isFrontFacing) 50.0 else 0.0
    val targetZoom = if (isFrontFacing) 16.5 else 15.0

    val cameraPosition = CameraPosition.Builder()
        .target(riderPos)
        .zoom(targetZoom)
        .tilt(targetTilt)
        .bearing(targetBearing)
        .build()

    map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 700)

    // 2. Render Route Polyline & Markers
    map.clear()

    // Draw active route polyline
    status.route?.let { route ->
        val polyPoints = route.points.map { MapLibreLatLng(it.latitude, it.longitude) }
        if (polyPoints.isNotEmpty()) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(polyPoints)
                    .color(AndroidColor.parseColor("#00E5FF")) // High-contrast neon cyan
                    .width(8f)
            )

            val dest = polyPoints.last()
            map.addMarker(
                MarkerOptions()
                    .position(dest)
                    .title("🏁 Destination")
            )
        }
    }

    // 3. Motorcycle Rider Marker
    map.addMarker(
        MarkerOptions()
            .position(riderPos)
            .title("🏍️ Speed: ${status.currentSpeedKmh.toInt()} km/h")
    )
}
