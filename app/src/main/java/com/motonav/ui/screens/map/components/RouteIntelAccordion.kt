package com.motonav.ui.screens.map.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.data.model.RouteIntel
import com.motonav.ui.theme.MotoNavIcons

private val SpecularAccordionBorder = Brush.linearGradient(
    listOf(
        Color(0x55FFFFFF),
        Color(0x2238BDF8),
        Color(0x0AFFFFFF)
    )
)

/**
 * Enterprise-grade Glassmorphic Route Intelligence Accordion
 * Features 3 switchable tabs:
 * 1. Roads & Tolls (Option 3 First)
 * 2. Fuel & Pitstops (Option 2 Second)
 * 3. Twisties & Curves (Option 1 Third)
 * 100% vector iconography — zero raw emojis.
 */
@Composable
fun RouteIntelAccordion(
    intel: RouteIntel,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(initialExpanded) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Roads & Tolls, 1: Fuel & Pitstops, 2: Twisties

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "arrowRotation"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xCC0A0F1D))
            .border(BorderStroke(1.dp, SpecularAccordionBorder), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Dropdown Header Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = MotoNavIcons.Highway,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "ROUTE INTEL",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!isExpanded) {
                    // Compact Micro-Summary Badges when collapsed
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IntelMicroBadge(
                            icon = MotoNavIcons.Highway,
                            text = "${intel.highwayPercentage}% Hwy",
                            color = Color(0xFFF59E0B)
                        )
                        IntelMicroBadge(
                            icon = MotoNavIcons.Fuel,
                            text = "${intel.fuelStopsCount} Fuel",
                            color = Color(0xFF10B981)
                        )
                        IntelMicroBadge(
                            icon = MotoNavIcons.Twisties,
                            text = "${intel.totalCurvesCount} Curves",
                            color = Color(0xFFA855F7)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0x26FFFFFF))
                        .border(BorderStroke(0.5.dp, Color(0x33FFFFFF)), CircleShape),
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
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut()
        ) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                // 3 Switchable Tabs: Roads & Tolls, Fuel & Pitstops, Twisties
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x80131D31))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    IntelTabButton(
                        icon = MotoNavIcons.Highway,
                        title = "Roads & Tolls",
                        isSelected = selectedTab == 0,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 0 }
                    )
                    IntelTabButton(
                        icon = MotoNavIcons.Fuel,
                        title = "Fuel & Stops",
                        isSelected = selectedTab == 1,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 1 }
                    )
                    IntelTabButton(
                        icon = MotoNavIcons.Twisties,
                        title = "Twisties",
                        isSelected = selectedTab == 2,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedTab = 2 }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Smooth animated content transition between tabs
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) togetherWith fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) },
                    label = "tabTransition"
                ) { targetTab ->
                    when (targetTab) {
                        0 -> RoadAndTollsTab(intel)
                        1 -> FuelAndPitstopsTab(intel)
                        2 -> TwistiesAndCurvesTab(intel)
                    }
                }
            }
        }
    }
}

