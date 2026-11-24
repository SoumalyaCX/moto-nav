package com.motonav.ui.screens.map.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.data.local.db.entities.PoiCategory

data class PoiFilterItem(
    val category: PoiCategory,
    val label: String,
    val iconEmoji: String
)

val DEFAULT_POI_FILTERS = listOf(
    PoiFilterItem(PoiCategory.CITY, "Towns / Cities", "🏙️"),
    PoiFilterItem(PoiCategory.PETROL, "Petrol Pump", "⛽"),
    PoiFilterItem(PoiCategory.DHABA, "Dhaba / Food", "🍛"),
    PoiFilterItem(PoiCategory.MECHANIC, "Mechanic / Repair", "🔧"),
    PoiFilterItem(PoiCategory.VIEWPOINT, "Viewpoint", "🏔️"),
    PoiFilterItem(PoiCategory.ATM, "ATM", "🏧")
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
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xCC1E293B),
                    labelColor = Color.White,
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White
                )
            )
        }
    }
}
