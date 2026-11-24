package com.motonav.ui.screens.rides

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.data.local.db.entities.RideEntity
import com.motonav.data.repository.RideRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun RideHistoryScreen(
    rideRepository: RideRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rides by rideRepository.getAllRides().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val totalDistanceKm = rides.sumOf { it.distanceKm }
    val maxOverallSpeed = rides.maxOfOrNull { it.maxSpeedKmh } ?: 0.0
    val maxOverallLean = rides.maxOfOrNull { max(it.maxLeanLeft, it.maxLeanRight) } ?: 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "🏍️ Ride Logbook & Telemetry",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Summary Cockpit Stats Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatItem(label = "TOTAL RIDES", value = "${rides.size}")
                StatItem(label = "DISTANCE", value = String.format("%.1f km", totalDistanceKm))
                StatItem(label = "MAX LEAN", value = "$maxOverallLean°", highlightColor = Color(0xFF10B981))
                StatItem(label = "TOP SPEED", value = "${maxOverallSpeed.toInt()} km/h", highlightColor = Color(0xFF38BDF8))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (rides.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.DirectionsBike,
                        contentDescription = null,
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No saved rides yet",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Complete a ride in MotoNav to see your telemetry, distance, and lean angles logged here.",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rides, key = { it.id }) { ride ->
                    RideItemCard(
                        ride = ride,
                        onDelete = {
                            scope.launch { rideRepository.deleteRide(ride.id) }
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    label: String,
    value: String,
    highlightColor: Color = Color.White
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = highlightColor,
            fontSize = 17.sp,
            fontWeight = FontWeight.Black
        )
        Text(
            text = label,
            color = Color(0xFF94A3B8),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RideItemCard(
    ride: RideEntity,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("MMM d, yyyy · hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(ride.startTime))
    val durationMinutes = ride.durationSeconds / 60
    val durationStr = if (durationMinutes >= 60) {
        "${durationMinutes / 60}h ${durationMinutes % 60}m"
    } else {
        "${durationMinutes}m"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ride.title.ifBlank { "Motorcycle Ride" },
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = dateStr,
                        color = Color.LightGray,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Ride",
                        tint = Color(0xFF64748B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = String.format("%.1f km", ride.distanceKm),
                        color = Color(0xFF38BDF8),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "$durationStr · avg ${ride.avgSpeedKmh.toInt()} km/h",
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }

                // Lean Angle Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LEAN ANGLE",
                            color = Color.LightGray,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "⮜ L ${ride.maxLeanLeft}° | R ${ride.maxLeanRight}° ⮞",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}
