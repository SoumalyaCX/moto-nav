package com.motonav.ui.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.ui.theme.MotoNavIcons

enum class ScreenOrientationLock {
    AUTO,
    LANDSCAPE,
    PORTRAIT
}

private val GlassSpecularBorder = Brush.linearGradient(
    listOf(
        Color(0x55FFFFFF),
        Color(0x1F38BDF8),
        Color(0x0AFFFFFF)
    )
)

private val GlassBackground = Color(0xD90A0F1D)

/**
 * Enterprise-grade Motorcycle Cockpit Orientation Lock Toggle
 * Clean vector iconography, specular glass border, and high-contrast lock indicators.
 */
@Composable
fun OrientationLockButton(
    lockMode: ScreenOrientationLock,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(GlassBackground)
            .border(BorderStroke(1.dp, GlassSpecularBorder), CircleShape)
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (lockMode) {
                ScreenOrientationLock.AUTO -> {
                    Icon(
                        imageVector = MotoNavIcons.ScreenRotation,
                        contentDescription = "Auto Orientation",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "AUTO",
                        color = Color(0xFF38BDF8),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                ScreenOrientationLock.LANDSCAPE -> {
                    Icon(
                        imageVector = MotoNavIcons.Landscape,
                        contentDescription = "Landscape Locked",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "LAND",
                        color = Color(0xFF10B981),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                ScreenOrientationLock.PORTRAIT -> {
                    Icon(
                        imageVector = MotoNavIcons.Portrait,
                        contentDescription = "Portrait Locked",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PORT",
                        color = Color(0xFFF59E0B),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

/**
 * Precision Compass & Camera Heading Gauge
 * Front-Facing cockpit bike mode vs True North compass needle with zero emojis.
 */
@Composable
fun CompassWidget(
    bearing: Float,
    isFrontFacing: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(GlassBackground)
            .border(BorderStroke(1.dp, GlassSpecularBorder), CircleShape)
            .clickable { onClick() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isFrontFacing) {
                Icon(
                    imageVector = MotoNavIcons.Bike,
                    contentDescription = "Forward Cockpit Heading",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "FRONT",
                    color = Color(0xFF38BDF8),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            } else {
                Icon(
                    imageVector = MotoNavIcons.NavigationArrow,
                    contentDescription = "North Bearing",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier
                        .size(18.dp)
                        .rotate(-bearing)
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = "NORTH",
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Superbike Digital Speedometer Gauge
 * Animated speed numeral with dynamic red-line over-speed indicator.
 */
@Composable
fun SpeedIndicator(
    speedKmh: Float,
    modifier: Modifier = Modifier
) {
    val animatedSpeed by animateIntAsState(targetValue = speedKmh.toInt(), label = "speedAnim")
    val isOverSpeed = animatedSpeed > 90
    val speedColor by animateColorAsState(
        targetValue = if (isOverSpeed) Color(0xFFEF4444) else Color(0xFF38BDF8),
        label = "speedColor"
    )

    Box(
        modifier = modifier
            .size(width = 68.dp, height = 68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GlassBackground)
            .border(
                BorderStroke(
                    1.dp,
                    if (isOverSpeed) Brush.linearGradient(listOf(Color(0xFFEF4444), Color(0x66EF4444))) else GlassSpecularBorder
                ),
                RoundedCornerShape(18.dp)
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$animatedSpeed",
                color = speedColor,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "km/h",
                color = Color.LightGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Status Bar Battery & Power Widget
 * Vector charging and charge-level states.
 */
@Composable
fun BatteryWidget(
    percentage: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    val batteryColor = when {
        isCharging -> Color(0xFF10B981)
        percentage <= 20 -> Color(0xFFEF4444)
        percentage <= 40 -> Color(0xFFF59E0B)
        else -> Color(0xFF10B981)
    }

    val batteryIcon = when {
        isCharging -> MotoNavIcons.BatteryCharging
        percentage <= 20 -> MotoNavIcons.BatteryAlert
        else -> MotoNavIcons.BatteryFull
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GlassBackground)
            .border(BorderStroke(1.dp, Color(0x2EFFFFFF)), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            imageVector = batteryIcon,
            contentDescription = "Battery Status",
            tint = batteryColor,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$percentage%",
            color = batteryColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Cornering Lean Angle Telemetry Gauge
 * Real-time gyroscope degree tracking with peak angle retention.
 */
@Composable
fun LeanAngleWidget(
    leanAngle: Float,
    maxLeft: Float = 0f,
    maxRight: Float = 0f,
    onResetMax: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val absLean = kotlin.math.abs(leanAngle).toInt()
    val isLeaningLeft = leanAngle < -1.5f
    val isLeaningRight = leanAngle > 1.5f

    val leanColor = when {
        absLean >= 42 -> Color(0xFFEF4444) // Deep Red (track threshold)
        absLean >= 30 -> Color(0xFFF59E0B) // Amber
        absLean >= 18 -> Color(0xFF10B981) // Emerald
        else -> Color(0xFF38BDF8)          // Sky Blue
    }

    Box(
        modifier = modifier
            .size(width = 68.dp, height = 68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GlassBackground)
            .border(BorderStroke(1.dp, GlassSpecularBorder), RoundedCornerShape(18.dp))
            .clickable { onResetMax() }
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when {
                    isLeaningLeft -> "◀ $absLean°"
                    isLeaningRight -> "$absLean° ▶"
                    else -> "$absLean°"
                },
                color = leanColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "LEAN",
                color = Color.LightGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = "L${maxLeft.toInt()}°|R${maxRight.toInt()}°",
                color = Color(0xFF94A3B8),
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
