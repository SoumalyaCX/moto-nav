package com.motonav.ui.screens.map

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
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.data.model.LatLng
import com.motonav.integrations.spotify.MiniSpotifyPlayer
import com.motonav.integrations.whatsapp.WALocationPeekWidget
import com.motonav.ui.screens.map.components.MapSearchBar
import com.motonav.ui.screens.map.components.PoiQuickFilters
import com.motonav.ui.widgets.BatteryWidget
import com.motonav.ui.widgets.CompassWidget
import com.motonav.ui.widgets.SpeedIndicator

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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // High-Contrast Motorcycle Map Canvas
        MapViewportCanvas(
            uiState = uiState,
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

            Spacer(modifier = Modifier.height(8.dp))

            PoiQuickFilters(
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.onCategoryFilterSelected(it) }
            )
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
 * Renders offline vector map viewport with user position and POI markers.
 */
@Composable
fun MapViewportCanvas(
    uiState: MapUiState,
    onPoiClicked: (com.motonav.data.local.db.entities.PoiEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // High-contrast motorcycle map background visualization
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🗺️ MotoNav Offline Vector Engine",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Region: ${uiState.activeRegionId.replace('_', ' ').uppercase()} (Offline Tiles Ready)",
                color = Color(0xFF10B981),
                fontSize = 12.sp
            )
            Text(
                text = "GPS: %.4f, %.4f".format(uiState.userLocation.latitude, uiState.userLocation.longitude),
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }

        // Overlay nearby POIs if searched / filtered
        if (uiState.nearbyPois.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 130.dp, start = 16.dp, end = 90.dp)
                    .background(Color(0xF00F172A), RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                items(uiState.nearbyPois.take(4)) { poi ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPoiClicked(poi) }
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "⛽", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = poi.name,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            poi.brand?.let {
                                Text(text = it, color = Color(0xFF10B981), fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}
