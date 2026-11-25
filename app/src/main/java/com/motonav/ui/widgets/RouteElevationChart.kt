package com.motonav.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.compose.chart.line.lineSpec
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.entry.entryModelOf

/**
 * Renders Route Elevation / Altitude Profile and speed telemetry using Vico Charts.
 */
@Composable
fun RouteElevationChart(
    elevationPoints: List<Float> = listOf(15f, 22f, 48f, 95f, 160f, 240f, 180f, 130f, 90f, 40f),
    title: String = "⛰️ Altitude Profile (Ghats & Passes)",
    modifier: Modifier = Modifier
) {
    val model = remember(elevationPoints) {
        val safePoints = if (elevationPoints.isNotEmpty()) elevationPoints else listOf(10f, 20f, 15f)
        entryModelOf(*safePoints.toTypedArray())
    }

    val gradientShader = remember {
        DynamicShaders.fromBrush(
            Brush.verticalGradient(
                listOf(
                    Color(0xFF38BDF8),
                    Color(0x330284C7),
                    Color(0x000F172A)
                )
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xD90A0F1D))
            .border(1.dp, Color(0x3338BDF8), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFF38BDF8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Chart(
                chart = lineChart(
                    lines = listOf(
                        lineSpec(
                            lineColor = Color(0xFF38BDF8),
                            lineBackgroundShader = gradientShader
                        )
                    )
                ),
                model = model,
                startAxis = rememberStartAxis(),
                bottomAxis = rememberBottomAxis(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            )
        }
    }
}
