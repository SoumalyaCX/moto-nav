package com.motonav.ui.screens.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.data.local.MapFileManager
import com.motonav.data.local.PreferencesStore
import com.motonav.data.model.MapRegion
import com.motonav.data.model.RegionStatus
import com.motonav.data.repository.PoiRepository
import kotlinx.coroutines.launch

@Composable
fun DownloadScreen(
    mapFileManager: MapFileManager,
    preferencesStore: PreferencesStore,
    poiRepository: PoiRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var regions by remember { mutableStateOf(mapFileManager.getInstalledRegions()) }
    val activeRegionId by preferencesStore.activeRegionIdFlow.collectAsState(initial = MapFileManager.PREPACKAGED_REGION_ID)

    var downloadingRegionId by remember { mutableStateOf<String?>(null) }
    var downloadStatusText by remember { mutableStateOf("") }
    val poiCounts = remember { mutableStateMapOf<String, Int>() }

    // Load initial POI counts for installed regions
    LaunchedEffect(regions) {
        regions.forEach { reg ->
            val count = poiRepository.getPoiCountForRegion(reg.id)
            if (count > 0) poiCounts[reg.id] = count
        }
    }

    val freeSpaceGb = String.format("%.1f GB", mapFileManager.getAvailableStorageBytes() / (1024.0 * 1024.0 * 1024.0))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Offline State Regions",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Free Storage: $freeSpaceGb · Mapbox & OSM Places",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp
                )
            }
        }

        // Live Download Progress Banner if active
        if (downloadingRegionId != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color(0xFF38BDF8),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = downloadStatusText.ifBlank { "Downloading map and stores..." },
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(regions) { region ->
                val isDownloading = downloadingRegionId == region.id
                val poiCount = poiCounts[region.id] ?: 0

                RegionDownloadCard(
                    region = region,
                    isActive = region.id == activeRegionId,
                    isDownloading = isDownloading,
                    poiCount = poiCount,
                    onSelectActive = {
                        scope.launch {
                            preferencesStore.setActiveRegionId(region.id)
                        }
                    },
                    onDownload = {
                        scope.launch {
                            downloadingRegionId = region.id
                            downloadStatusText = "Downloading vector tiles for ${region.displayName}..."

                            // Extract / create offline files
                            mapFileManager.ensurePrepackagedRegionExtracted()

                            // Populate real stores, fuel hubs, and amenities via Mapbox & OSM
                            val count = poiRepository.populatePoisForRegion(region) { status ->
                                downloadStatusText = status
                            }
                            poiCounts[region.id] = count

                            regions = regions.map {
                                if (it.id == region.id) it.copy(status = RegionStatus.DOWNLOADED, downloadProgressPercent = 100) else it
                            }
                            downloadingRegionId = null
                            downloadStatusText = ""
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun RegionDownloadCard(
    region: MapRegion,
    isActive: Boolean,
    isDownloading: Boolean = false,
    poiCount: Int = 0,
    onSelectActive: () -> Unit,
    onDownload: () -> Unit
) {
    val isInstalled = region.status == RegionStatus.DOWNLOADED || region.status == RegionStatus.PRE_INSTALLED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Color(0xFF1E3A5F) else Color(0xFF1E293B)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = region.displayName,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Size: ~${region.approxSizeMb} MB · Mapbox vector tiles + Routing + Stores",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                if (poiCount > 0) {
                    Text(
                        text = "🛒 $poiCount stores, fuel & amenities offline",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else if (region.isPrepackaged) {
                    Text(
                        text = "⭐ Pre-Packaged Regional Bundle (Stores & Amenities Ready)",
                        color = Color(0xFFFBBF24),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(28.dp),
                    color = Color(0xFF38BDF8),
                    strokeWidth = 2.5.dp
                )
            } else if (isInstalled) {
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF10B981).copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "ACTIVE",
                            color = Color(0xFF10B981),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = onSelectActive,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Use", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Get", fontSize = 12.sp)
                }
            }
        }
    }
}
