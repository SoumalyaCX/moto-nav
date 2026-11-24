package com.motonav.ui.screens.map.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.data.local.db.entities.PoiCategory

data class PoiFilterItem(
    val category: PoiCategory,
    val label: String,
    val iconEmoji: String
)

val DEFAULT_POI_FILTERS = listOf(
    PoiFilterItem(PoiCategory.PETROL, "Petrol", "⛽"),
    PoiFilterItem(PoiCategory.DHABA, "Food / Dhaba", "🍛"),
    PoiFilterItem(PoiCategory.MECHANIC, "Mechanic", "🔧"),
    PoiFilterItem(PoiCategory.VIEWPOINT, "Viewpoint", "🏔️")
)

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
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.forEach { item ->
            val isSelected = selectedCategory == item.category
            FilterChip(
                selected = isSelected,
                onClick = {
                    if (isSelected) onCategorySelected(null) else onCategorySelected(item.category)
                },
                label = {
                    Text(
                        text = "${item.iconEmoji} ${item.label}",
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) Color(0xFF38BDF8) else Color(0x33FFFFFF)
                ),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xD90A0F1D),
                    labelColor = Color(0xFFE2E8F0),
                    selectedContainerColor = Color(0xFF0284C7),
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}
