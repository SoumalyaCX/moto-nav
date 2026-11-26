package com.motonav.ui.screens.map.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.data.local.db.entities.PoiCategory
import com.motonav.ui.theme.MotoNavIcons

data class PoiFilterItem(
    val category: PoiCategory,
    val label: String,
    val icon: ImageVector
)

val DEFAULT_POI_FILTERS = listOf(
    PoiFilterItem(PoiCategory.STORE, "Shops", MotoNavIcons.Store),
    PoiFilterItem(PoiCategory.PETROL, "Fuel", MotoNavIcons.Fuel),
    PoiFilterItem(PoiCategory.DHABA, "Dhabas", MotoNavIcons.Dhaba),
    PoiFilterItem(PoiCategory.TEMPLE, "Temples", MotoNavIcons.Temple),
    PoiFilterItem(PoiCategory.ATTRACTION, "Landmarks", MotoNavIcons.Landmark),
    PoiFilterItem(PoiCategory.CITY, "Streets & Hubs", MotoNavIcons.City),
    PoiFilterItem(PoiCategory.VIEWPOINT, "Viewpoints", MotoNavIcons.Viewpoint),
    PoiFilterItem(PoiCategory.CAFE, "Cafes", MotoNavIcons.Cafe),
    PoiFilterItem(PoiCategory.HOTEL, "Hotels", MotoNavIcons.Hotel),
    PoiFilterItem(PoiCategory.MECHANIC, "Mechanic", MotoNavIcons.Mechanic),
    PoiFilterItem(PoiCategory.PARK, "Parks", MotoNavIcons.Park),
    PoiFilterItem(PoiCategory.ATM, "ATMs", MotoNavIcons.Atm),
    PoiFilterItem(PoiCategory.HOSPITAL, "Hospital", MotoNavIcons.Hospital)
)

/**
 * Enterprise-grade Glassmorphic Category Filter Strip
 * Smooth spring-animated pills with cohesive vector iconography,
 * high-contrast selection glow, and zero emoji clutter.
 */
@Composable
fun PoiQuickFilters(
    selectedCategory: PoiCategory?,
    onCategorySelected: (PoiCategory?) -> Unit,
    modifier: Modifier = Modifier,
    filters: List<PoiFilterItem> = DEFAULT_POI_FILTERS
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        filters.forEach { item ->
            val isSelected = selectedCategory == item.category

            val animatedBgColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF0284C7) else Color(0xD90A0F1D),
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "chipBg"
            )
            val animatedBorderColor by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF38BDF8) else Color(0x2EFFFFFF),
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "chipBorder"
            )
            val animatedContentColor by animateColorAsState(
                targetValue = if (isSelected) Color.White else Color(0xFF38BDF8),
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "chipIcon"
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(animatedBgColor)
                    .border(
                        BorderStroke(1.dp, animatedBorderColor),
                        RoundedCornerShape(20.dp)
                    )
                    .clickable {
                        if (isSelected) onCategorySelected(null) else onCategorySelected(item.category)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    modifier = Modifier.size(15.dp),
                    tint = animatedContentColor
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = item.label,
                    color = if (isSelected) Color.White else Color(0xFFE2E8F0),
                    fontSize = 12.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }
    }
}
