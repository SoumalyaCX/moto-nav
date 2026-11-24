package com.motonav.ui.screens.navigation.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ETABar(
    remainingDistanceMeters: Double,
    remainingTimeMillis: Long,
    onEndRide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val distanceKmText = String.format("%.1f km", remainingDistanceMeters / 1000)
    val totalMinutes = (remainingTimeMillis / 1000 / 60).toInt()
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    val durationText = if (hours > 0) "${hours}h ${minutes}m" else "${minutes} min"

    val etaDate = Date(System.currentTimeMillis() + remainingTimeMillis)
    val etaTimeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val etaClockText = etaTimeFormat.format(etaDate)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = Color(0xFA0F172A),
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = durationText,
                        color = Color(0xFF10B981), // Bright green
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "· $distanceKmText",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "ETA $etaClockText",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }

            // End ride button (red circle)
            IconButton(
                onClick = onEndRide,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color(0xFFEF4444), RoundedCornerShape(23.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "End Ride",
                    tint = Color.White
                )
            }
        }
    }
}
