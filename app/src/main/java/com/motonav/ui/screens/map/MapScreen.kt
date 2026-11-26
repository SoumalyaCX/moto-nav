package com.motonav.ui.screens.map

import android.content.res.Configuration
import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
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
import com.motonav.ui.screens.map.components.PoiDetailCard
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.motonav.navigation.GpxParser
import com.motonav.ui.screens.map.components.RouteIntelAccordion
import com.motonav.ui.theme.MotoNavIcons
import com.motonav.ui.widgets.BatteryWidget
import com.motonav.ui.widgets.CockpitPulseIndicator
import com.motonav.ui.widgets.CompassWidget
import com.motonav.ui.widgets.LeanAngleWidget
import com.motonav.ui.widgets.OrientationLockButton
import com.motonav.ui.widgets.ScreenOrientationLock
import com.motonav.ui.widgets.SpeedIndicator
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.geometry.LatLng as MapLibreLatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style

private val GlassSpecularBorder = Brush.linearGradient(
    listOf(
        Color(0x55FFFFFF),
        Color(0x1F38BDF8),
        Color(0x0AFFFFFF)
    )
)
private val GlassBackground = Color(0xD90A0F1D)

@Composable
fun MapScreen(
    viewModel: MapViewModel,
    onNavigateToActiveRide: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDownloads: () -> Unit = {},
    orientationLock: ScreenOrientationLock = ScreenOrientationLock.AUTO,
    onToggleOrientationLock: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val spotifyState by viewModel.spotifyState.collectAsState()
    val waMessages by viewModel.waMessages.collectAsState()
    val batteryStatus by viewModel.batteryStatus.collectAsState()
    val offlineAlert by viewModel.offlineAlert.collectAsState()

    var recenterTrigger by remember { mutableStateOf(0) }
    var isFrontFacing by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    val hazeState = remember { HazeState() }

    val context = LocalContext.current
    val gpxLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val gpxRoute = GpxParser.parseGpx(stream)
                    if (gpxRoute != null) {
                        viewModel.loadGpxRoute(gpxRoute)
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // 1. Primary View: Map takes 100% full screen in all modes
        MapViewportCanvas(
            uiState = uiState,
            recenterTrigger = recenterTrigger,
            isFrontFacing = isFrontFacing,
            onPoiClicked = { viewModel.onPoiSelected(it) },
            onMapCoordinateTapped = { viewModel.onMapCoordinateTapped(it) },
            onDismissPoi = { viewModel.dismissPoiDetails() },
            onViewportChanged = { minLat, maxLat, minLng, maxLng ->
                viewModel.onViewportChanged(minLat, maxLat, minLng, maxLng)
            },
            modifier = Modifier.fillMaxSize().haze(hazeState)
        )

        // 2. Top Layer: Compact expandable search button & category pills (hidden in landscape when route or POI card is open)
        val hideSearchInLandscape = isLandscape && (uiState.calculatedRoute != null || uiState.selectedPoi != null)
        AnimatedVisibility(
            visible = !hideSearchInLandscape,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart)
        ) {
            Column(
                modifier = Modifier
                    .padding(start = 16.dp, top = 14.dp, end = if (isLandscape) 90.dp else 16.dp)
                    .fillMaxWidth(if (isLandscape && isSearchExpanded) 0.52f else if (isSearchExpanded) 1f else 0.15f)
            ) {
                MapSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.onSearchQueryChanged(it) },
                    onSearch = { viewModel.onSearchQueryChanged(it) },
                    onClear = { viewModel.onSearchQueryChanged("") },
                    isExpanded = isSearchExpanded,
                    onToggleExpand = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) {
                            viewModel.onSearchQueryChanged("")
                            viewModel.onCategoryFilterSelected(null)
                        }
                    }
                )

                // Category filter pills: ONLY visible when search is active
                AnimatedVisibility(
                    visible = isSearchExpanded,
                    enter = fadeIn() + slideInVertically { -it / 2 },
                    exit = fadeOut() + slideOutVertically { -it / 2 }
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        PoiQuickFilters(
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = { viewModel.onCategoryFilterSelected(it) }
                        )

                        // Search Results or Category Results dropdown
                        if ((uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null) && uiState.nearbyPois.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .border(BorderStroke(1.dp, GlassSpecularBorder), RoundedCornerShape(18.dp)),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = GlassBackground)
                            ) {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = if (isLandscape) 180.dp else 260.dp)
                                        .padding(6.dp)
                                ) {
                                    items(uiState.nearbyPois) { poi ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.onPoiSelected(poi)
                                                    isSearchExpanded = false
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            val catInfo = com.motonav.data.model.CategoryResolver.getCategoryInfo(poi.category)
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(catInfo.color.copy(alpha = 0.15f))
                                                    .border(BorderStroke(1.dp, catInfo.color.copy(alpha = 0.35f)), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = com.motonav.data.model.CategoryResolver.getCategoryIcon(poi.category),
                                                    contentDescription = poi.category,
                                                    tint = catInfo.color,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = poi.name,
                                                    color = Color.White,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = poi.address ?: poi.brand ?: "West Bengal",
                                                    color = Color.LightGray,
                                                    fontSize = 11.sp,
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
                    }
                }
            }
        }

        // 3a. Interactive POI / Shop Detail Card (docked on left in landscape)
        AnimatedVisibility(
            visible = uiState.selectedPoi != null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = (if (isLandscape) {
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
                    .width(350.dp)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 14.dp, end = 14.dp, bottom = 86.dp)
                    .fillMaxWidth()
            }).zIndex(20f).hazeChild(state = hazeState, style = HazeMaterials.ultraThin())
        ) {
            uiState.selectedPoi?.let { poi ->
                PoiDetailCard(
                    poi = poi,
                    userLocation = uiState.userLocation,
                    onRideHere = { viewModel.rideToPoi(it) },
                    onDismiss = { viewModel.dismissPoiDetails() },
                    modifier = if (isLandscape) Modifier.fillMaxHeight() else Modifier
                )
            }
        }

        // 3b. Calculated Route Overview Card (docked on left in landscape with scrollable Route Intel Accordion)
        AnimatedVisibility(
            visible = uiState.calculatedRoute != null && uiState.selectedPoi == null,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = (if (isLandscape) {
                Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 16.dp, bottom = 16.dp)
                    .width(350.dp)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 14.dp, end = 14.dp, bottom = 86.dp)
                    .fillMaxWidth()
            }).zIndex(20f).hazeChild(state = hazeState, style = HazeMaterials.ultraThin())
        ) {
            uiState.calculatedRoute?.let { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isLandscape) Modifier.fillMaxHeight() else Modifier)
                        .border(BorderStroke(1.dp, GlassSpecularBorder), RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GlassBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(14.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Icon(
                                        imageVector = MotoNavIcons.Bike,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "ROUTE (${route.ridingStyle})",
                                        color = Color(0xFF38BDF8),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Text(
                                    text = "${route.distanceKmFormatted} · ${route.etaDurationFormatted}",
                                    color = Color.White,
                                    fontSize = 18.sp,
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

                        // 3-Flavor Route Multi-Option Selector (Fastest, Curvy, Scenic)
                        if (uiState.availableRoutes.size > 1) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                uiState.availableRoutes.forEachIndexed { index, optRoute ->
                                    val isSelected = index == uiState.selectedRouteFlavorIndex
                                    val (flavorIcon, flavorLabel) = when (optRoute.ridingStyle) {
                                        "FASTEST" -> MotoNavIcons.Fastest to "Fastest"
                                        "CURVY" -> MotoNavIcons.Curvy to "Curvy"
                                        "SCENIC" -> MotoNavIcons.Viewpoint to "Scenic"
                                        else -> MotoNavIcons.NavigationArrow to optRoute.ridingStyle
                                    }
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) Color(0xFF0284C7) else Color(0x661E293B))
                                            .border(
                                                BorderStroke(1.dp, if (isSelected) Color(0xFF38BDF8) else Color(0x33FFFFFF)),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { viewModel.selectRouteFlavor(index) }
                                            .padding(vertical = 7.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = flavorIcon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color.White else Color(0xFF38BDF8),
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = flavorLabel,
                                            color = if (isSelected) Color.White else Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        // Route Intel Accordion: 3 Switchable Tabs (Road & Tolls, Fuel & Pitstops, Twisties)
                        uiState.routeIntel?.let { intel ->
                            Spacer(modifier = Modifier.height(8.dp))
                            RouteIntelAccordion(
                                intel = intel,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = { viewModel.startNavigation(onNavigateToActiveRide) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .border(BorderStroke(1.dp, Color(0x66FFFFFF)), RoundedCornerShape(14.dp)),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "START RIDE",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 4. Spotify Player: in landscape, placed at bottom-right if route/poi card is open to avoid ANY overlap
        Box(
            modifier = (if (isLandscape) {
                Modifier
                    .align(if (uiState.calculatedRoute != null || uiState.selectedPoi != null) Alignment.BottomEnd else Alignment.BottomStart)
                    .padding(
                        start = if (uiState.calculatedRoute != null || uiState.selectedPoi != null) 0.dp else 16.dp,
                        end = if (uiState.calculatedRoute != null || uiState.selectedPoi != null) 86.dp else 0.dp,
                        bottom = 14.dp
                    )
                    .width(280.dp)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth()
            }).hazeChild(state = hazeState, style = HazeMaterials.ultraThin())
        ) {
            MiniSpotifyPlayer(
                playerState = spotifyState,
                onTogglePlayPause = { viewModel.toggleMusic() },
                onSkipNext = { viewModel.nextTrack() },
                onSkipPrevious = { viewModel.prevTrack() },
                onOpenSpotify = { viewModel.openSpotify() }
            )
        }

        // 4b. Missing Offline State Heads-Up Notification (5s auto-dismiss or manual user dismiss)
        AnimatedVisibility(
            visible = offlineAlert != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp, start = if (isLandscape) 90.dp else 16.dp, end = if (isLandscape) 90.dp else 16.dp)
                .zIndex(15f)
        ) {
            offlineAlert?.let { region ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(if (isLandscape) 0.58f else 1f)
                        .border(
                            BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                            RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF20A0F1D))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🗺️", fontSize = 22.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Offline Map Missing",
                                    color = Color(0xFFF59E0B),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "· 5s",
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = "You entered ${region.displayName}. Map & POIs not downloaded.",
                                color = Color.White,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                viewModel.dismissOfflineAlert()
                                onOpenDownloads()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "📥 Download", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = { viewModel.dismissOfflineAlert() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss",
                                tint = Color.LightGray,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. WhatsApp Location Peek popup (if friend shares location)
        AnimatedVisibility(
            visible = waMessages.isNotEmpty(),
            enter = slideInVertically { -it },
            exit = slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp, start = 80.dp, end = 80.dp)
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

        // 6. Right Side: Drawer in Landscape / Clean HUD in Portrait
        if (isLandscape) {
            // Collapsible HUD Drawer in Landscape Mode
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                // Floating Toggle Tab when closed
                if (!isDrawerOpen) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp))
                            .background(GlassBackground)
                            .border(
                                BorderStroke(1.dp, Color(0x3338BDF8)),
                                RoundedCornerShape(topStart = 18.dp, bottomStart = 18.dp)
                            )
                            .clickable { isDrawerOpen = true }
                            .padding(horizontal = 8.dp, vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Open HUD Drawer",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "HUD",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Sliding Drawer Panel when opened
                AnimatedVisibility(
                    visible = isDrawerOpen,
                    enter = slideInHorizontally { it },
                    exit = slideOutHorizontally { it }
                ) {
                    Column(
                        modifier = Modifier
                            .width(82.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
                            .background(GlassBackground)
                            .border(
                                BorderStroke(1.dp, Color(0x3338BDF8)),
                                RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                            )
                            .verticalScroll(rememberScrollState())
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Close Drawer Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC0F172A))
                                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), CircleShape)
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

                        OrientationLockButton(
                            lockMode = orientationLock,
                            onClick = onToggleOrientationLock
                        )

                        CompassWidget(
                            bearing = uiState.bearing,
                            isFrontFacing = isFrontFacing,
                            onClick = { isFrontFacing = !isFrontFacing }
                        )

                        SpeedIndicator(speedKmh = uiState.speedKmh)

                        LeanAngleWidget(
                            leanAngle = uiState.leanAngle,
                            maxLeft = uiState.maxLeftLean,
                            maxRight = uiState.maxRightLean,
                            onResetMax = { viewModel.resetMaxLean() }
                        )

                        BatteryWidget(
                            percentage = batteryStatus.percentage,
                            isCharging = batteryStatus.isCharging
                        )

                        // Import GPX Route
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC0F172A))
                                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), CircleShape)
                                .clickable { gpxLauncher.launch("*/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "GPX",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Recenter on GPS location with satellite lock pulse
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC0F172A))
                                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), CircleShape)
                                .clickable { recenterTrigger++ },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = "Recenter on Me",
                                tint = Color(0xFF38BDF8)
                            )
                            CockpitPulseIndicator(
                                isLocked = uiState.userLocation != null,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                            )
                        }

                        // Settings Button (Guaranteed visible in drawer)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(Color(0xCC0F172A))
                                .border(BorderStroke(1.dp, Color(0x33FFFFFF)), CircleShape)
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
                }
            }
        } else {
            // Portrait HUD Cluster: Automatically hides when inspecting POI or reviewing calculated route to prevent covering close button
            AnimatedVisibility(
                visible = uiState.selectedPoi == null && uiState.calculatedRoute == null,
                enter = fadeIn() + slideInHorizontally { it },
                exit = fadeOut() + slideOutHorizontally { it },
                modifier = Modifier.align(Alignment.CenterEnd)
            ) {
                Column(
                    modifier = Modifier
                        .padding(end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OrientationLockButton(
                        lockMode = orientationLock,
                        onClick = onToggleOrientationLock
                    )

                    CompassWidget(
                        bearing = uiState.bearing,
                        isFrontFacing = isFrontFacing,
                        onClick = { isFrontFacing = !isFrontFacing }
                    )

                    SpeedIndicator(speedKmh = uiState.speedKmh)

                    LeanAngleWidget(
                        leanAngle = uiState.leanAngle,
                        maxLeft = uiState.maxLeftLean,
                        maxRight = uiState.maxRightLean,
                        onResetMax = { viewModel.resetMaxLean() }
                    )

                    BatteryWidget(
                        percentage = batteryStatus.percentage,
                        isCharging = batteryStatus.isCharging
                    )

                    // Import GPX Route
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC0F172A))
                            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), CircleShape)
                            .clickable { gpxLauncher.launch("*/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "GPX",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC0F172A))
                            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), CircleShape)
                            .clickable { recenterTrigger++ },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = "Recenter on Me",
                            tint = Color(0xFF38BDF8)
                        )
                        CockpitPulseIndicator(
                            isLocked = uiState.userLocation != null,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                        )
                    }

                    // Settings Button (Portrait)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xCC0F172A))
                            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), CircleShape)
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
            }
        }

        // 7. Route Calculating Indicator
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
    isFrontFacing: Boolean = false,
    onPoiClicked: (com.motonav.data.local.db.entities.PoiEntity) -> Unit = {},
    onMapCoordinateTapped: (com.motonav.data.model.LatLng) -> Unit = {},
    onDismissPoi: () -> Unit = {},
    onViewportChanged: (Double, Double, Double, Double) -> Unit = { _, _, _, _ -> },
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

        // Ensure MapLibre native GL surface starts immediately if Activity is already in STARTED/RESUMED state
        when (lifecycleOwner.lifecycle.currentState) {
            Lifecycle.State.STARTED -> mapView.onStart()
            Lifecycle.State.RESUMED -> {
                mapView.onStart()
                mapView.onResume()
            }
            else -> {}
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDestroy()
        }
    }

    var mapInstance by remember { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyleKey by remember { mutableStateOf<String?>(null) }
    var isMapReady by remember { mutableStateOf(false) }

    val resolvedStyle = remember(uiState.activeStyleJson, uiState.isDarkTheme) {
        if (uiState.activeStyleJson.trim().startsWith("{")) {
            uiState.activeStyleJson
        } else if (uiState.isDarkTheme) {
            MapStyleManager.ONLINE_STYLE_DARK
        } else {
            MapStyleManager.ONLINE_STYLE_LIBERTY
        }
    }

    fun handleMapTap(map: MapLibreMap, clickLatLng: MapLibreLatLng) {
        // 1. Check nearby POIs in memory (~150-200m)
        val nearby = uiState.nearbyPois.firstOrNull { poi ->
            val dLat = Math.abs(poi.latitude - clickLatLng.latitude)
            val dLng = Math.abs(poi.longitude - clickLatLng.longitude)
            dLat < 0.0018 && dLng < 0.0018
        }
        if (nearby != null) {
            onPoiClicked(nearby)
            val cam = CameraPosition.Builder()
                .target(MapLibreLatLng(nearby.latitude, nearby.longitude))
                .zoom(Math.max(map.cameraPosition.zoom, 14.5))
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cam), 500)
            return
        }

        // 2. Query rendered vector features on the map (e.g. Decathlon, shops, supermarkets, hospitals)
        val pointF = map.projection.toScreenLocation(clickLatLng)
        val hitBox = android.graphics.RectF(
            pointF.x - 32f,
            pointF.y - 32f,
            pointF.x + 32f,
            pointF.y + 32f
        )
        val features = try { map.queryRenderedFeatures(hitBox) } catch (e: Exception) { emptyList() }
        val poiFeature = features.firstOrNull { feat ->
            val n = feat.getStringProperty("name") ?: feat.getStringProperty("name_en")
            !n.isNullOrBlank()
        }

        if (poiFeature != null) {
            val featName = poiFeature.getStringProperty("name")
                ?: poiFeature.getStringProperty("name_en")
                ?: "Point of Interest"
            val featClass = poiFeature.getStringProperty("class") ?: ""
            val featSubclass = poiFeature.getStringProperty("subclass") ?: ""
            val featCategory = poiFeature.getStringProperty("category") ?: ""
            val featType = poiFeature.getStringProperty("type") ?: ""
            val featMaki = poiFeature.getStringProperty("maki") ?: ""

            val category = com.motonav.data.model.CategoryResolver.resolve(
                name = featName,
                rawClass = featClass,
                subclass = featSubclass,
                category = featCategory,
                type = featType,
                maki = featMaki
            )
            val info = com.motonav.data.model.CategoryResolver.getCategoryInfo(category)
            val matchedRegion = com.motonav.data.local.MapFileManager.AVAILABLE_REGIONS.find { r ->
                clickLatLng.latitude in r.minLat..r.maxLat && clickLatLng.longitude in r.minLng..r.maxLng
            }
            val regionTitle = matchedRegion?.displayName ?: "India"
            val tappedPoi = com.motonav.data.local.db.entities.PoiEntity(
                osmId = System.currentTimeMillis(),
                name = featName,
                category = category,
                latitude = clickLatLng.latitude,
                longitude = clickLatLng.longitude,
                brand = featClass.ifBlank { info.title },
                address = "Near $featName, $regionTitle",
                regionId = matchedRegion?.id ?: "rendered_vector"
            )
            onPoiClicked(tappedPoi)
            val cam = CameraPosition.Builder()
                .target(MapLibreLatLng(clickLatLng.latitude, clickLatLng.longitude))
                .zoom(Math.max(map.cameraPosition.zoom, 14.5))
                .build()
            map.animateCamera(CameraUpdateFactory.newCameraPosition(cam), 500)
            return
        }

        // 3. Fallback: Reverse geocode / Database lookup for clicked coordinate
        onMapCoordinateTapped(com.motonav.data.model.LatLng(clickLatLng.latitude, clickLatLng.longitude))
        val cam = CameraPosition.Builder()
            .target(MapLibreLatLng(clickLatLng.latitude, clickLatLng.longitude))
            .zoom(Math.max(map.cameraPosition.zoom, 14.5))
            .build()
        map.animateCamera(CameraUpdateFactory.newCameraPosition(cam), 500)
    }

    AndroidView(
        factory = {
            mapView.apply {
                getMapAsync { map ->
                    mapInstance = map
                    map.uiSettings.isCompassEnabled = false
                    map.uiSettings.isLogoEnabled = false
                    map.uiSettings.isAttributionEnabled = false

                    map.setOnMarkerClickListener { marker ->
                        val title = marker.title
                        val pos = marker.position
                        val clickedPoi = uiState.nearbyPois.firstOrNull { poi ->
                            (Math.abs(poi.latitude - pos.latitude) < 0.0003 && Math.abs(poi.longitude - pos.longitude) < 0.0003) ||
                                    (title != null && title.contains(poi.name))
                        }
                        if (clickedPoi != null) {
                            onPoiClicked(clickedPoi)
                            val cam = CameraPosition.Builder()
                                .target(MapLibreLatLng(clickedPoi.latitude, clickedPoi.longitude))
                                .zoom(Math.max(map.cameraPosition.zoom, 14.5))
                                .build()
                            map.animateCamera(CameraUpdateFactory.newCameraPosition(cam), 500)
                            true
                        } else {
                            false
                        }
                    }

                    map.addOnMapClickListener { clickLatLng ->
                        handleMapTap(map, clickLatLng)
                        true
                    }

                    loadStyleWithFallback(map, mapView, resolvedStyle) {
                        loadedStyleKey = resolvedStyle
                        isMapReady = true
                        updateMapCameraAndLayers(map, uiState, isFrontFacing)
                    }
                }
            }
        },
        modifier = modifier,
        update = { _ ->
            val map = mapInstance ?: return@AndroidView

            map.setOnMarkerClickListener { marker ->
                val title = marker.title
                val pos = marker.position
                val clickedPoi = uiState.nearbyPois.firstOrNull { poi ->
                    (Math.abs(poi.latitude - pos.latitude) < 0.0003 && Math.abs(poi.longitude - pos.longitude) < 0.0003) ||
                            (title != null && title.contains(poi.name))
                }
                if (clickedPoi != null) {
                    onPoiClicked(clickedPoi)
                    val cam = CameraPosition.Builder()
                        .target(MapLibreLatLng(clickedPoi.latitude, clickedPoi.longitude))
                        .zoom(Math.max(map.cameraPosition.zoom, 14.5))
                        .build()
                    map.animateCamera(CameraUpdateFactory.newCameraPosition(cam), 500)
                    true
                } else {
                    false
                }
            }

            map.addOnMapClickListener { clickLatLng ->
                handleMapTap(map, clickLatLng)
                true
            }

            map.addOnCameraIdleListener {
                try {
                    val bounds = map.projection.visibleRegion.latLngBounds
                    onViewportChanged(
                        bounds.latitudeSouth,
                        bounds.latitudeNorth,
                        bounds.longitudeWest,
                        bounds.longitudeEast
                    )
                } catch (ignored: Exception) {}
            }

            if (loadedStyleKey != resolvedStyle) {
                isMapReady = false
                loadStyleWithFallback(map, mapView, resolvedStyle) {
                    loadedStyleKey = resolvedStyle
                    isMapReady = true
                    enable3DBuildings(map)
                    updateMapCameraAndLayers(map, uiState, isFrontFacing)
                }
            } else if (isMapReady) {
                updateMapCameraAndLayers(map, uiState, isFrontFacing)
            }
        }
    )

    // Recenter camera when trigger increments
    LaunchedEffect(recenterTrigger) {
        if (recenterTrigger > 0) {
            mapInstance?.let { map ->
                val userPos = MapLibreLatLng(uiState.userLocation.latitude, uiState.userLocation.longitude)
                val targetBearing = if (isFrontFacing) uiState.bearing.toDouble() else 0.0
                val targetTilt = if (isFrontFacing) 42.0 else 0.0
                val cameraPosition = CameraPosition.Builder()
                    .target(userPos)
                    .zoom(15.5)
                    .tilt(targetTilt)
                    .bearing(targetBearing)
                    .build()
                map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition), 800)
            }
        }
    }
}

