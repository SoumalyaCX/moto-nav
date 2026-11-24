package com.motonav.ui.screens.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.integrations.spotify.MiniSpotifyPlayer
import com.motonav.integrations.spotify.SpotifyPlayerState
import com.motonav.integrations.whatsapp.WALocationMessage
import com.motonav.integrations.whatsapp.WALocationPeekWidget
import com.motonav.navigation.FerrostarBridge
import com.motonav.navigation.NavigationStatus
import com.motonav.ui.screens.navigation.components.ETABar
import com.motonav.ui.screens.navigation.components.ManeuverBanner
import com.motonav.ui.widgets.BatteryWidget
import com.motonav.ui.widgets.CompassWidget
import com.motonav.ui.widgets.SpeedIndicator

@Composable
fun NavigationScreen(
    ferrostarBridge: FerrostarBridge,
    spotifyState: SpotifyPlayerState,
    waMessages: List<WALocationMessage>,
    batteryPercentage: Int,
    isCharging: Boolean,
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF020617)) // Deep OLED black for night / high-contrast riding
    ) {
        when (val status = navStatus) {
            is NavigationStatus.Navigating -> {
                // Top: Maneuver instruction banner
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

                // Center Map / Viewport
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "🏍️ Following Route",
                            color = Color(0xFF38BDF8),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Next: ${status.currentManeuver.streetName}",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                }

                // Right HUD: Compass, Speed, Battery
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CompassWidget(bearing = status.currentBearing)
                    SpeedIndicator(speedKmh = status.currentSpeedKmh)
                    BatteryWidget(percentage = batteryPercentage, isCharging = isCharging)
                }

                // Bottom: ETA Bar + WhatsApp Peek + Spotify Player
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // WhatsApp location peek if friend shares location during ride
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

                    // Spotify mini player
                    MiniSpotifyPlayer(
                        playerState = spotifyState,
                        onTogglePlayPause = onTogglePlayPause,
                        onSkipNext = onSkipNext,
                        onSkipPrevious = onSkipPrevious,
                        onOpenSpotify = onOpenSpotify
                    )
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
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                        ) {
                            Text("Back to Map")
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
