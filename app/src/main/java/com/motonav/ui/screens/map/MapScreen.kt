package com.motonav.ui.screens.map

import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.motonav.data.model.LatLng
import com.motonav.integrations.spotify.MiniSpotifyPlayer
import com.motonav.integrations.whatsapp.WALocationPeekWidget
import com.motonav.map.MapStyleManager
import com.motonav.ui.screens.map.components.MapSearchBar
import com.motonav.ui.screens.map.components.PoiQuickFilters
import com.motonav.ui.widgets.BatteryWidget
import com.motonav.ui.widgets.CompassWidget
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
fun MapScreen(
    viewModel: MapViewModel,
    onNavigateToActiveRide: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val spotifyState by viewModel.spotifyState.collectAsState()
    val waMessages by viewModel.waMessages.collectAsState()
    val batteryStatus by viewModel.batteryStatus.collectAsState()
    var recenterTrigger by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // High-Contrast Motorcycle Map Canvas
        MapViewportCanvas(
            uiState = uiState,
            recenterTrigger = recenterTrigger,
            onPoiClicked = { viewModel.onPoiSelected(it) },
            modifier = Modifier.fillMaxSize()
        )

        // Top Floating Control Layer: Search Bar & POI Filters
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            MapSearchBar(
                query = uiState.searchQuery,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onSearch = { viewModel.onSearchQueryChanged(it) },
                onClear = { viewModel.onSearchQueryChanged("") }
            )

            // Floating Search Results Dropdown (Purulia, Kolaghat, etc.)
            if (uiState.searchQuery.isNotBlank() && uiState.nearbyPois.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF50F172A))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .padding(8.dp)
                    ) {
                        items(uiState.nearbyPois) { poi ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.onPoiSelected(poi)
                                        viewModel.onSearchQueryChanged("")
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val iconEmoji = when (poi.category) {
                                    "CITY" -> "🏙️"
                                    "PETROL" -> "⛽"
                                    "DHABA", "RESTAURANT" -> "🍛"
                                    "MECHANIC" -> "🔧"
                                    "VIEWPOINT" -> "🏔️"
                                    else -> "📍"
                                }
                                Text(text = iconEmoji, fontSize = 22.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = poi.name,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = poi.address ?: poi.brand ?: "West Bengal",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val distKm = LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude)
                                    .distanceTo(LatLng(poi.latitude, poi.longitude)) / 1000.0
                                Text(
                                    text = "%.0f km".format(distKm),
                                    color = Color(0xFF38BDF8),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Divider(color = Color.White.copy(alpha = 0.08f), thickness = 0.5.dp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            PoiQuickFilters(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.onCategoryFilterSelected(it) }
            )

            // Category POI Quick Results (when filter chip is active)
            if (uiState.selectedCategory != null && uiState.nearbyPois.isNotEmpty() && uiState.searchQuery.isBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF50F172A))
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp)
                            .padding(8.dp)
                    ) {
                        items(uiState.nearbyPois) { poi ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.onPoiSelected(poi) }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (poi.category) {
                                        "CITY" -> "🏙️"
                                        "PETROL" -> "⛽"
                                        "DHABA", "RESTAURANT" -> "🍛"
                                        "MECHANIC" -> "🔧"
                                        "VIEWPOINT" -> "🏔️"
                                        else -> "📍"
                                    },
                                    fontSize = 18.sp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = poi.name,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = poi.address ?: poi.brand ?: "",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                val distKm = LatLng(uiState.userLocation.latitude, uiState.userLocation.longitude)
                                    .distanceTo(LatLng(poi.latitude, poi.longitude)) / 1000.0
                                Text(
                                    text = "%.1f km".format(distKm),
                                    color = Color(0xFF10B981),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right HUD Panel: Compass, Speedometer, Battery Indicator
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompassWidget(bearing = uiState.bearing)
            SpeedIndicator(speedKmh = uiState.speedKmh)
            BatteryWidget(
                percentage = batteryStatus.percentage,
                isCharging = batteryStatus.isCharging
            )

            // Recenter on my GPS location button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xEE1E293B))
                    .clickable { recenterTrigger++ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Recenter on Me",
                    tint = Color(0xFF38BDF8)
                )
            }

            // Quick Shortcut buttons for Settings & State Downloads
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xEE1E293B))
                    .clickable { onOpenDownloads() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Downloads",
                    tint = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xEE1E293B))
                    .clickable { onOpenSettings() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        }

        // Bottom Controls: Route Preview / WhatsApp Peek / Spotify Floating Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. WhatsApp Location Peek popup (shows if friend sends location)
            AnimatedVisibility(
                visible = waMessages.isNotEmpty(),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }
            ) {
                waMessages.firstOrNull()?.let { msg ->
                    WALocationPeekWidget(
                        message = msg,
                        onNavigateTo = { coords, link ->
                            if (coords != null) {
                                viewModel.calculateRouteTo(coords)
                            }
                            viewModel.dismissWAMessage(msg.id)
                        },
                        onDismiss = { viewModel.dismissWAMessage(msg.id) }
                    )
                }
            }

            // 2. Route Calculated Overview Card
            AnimatedVisibility(visible = uiState.calculatedRoute != null) {
                uiState.calculatedRoute?.let { route ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFA1E293B))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "🏍️ Motorcycle Route (${route.ridingStyle})",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${route.distanceKmFormatted} · ${route.etaDurationFormatted}",
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }

                                IconButton(onClick = { viewModel.clearRoute() }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Cancel",
                                        tint = Color.LightGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = { viewModel.startNavigation(onNavigateToActiveRide) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Navigation,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "START RIDE",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Mini Spotify Widget (Glove-friendly)
            MiniSpotifyPlayer(
                playerState = spotifyState,
                onTogglePlayPause = { viewModel.toggleMusic() },
                onSkipNext = { viewModel.nextTrack() },
                onSkipPrevious = { viewModel.prevTrack() },
                onOpenSpotify = { viewModel.openSpotify() }
            )
        }

        // Calculating Route Spinner
        if (uiState.isCalculatingRoute) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Calculating motorcycle route...",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Renders real interactive MapLibre vector map with user position, destination marker, and route polyline.
 */
@Composable
fun MapViewportCanvas(
    uiState: MapUiState,
    recenterTrigger: Int,
    onPoiClicked: (com.motonav.data.local.db.entities.PoiEntity) -> Unit,
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
    var isStyleLoaded by remember { mutableStateOf(false) }
    var lastSelectedPoiId by remember { mutableStateOf<Long?>(null) }
    var lastHandledRecenter by remember { mutableStateOf(0) }
    var hasCenteredInitialLocation by remember { mutableStateOf(false) }

    val resolvedStyle = remember(uiState.activeStyleJson, uiState.isDarkTheme) {
        when {
            uiState.activeStyleJson.isNotBlank() && !uiState.activeStyleJson.contains("tiles.openfreemap.org/styles/liberty/style.json") -> {
                uiState.activeStyleJson
            }
            uiState.isDarkTheme -> MapStyleManager.ONLINE_STYLE_DARK
            else -> MapStyleManager.ONLINE_STYLE_LIBERTY
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = {
                mapView.apply {
                    getMapAsync { map ->
                        mapInstance = map
                        map.uiSettings.isCompassEnabled = false
                        map.uiSettings.isLogoEnabled = false
                        map.uiSettings.isAttributionEnabled = false

                        loadMapScreenStyle(map, mapView, resolvedStyle) {
                            loadedStyleKey = resolvedStyle
                            isStyleLoaded = true
                            refreshMapScreenAnnotations(map, uiState)
                            if (!hasCenteredInitialLocation) {
                                val userPos = MapLibreLatLng(uiState.userLocation.latitude, uiState.userLocation.longitude)
                                map.cameraPosition = CameraPosition.Builder()
                                    .target(userPos)
                                    .zoom(12.5)
                                    .build()
                                hasCenteredInitialLocation = true
                            }
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { _ ->
                val map = mapInstance ?: return@AndroidView

                // 1. Reload style ONLY if the target style actually changed
                if (loadedStyleKey != resolvedStyle) {
                    isStyleLoaded = false
                    loadMapScreenStyle(map, mapView, resolvedStyle) {
                        loadedStyleKey = resolvedStyle
                        isStyleLoaded = true
                        refreshMapScreenAnnotations(map, uiState)
                    }
                    return@AndroidView
                }

                // 2. If style is loaded, refresh markers and polyline
                if (isStyleLoaded) {
                    refreshMapScreenAnnotations(map, uiState)

                    // Check for Recenter button press
                    if (recenterTrigger != lastHandledRecenter) {
                        lastHandledRecenter = recenterTrigger
                        val userPos = MapLibreLatLng(uiState.userLocation.latitude, uiState.userLocation.longitude)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(userPos, 14.5), 1000)
                    }

                    // Animate to POI if a new POI was selected
                    val currentPoiId = uiState.selectedPoi?.osmId
                    if (currentPoiId != null && currentPoiId != lastSelectedPoiId) {
                        lastSelectedPoiId = currentPoiId
                        val dest = MapLibreLatLng(uiState.selectedPoi.latitude, uiState.selectedPoi.longitude)
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(dest, 13.5), 1200)
                    }
                }
            }
        )

        // Status pill at bottom right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 80.dp, end = 16.dp)
                .background(Color(0xD00F172A), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "🗺️ MapLibre · ${uiState.activeRegionId.replace('_', ' ').uppercase()}",
                color = Color.LightGray,
                fontSize = 11.sp
            )
        }
    }
}

private fun loadMapScreenStyle(
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
        android.util.Log.e("MapScreen", "Failed to load style $styleTarget: $error, falling back to OSM raster")
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

private fun refreshMapScreenAnnotations(map: MapLibreMap, uiState: MapUiState) {
    map.clear()

    // 1. Rider Marker
    val userPos = MapLibreLatLng(uiState.userLocation.latitude, uiState.userLocation.longitude)
    map.addMarker(
        MarkerOptions()
            .position(userPos)
            .title("Your Location")
    )

    // 2. Route polyline & destination marker
    uiState.calculatedRoute?.let { route ->
        val polyPoints = route.points.map { MapLibreLatLng(it.latitude, it.longitude) }
        if (polyPoints.isNotEmpty()) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(polyPoints)
                    .color(AndroidColor.parseColor("#0284C7"))
                    .width(7f)
            )
            val dest = polyPoints.last()
            map.addMarker(
                MarkerOptions()
                    .position(dest)
                    .title(uiState.selectedPoi?.name ?: "Destination")
            )
        }
    }
}