private fun enable3DBuildings(map: MapLibreMap) {
    try {
        val style = map.style ?: return
        if (style.getLayer("3d-buildings") != null) return

        val sourceId = if (style.getSource("composite") != null) "composite" else "openmaptiles"
        if (style.getSource(sourceId) == null) return

        val buildingLayer = org.maplibre.android.style.layers.FillExtrusionLayer("3d-buildings", sourceId)
        buildingLayer.sourceLayer = "building"
        buildingLayer.minZoom = 14.5f
        buildingLayer.setProperties(
            org.maplibre.android.style.layers.PropertyFactory.fillExtrusionColor(android.graphics.Color.parseColor("#141E33")),
            org.maplibre.android.style.layers.PropertyFactory.fillExtrusionHeight(
                org.maplibre.android.style.expressions.Expression.get("height")
            ),
            org.maplibre.android.style.layers.PropertyFactory.fillExtrusionBase(
                org.maplibre.android.style.expressions.Expression.get("min_height")
            ),
            org.maplibre.android.style.layers.PropertyFactory.fillExtrusionOpacity(0.85f)
        )
        style.addLayer(buildingLayer)
    } catch (ignored: Exception) {}
}

private fun loadStyleWithFallback(
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
        android.util.Log.e("MapViewportCanvas", "Failed to load style $styleTarget: $error")
        val isMapboxIssue = styleTarget.contains("mapbox.com") ||
                error.contains("402") || error.contains("429") ||
                error.contains("Payment") || error.contains("Forbidden") || error.contains("Unauthorized")

        if (isMapboxIssue) {
            com.motonav.data.ApiConfig.markMapboxQuotaExceeded("Tile/style load error: $error")
        }

        val fallbackUri = if (styleTarget != MapStyleManager.ONLINE_STYLE_DARK) {
            MapStyleManager.ONLINE_STYLE_DARK
        } else {
            MapStyleManager.OSM_RASTER_FALLBACK
        }

        try {
            if (fallbackUri.trim().startsWith("{")) {
                map.setStyle(Style.Builder().fromJson(fallbackUri)) { onLoaded() }
            } else {
                map.setStyle(Style.Builder().fromUri(fallbackUri)) { onLoaded() }
            }
        } catch (ignored: Exception) {
            try {
                map.setStyle(Style.Builder().fromJson(MapStyleManager.OSM_RASTER_FALLBACK)) { onLoaded() }
            } catch (ignored2: Exception) {}
        }
    }

    try {
        map.setStyle(builder) {
            onLoaded()
        }
    } catch (e: Exception) {
        val isMapboxIssue = styleTarget.contains("mapbox.com")
        if (isMapboxIssue) {
            com.motonav.data.ApiConfig.markMapboxQuotaExceeded("Exception loading style: ${e.message}")
        }
        try {
            map.setStyle(Style.Builder().fromUri(MapStyleManager.ONLINE_STYLE_DARK)) {
                onLoaded()
            }
        } catch (ignored: Exception) {
            try {
                map.setStyle(Style.Builder().fromJson(MapStyleManager.OSM_RASTER_FALLBACK)) {
                    onLoaded()
                }
            } catch (ignored2: Exception) {}
        }
    }
}

