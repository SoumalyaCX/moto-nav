package com.motonav.ui.widgets

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
            .background(Color(0xEE0F172A))
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
            .background(
                color = Color(0xEE0F172A),
                shape = RoundedCornerShape(16.dp)
            )
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
            .background(
                color = Color(0xCC0F172A),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        val icon = if (isCharging) "⚡" else "🔋"
        Text(
            text = "$icon $percentage%",
            color = if (percentage <= 20) Color.Red else Color.Green,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
