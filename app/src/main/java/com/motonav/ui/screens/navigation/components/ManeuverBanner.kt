package com.motonav.ui.screens.navigation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.data.model.ManeuverType
import com.motonav.data.model.RouteManeuver
import com.motonav.ui.theme.MotoNavIcons

/**
 * Enterprise-grade Motorcycle Maneuver Guidance Banner
 * Oversized illuminated turn icon badge, specular glassmorphic frame,
 * high-contrast distance indicators, and zero raw emojis.
 */
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

    val maneuverIcon = when (maneuver.type) {
        ManeuverType.LEFT -> MotoNavIcons.TurnLeftManeuver
        ManeuverType.SHARP_LEFT -> MotoNavIcons.TurnSharpLeftManeuver
        ManeuverType.SLIGHT_LEFT -> MotoNavIcons.TurnSlightLeftManeuver
        ManeuverType.RIGHT -> MotoNavIcons.TurnRightManeuver
        ManeuverType.SHARP_RIGHT -> MotoNavIcons.TurnSharpRightManeuver
        ManeuverType.SLIGHT_RIGHT -> MotoNavIcons.TurnSlightRightManeuver
        ManeuverType.U_TURN -> MotoNavIcons.UTurnManeuver
        ManeuverType.ROUNDABOUT -> MotoNavIcons.RoundaboutManeuver
        ManeuverType.ARRIVE -> MotoNavIcons.Flag
        else -> MotoNavIcons.StraightManeuver
    }

    val glassBorder = if (isOffRoute) {
        BorderStroke(1.dp, Color(0x66EF4444))
    } else {
        BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(
                    Color(0x55FFFFFF),
                    Color(0x2238BDF8),
                    Color(0x0AFFFFFF)
                )
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                color = if (isOffRoute) Color(0xD9991B1B) else Color(0xD90A0F1D)
            )
            .border(glassBorder, RoundedCornerShape(20.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Oversized illuminated maneuver icon badge
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isOffRoute) Color(0x33EF4444) else Color(0x2E38BDF8))
                    .border(
                        BorderStroke(1.dp, if (isOffRoute) Color(0xFFEF4444) else Color(0x4D38BDF8)),
                        RoundedCornerShape(14.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = maneuverIcon,
                    contentDescription = maneuver.instruction,
                    tint = if (isOffRoute) Color.White else Color(0xFF38BDF8),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (isOffRoute) {
                    Text(
                        text = "OFF ROUTE — RECALCULATING",
                        color = Color(0xFFFBBF24),
                        fontSize = 14.sp,
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
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )

                if (maneuver.streetName.isNotBlank()) {
                    Text(
                        text = maneuver.streetName,
                        color = Color(0xFFCBD5E1),
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
