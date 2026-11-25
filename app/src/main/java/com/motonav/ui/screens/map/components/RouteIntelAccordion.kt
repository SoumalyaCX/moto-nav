package com.motonav.ui.screens.map.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.data.model.RouteIntel

/**
 * Expandable Motorcycle Route Intelligence Accordion
 * Features 3 switchable tabs:
 * 1. 🛣️ Road & Tolls (Option 3 First)
 * 2. ⛽ Fuel & Pitstops (Option 2 Second)
 * 3. 🔄 Twisties & Curves (Option 1 Third)
 */
@Composable
fun RouteIntelAccordion(
    intel: RouteIntel,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(initialExpanded) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Road & Tolls, 1: Fuel & Pitstops, 2: Twisties

    val arrowRotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "arrowRotation")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x800F172A))
            .border(BorderStroke(1.dp, Color(0x3338BDF8)), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        // Dropdown Header Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "📊 BIKER ROUTE INTEL",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isExpanded) {
                    // Compact Micro-Summary Pills when collapsed
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IntelMicroPill(text = "🛣️ ${intel.highwayPercentage}% Hwy", color = Color(0xFFF59E0B))
                        IntelMicroPill(text = "⛽ ${intel.fuelStopsCount} Fuel", color = Color(0xFF10B981))
                        IntelMicroPill(text = "🔄 ${intel.totalCurvesCount} Bends", color = Color(0xFF8B5CF6))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0x33FFFFFF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.White,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(arrowRotation)
                    )
                }
            }
        }

        // Expandable Content Body
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                // 3 Switchable Tabs (Option 3 First, Option 2 Second, Option 1 Third)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0x661E293B))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IntelTabButton(
                        title = "🛣️ Roads & Tolls",
                        isSelected = selectedTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    IntelTabButton(
                        title = "⛽ Fuel & Pitstops",
                        isSelected = selectedTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                    IntelTabButton(
                        title = "🔄 Twisties",
                        isSelected = selectedTab == 2,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 2 }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tab Content Panel
                when (selectedTab) {
                    0 -> RoadAndTollsTab(intel)
                    1 -> FuelAndPitstopsTab(intel)
                    2 -> TwistiesAndCurvesTab(intel)
                }
            }
        }
    }
}

@Composable
private fun IntelMicroPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(BorderStroke(0.5.dp, color.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    ) {
        Text(text = text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IntelTabButton(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) Color(0xFF0284C7) else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) Color.White else Color(0xFF94A3B8),
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

// ==============================================================================
// TAB 1 (OPTION 3): ROAD COMPOSITION & TOLL INTEL
// ==============================================================================
@Composable
private fun RoadAndTollsTab(intel: RouteIntel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x401E293B))
            .padding(10.dp)
    ) {
        // Highway vs Byway Split Bar
        Text(
            text = "Road Composition: ${intel.highwayName}",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(intel.highwayPercentage.coerceAtLeast(5).toFloat())
                    .fillMaxWidth()
                    .background(Color(0xFFF59E0B))
            )
            Box(
                modifier = Modifier
                    .weight(intel.stateRoadPercentage.coerceAtLeast(5).toFloat())
                    .fillMaxWidth()
                    .background(Color(0xFF38BDF8))
            )
            Box(
                modifier = Modifier
                    .weight(intel.bywayPercentage.coerceAtLeast(5).toFloat())
                    .fillMaxWidth()
                    .background(Color(0xFF64748B))
            )
        }

        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "🟠 ${intel.highwayPercentage}% Highway", color = Color(0xFFF59E0B), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = "🔵 ${intel.stateRoadPercentage}% State", color = Color(0xFF38BDF8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(text = "⚪ ${intel.bywayPercentage}% Byway", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Toll Plaza Intel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(if (intel.tollPlazaCount > 0) Color(0x33F59E0B) else Color(0x3310B981))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (intel.tollPlazaCount > 0) "🛂 Toll Plaza Detected" else "🛂 Toll-Free Route",
                    color = if (intel.tollPlazaCount > 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (intel.tollPlazaCount > 0) "${intel.tollPlazaCount} Fastag Plaza (${intel.tollName})" else "Zero tolls required (₹0)",
                    color = Color.LightGray,
                    fontSize = 10.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (intel.tollPlazaCount > 0) Color(0xFFF59E0B) else Color(0xFF10B981))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (intel.tollPlazaCount > 0) "FASTAG" else "FREE",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Surface: ${intel.roadSurfaceTitle}",
            color = Color(0xFF94A3B8),
            fontSize = 10.sp
        )
    }
}

// ==============================================================================
// TAB 2 (OPTION 2): FUEL & PITSTOP SAFETY RADAR
// ==============================================================================
@Composable
private fun FuelAndPitstopsTab(intel: RouteIntel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x401E293B))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Fuel Stops
            PitstopStatCard(
                icon = "⛽",
                title = "${intel.fuelStopsCount} Pumps",
                subtext = "Next: ${intel.nearestFuelDistanceKm} km",
                accentColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            // Food & Dhabas
            PitstopStatCard(
                icon = "🍛",
                title = "${intel.dhabaFoodCount} Dhabas",
                subtext = "Food & Chai",
                accentColor = Color(0xFFF97316),
                modifier = Modifier.weight(1f)
            )
            // Bike Garage
            PitstopStatCard(
                icon = "🔧",
                title = "${intel.mechanicCount} Garages",
                subtext = "Puncture help",
                accentColor = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x3310B981))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "🛡️", fontSize = 13.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = intel.fuelSafetyRecommendation,
                color = Color(0xFF34D399),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PitstopStatCard(
    icon: String,
    title: String,
    subtext: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x330F172A))
            .border(BorderStroke(0.5.dp, accentColor.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = icon, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(text = subtext, color = Color.LightGray, fontSize = 9.sp, maxLines = 1)
        }
    }
}

// ==============================================================================
// TAB 3 (OPTION 1): TWISTIES & CORNER ANALYSIS
// ==============================================================================
@Composable
private fun TwistiesAndCurvesTab(intel: RouteIntel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x401E293B))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "🔄 ${intel.totalCurvesCount} Curves & Bends",
                    color = Color(0xFFA855F7),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = "${intel.technicalBendsCount} Technical Hairpins · ${intel.flowingSweepersCount} Sweepers",
                    color = Color.LightGray,
                    fontSize = 10.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x40A855F7))
                    .border(BorderStroke(0.5.dp, Color(0xFFA855F7)), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = intel.twistinessScore,
                    color = Color(0xFFD8B4FE),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Lean Rating & Flow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x330F172A))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = intel.leanAnglePotential,
                    color = Color(0xFFF1F5F9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Flow: ${intel.flowCharacter}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
        }
    }
}
