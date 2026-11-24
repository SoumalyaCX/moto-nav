package com.motonav.ui.screens.navigation

import android.content.res.Configuration
import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
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
import com.motonav.ui.widgets.LeanAngleWidget
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
    leanAngle: Float = 0f,
    maxLeftLean: Float = 0f,
    maxRightLean: Float = 0f,
    onResetMaxLean: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val navStatus by ferrostarBridge.navigationState.collectAsState()
    var isFrontFacing by remember { mutableStateOf(true) } // Motorcycle Front-Facing mode by default
    var isDrawerOpen by remember { mutableStateOf(false) }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Deep OLED black for high-contrast motorcycle riding
    ) {
        when (val status = navStatus) {
            is NavigationStatus.Navigating -> {
                // 1. Primary Full-Screen 3D Navigation Map (in all orientations)
                NavigationMapViewport(
                    status = status,
                    styleJson = styleJson,
                    isDarkTheme = isDarkTheme,
                    isFrontFacing = isFrontFacing,
                    modifier = Modifier.fillMaxSize()
                )

                if (isLandscape) {
                    // ==============================================================
                    // LANDSCAPE PRIMARY MAP VIEW WITH BOTTOM-LEFT SPOTIFY & RIGHT DRAWER
                    // ==============================================================

                    // Top-Left: Floating Maneuver Instruction Banner
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(start = 16.dp, top = 14.dp)
                            .width(310.dp)
                    ) {
                        ManeuverBanner(
                            maneuver = status.currentManeuver,
                            distanceToManeuverMeters = status.distanceToNextManeuverMeters,
                            isOffRoute = status.isOffRoute
                        )
                    }

                    // Bottom-Left: Floating ETA Bar & Spotify Widget
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, bottom = 14.dp)
                            .width(310.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // WhatsApp Location Alert if received
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

                        ETABar(
                            remainingDistanceMeters = status.remainingDistanceTotalMeters,
                            remainingTimeMillis = status.remainingTimeTotalMillis,
                            onEndRide = onEndRide
                        )

                        MiniSpotifyPlayer(
                            playerState = spotifyState,
                            onTogglePlayPause = onTogglePlayPause,
                            onSkipNext = onSkipNext,
                            onSkipPrevious = onSkipPrevious,
                            onOpenSpotify = onOpenSpotify
                        )
                    }

                    // Right Edge: Collapsible HUD Drawer
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        // Toggle Tab when closed
                        if (!isDrawerOpen) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                                    .background(Color(0xF0020617))
                                    .clickable { isDrawerOpen = true }
                                    .padding(horizontal = 8.dp, vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronLeft,
                                        contentDescription = "Open Gauges",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${status.currentSpeedKmh.toInt()}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }
                        }

                        // Drawer Panel when open
                        AnimatedVisibility(
                            visible = isDrawerOpen,
                            enter = slideInHorizontally { it },
                            exit = slideOutHorizontally { it }
                        ) {
                            Column(
                                modifier = Modifier
                                    .width(76.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xF5020617))
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Close Drawer
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1E293B))
                                        .clickable { isDrawerOpen = false },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Close HUD",
                                        tint = Color.LightGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                SpeedIndicator(speedKmh = status.currentSpeedKmh)

                                LeanAngleWidget(
                                    leanAngle = leanAngle,
                                    maxLeft = maxLeftLean,
                                    maxRight = maxRightLean,
                                    onResetMax = onResetMaxLean
                                )

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
                        }
                    }
                } else {
                    // ==============================================================
                    // PORTRAIT MOTORCYCLE COCKPIT LAYOUT
                    // ==============================================================
                    // Top: Floating Turn-by-Turn Maneuver Instruction Banner
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

                    // Right HUD: Orientation Lock, Compass, Speedometer, Battery Indicator
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
                        LeanAngleWidget(
                            leanAngle = leanAngle,
                            maxLeft = maxLeftLean,
                            maxRight = maxRightLean,
                            onResetMax = onResetMaxLean
                        )
                        BatteryWidget(percentage = batteryPercentage, isCharging = isCharging)
                    }

                    // Bottom Controls: WhatsApp Peek + ETA Bar + Spotify Player
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

                        ETABar(
                            remainingDistanceMeters = status.remainingDistanceTotalMeters,
                            remainingTimeMillis = status.remainingTimeTotalMillis,
                            onEndRide = onEndRide
                        )

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