@Composable
private fun IntelMicroBadge(
    icon: ImageVector,
    text: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(BorderStroke(0.5.dp, color.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(10.dp)
        )
        Text(text = text, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun IntelTabButton(
    icon: ImageVector,
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val animatedBg by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF0284C7) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabBg"
    )
    val animatedColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF94A3B8),
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tabColor"
    )

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(animatedBg)
            .clickable { onClick() }
            .padding(vertical = 7.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = animatedColor,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = title,
            color = animatedColor,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
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
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x55131D31))
            .border(BorderStroke(1.dp, Color(0x1FFFFFFF)), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        // Highway vs Byway Split Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Road Composition: ${intel.highwayName}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Surface: ${intel.roadSurfaceTitle}",
                color = Color(0xFF94A3B8),
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Multi-segment progress bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(intel.highwayPercentage.coerceAtLeast(5).toFloat())
                    .height(8.dp)
                    .background(Color(0xFFF59E0B))
            )
            Box(
                modifier = Modifier
                    .weight(intel.stateRoadPercentage.coerceAtLeast(5).toFloat())
                    .height(8.dp)
                    .background(Color(0xFF38BDF8))
            )
            Box(
                modifier = Modifier
                    .weight(intel.bywayPercentage.coerceAtLeast(5).toFloat())
                    .height(8.dp)
                    .background(Color(0xFF64748B))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Clean Legend Indicators
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RoadLegendItem(color = Color(0xFFF59E0B), label = "${intel.highwayPercentage}% National")
            RoadLegendItem(color = Color(0xFF38BDF8), label = "${intel.stateRoadPercentage}% State")
            RoadLegendItem(color = Color(0xFF94A3B8), label = "${intel.bywayPercentage}% Byway")
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Toll Plaza Status Card
        val hasTolls = intel.tollPlazaCount > 0
        val tollColor = if (hasTolls) Color(0xFFF59E0B) else Color(0xFF10B981)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(tollColor.copy(alpha = 0.12f))
                .border(BorderStroke(1.dp, tollColor.copy(alpha = 0.3f)), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = MotoNavIcons.Toll,
                    contentDescription = null,
                    tint = tollColor,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = if (hasTolls) "Toll Plaza Detected" else "Toll-Free Route",
                        color = tollColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (hasTolls) "${intel.tollPlazaCount} Fastag Plaza (${intel.tollName})" else "Zero tolls required (₹0)",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(tollColor)
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (hasTolls) "FASTAG" else "FREE",
                    color = Color.Black,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun RoadLegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x55131D31))
            .border(BorderStroke(1.dp, Color(0x1FFFFFFF)), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Fuel Stops Card
            PitstopStatCard(
                icon = MotoNavIcons.Fuel,
                title = "${intel.fuelStopsCount} Pumps",
                subtext = "Next: ${intel.nearestFuelDistanceKm} km",
                accentColor = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            // Food & Dhabas Card
            PitstopStatCard(
                icon = MotoNavIcons.Dhaba,
                title = "${intel.dhabaFoodCount} Dhabas",
                subtext = "Food & Chai",
                accentColor = Color(0xFFF97316),
                modifier = Modifier.weight(1f)
            )
            // Bike Garages Card
            PitstopStatCard(
                icon = MotoNavIcons.Mechanic,
                title = "${intel.mechanicCount} Garages",
                subtext = "Puncture help",
                accentColor = Color(0xFF38BDF8),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Safety Recommendation Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x1A10B981))
                .border(BorderStroke(1.dp, Color(0x3310B981)), RoundedCornerShape(10.dp))
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = MotoNavIcons.Shield,
                contentDescription = null,
                tint = Color(0xFF10B981),
                modifier = Modifier.size(16.dp)
            )
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
    icon: ImageVector,
    title: String,
    subtext: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x660A0F1D))
            .border(BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtext,
                color = Color(0xFF94A3B8),
                fontSize = 9.sp,
                maxLines = 1
            )
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
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x55131D31))
            .border(BorderStroke(1.dp, Color(0x1FFFFFFF)), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(Color(0x26A855F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = MotoNavIcons.Twisties,
                        contentDescription = null,
                        tint = Color(0xFFA855F7),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "${intel.totalCurvesCount} Curves & Bends",
                        color = Color(0xFFA855F7),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${intel.technicalBendsCount} Technical Hairpins · ${intel.flowingSweepersCount} Sweepers",
                        color = Color(0xFFCBD5E1),
                        fontSize = 10.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0x33A855F7))
                    .border(BorderStroke(1.dp, Color(0xFFA855F7)), RoundedCornerShape(6.dp))
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = intel.twistinessScore,
                    color = Color(0xFFD8B4FE),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Lean Rating & Flow Character
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0x660A0F1D))
                .border(BorderStroke(1.dp, Color(0x1A38BDF8)), RoundedCornerShape(10.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = MotoNavIcons.Bike,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(16.dp)
            )
            Column {
                Text(
                    text = intel.leanAnglePotential,
                    color = Color(0xFFF1F5F9),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Flow: ${intel.flowCharacter}",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp
                )
            }
        }
    }
}
