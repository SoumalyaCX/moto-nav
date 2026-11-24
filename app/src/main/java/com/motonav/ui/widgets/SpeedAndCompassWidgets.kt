package com.motonav.ui.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
                    Text(
                        text = "🔄",
                        fontSize = 15.sp
                    )
                    Text(
                        text = "AUTO",
                        color = Color(0xFF38BDF8), // Sky blue
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                ScreenOrientationLock.LANDSCAPE -> {
                    Text(
                        text = "🖥️",
                        fontSize = 15.sp
                    )
                    Text(
                        text = "LAND",
                        color = Color(0xFF10B981), // Emerald green (locked)
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                ScreenOrientationLock.PORTRAIT -> {
                    Text(
                        text = "📱",
                        fontSize = 15.sp
                    )
                    Text(
                        text = "PORT",
                        color = Color(0xFFF59E0B), // Amber (locked)
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

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
                // Front-Facing Mode icon: Indicates forward motorcycle cockpit perspective
                Text(
                    text = "🏍️",
                    fontSize = 17.sp
                )
                Text(
                    text = "FRONT",
                    color = Color(0xFF38BDF8),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            } else {
                // North-Facing Mode icon: Red compass needle pointing to North
                Text(
                    text = "▲",
                    color = Color(0xFFEF4444),
                    fontSize = 17.sp,
                    modifier = Modifier.rotate(-bearing)
                )
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

@Composable
fun SpeedIndicator(
    speedKmh: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = 68.dp, height = 68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(GlassBackground)
            .border(BorderStroke(1.dp, GlassSpecularBorder), RoundedCornerShape(18.dp))
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${speedKmh.toInt()}",
                color = if (speedKmh > 90) Color(0xFFEF4444) else Color(0xFF38BDF8),
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

@Composable
fun BatteryWidget(
    percentage: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GlassBackground)
            .border(BorderStroke(1.dp, Color(0x33FFFFFF)), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        val icon = if (isCharging) "⚡" else "🔋"
        Text(
            text = "$icon $percentage%",
            color = if (percentage <= 20) Color(0xFFEF4444) else Color(0xFF10B981),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

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
        absLean >= 42 -> Color(0xFFEF4444) // Deep Red (knee dragging threshold)
        absLean >= 32 -> Color(0xFFF59E0B) // Amber
        absLean >= 20 -> Color(0xFF10B981) // Emerald
        else -> Color(0xFF38BDF8)          // Sky Blue (cruising)
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
                    isLeaningLeft -> "⮜ $absLean°"
                    isLeaningRight -> "$absLean° ⮞"
                    else -> "0°"
                },
                color = leanColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "LEAN",
                color = Color.LightGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "L${maxLeft.toInt()}°|R${maxRight.toInt()}°",
                color = Color(0xFF94A3B8),
                fontSize = 8.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
