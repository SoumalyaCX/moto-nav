package com.motonav.ui.screens.navigation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.data.model.ManeuverType
import com.motonav.data.model.RouteManeuver

@Composable
fun ManeuverBanner(
    maneuver: RouteManeuver,
    distanceToManeuverMeters: Double,
    isOffRoute: Boolean,
    modifier: Modifier = Modifier
) {
    val distFormatted = if (distanceToManeuverMeters >= 1000) {
        String.format("%.1f km", distanceToManeuverMeters / 1000)
    } else {
        "${distanceToManeuverMeters.toInt()} m"
    }

    val arrowSymbol = when (maneuver.type) {
        ManeuverType.LEFT, ManeuverType.SHARP_LEFT -> "↰"
        ManeuverType.SLIGHT_LEFT -> "↖"
        ManeuverType.RIGHT, ManeuverType.SHARP_RIGHT -> "↱"
        ManeuverType.SLIGHT_RIGHT -> "↗"
        ManeuverType.U_TURN -> "⮌"
        ManeuverType.ROUNDABOUT -> "⟳"
        ManeuverType.ARRIVE -> "🏁"
        else -> "↑"
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = if (isOffRoute) Color(0xFFB91C1C) else Color(0xFA0F172A),
                shape = RoundedCornerShape(18.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Oversized maneuver arrow
            Text(
                text = arrowSymbol,
                color = if (isOffRoute) Color.White else Color(0xFF38BDF8),
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.size(54.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isOffRoute) {
                    Text(
                        text = "OFF ROUTE — RECALCULATING",
                        color = Color.Yellow,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Text(
                        text = "In $distFormatted",
                        color = Color(0xFF38BDF8),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Text(
                    text = maneuver.instruction,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )

                if (maneuver.streetName.isNotBlank()) {
                    Text(
                        text = maneuver.streetName,
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
