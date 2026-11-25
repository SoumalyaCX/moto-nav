package com.motonav.integrations.spotify

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MiniSpotifyPlayer(
    playerState: SpotifyPlayerState,
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onOpenSpotify: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val hasNotificationAccess = androidx.compose.runtime.remember(playerState) {
        com.motonav.integrations.whatsapp.WANotificationService.isNotificationAccessGranted(context)
    }

    val displayTitle = playerState.trackTitle.takeIf { it.isNotBlank() } ?: "No music playing"
    val displayArtist = when {
        playerState.artistName.isNotBlank() -> playerState.artistName
        !hasNotificationAccess -> "⚠️ Tap to enable Singer & Art"
        playerState.isPlaying -> "Spotify Music"
        else -> "Open Spotify"
    }

    // Glassmorphic cockpit media controller
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(70.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xD90A0F1D)) // Frosted translucent obsidian
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            Color(0x4DFFFFFF),
                            Color(0x1F38BDF8),
                            Color(0x0AFFFFFF)
                        )
                    )
                ),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Album Art Thumbnail (Clickable to open Spotify)
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, Color(0x331DB954)), RoundedCornerShape(12.dp))
                    .clickable { onOpenSpotify() },
                contentAlignment = Alignment.Center
            ) {
                if (playerState.albumArt != null) {
                    Image(
                        bitmap = playerState.albumArt.asImageBitmap(),
                        contentDescription = "Album Cover",
                        modifier = Modifier.size(50.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "Music",
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }

            // Track & Artist/Singer Information (Clickable to open Spotify or Grant Notification Access)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .clickable {
                        if (!hasNotificationAccess) {
                            try {
                                val intent = com.motonav.integrations.whatsapp.WANotificationService.getNotificationSettingsIntent().apply {
                                    flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                onOpenSpotify()
                            }
                        } else {
                            onOpenSpotify()
                        }
                    }
            ) {
                Text(
                    text = displayTitle,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (playerState.isPlaying) {
                        com.motonav.ui.widgets.SpotifyAudioWave(isPlaying = true)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = displayArtist,
                        color = if (!hasNotificationAccess) Color(0xFFFBBF24) else Color(0xFF1DB954),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Glove-friendly motorcycle touch controls (minimum 48dp targets)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onSkipPrevious,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous Track",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Play / Pause prominent circular button with specular gradient
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1ED760), Color(0xFF1DB954))
                            )
                        )
                        .border(BorderStroke(1.dp, Color(0x66FFFFFF)), CircleShape)
                        .clickable { onTogglePlayPause() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                        tint = Color.Black,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(
                    onClick = onSkipNext,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next Track",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}