private fun updateMapCameraAndLayers(
    map: MapLibreMap,
    uiState: MapUiState,
    isFrontFacing: Boolean
) {
    val userPos = MapLibreLatLng(uiState.userLocation.latitude, uiState.userLocation.longitude)

    // 1. Camera orientation & subtle 3D tilt when zoomed into street level
    val currentCamera = map.cameraPosition
    val targetBearing = if (isFrontFacing) uiState.bearing.toDouble() else 0.0
    val targetTilt = if (isFrontFacing || currentCamera.zoom >= 14.8) 38.0 else 0.0

    val currentTarget = currentCamera.target
    if (currentTarget == null || (currentTarget.latitude == 0.0 && currentTarget.longitude == 0.0)) {
        val cameraPosition = CameraPosition.Builder()
            .target(userPos)
            .zoom(14.5)
            .tilt(targetTilt)
            .bearing(targetBearing)
            .build()
        map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
    }

    // 2. Render Markers and Route Polylines
    map.clear()

    // Draw Route Polyline if present
    uiState.calculatedRoute?.let { route ->
        val polyPoints = route.points.map { MapLibreLatLng(it.latitude, it.longitude) }
        if (polyPoints.isNotEmpty()) {
            map.addPolyline(
                PolylineOptions()
                    .addAll(polyPoints)
                    .color(AndroidColor.parseColor("#38BDF8"))
                    .width(7f)
            )

            val dest = polyPoints.last()
            map.addMarker(
                MarkerOptions()
                    .position(dest)
                    .title("Destination")
            )
        }
    }

    // Draw POIs with rich Google Maps density and accurate category iconography (up to 350 simultaneous places)
    uiState.nearbyPois.take(350).forEach { poi ->
        val isSelected = uiState.selectedPoi?.osmId == poi.osmId
        val prefix = if (isSelected) "Selected: " else ""
        map.addMarker(
            MarkerOptions()
                .position(MapLibreLatLng(poi.latitude, poi.longitude))
                .title("$prefix${poi.name}")
                .snippet(poi.address ?: poi.brand ?: "")
        )
    }

    // User GPS location marker
    map.addMarker(
        MarkerOptions()
            .position(userPos)
            .title("My Location")
            .snippet("${uiState.speedKmh.toInt()} km/h")
    )
}
