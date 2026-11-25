package com.motonav.ui.screens.map.components

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.data.ApiConfig
import com.motonav.data.local.db.entities.PoiEntity
import com.motonav.data.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val SpecularBorder = Brush.linearGradient(
    listOf(
        Color(0x66FFFFFF),
        Color(0x2E38BDF8),
        Color(0x0AFFFFFF)
    )
)
private val CardBackground = Color(0xEB0A0F1D)

private val staticImageCache = mutableMapOf<String, Bitmap>()

@Composable
fun PoiDetailCard(
    poi: PoiEntity,
    userLocation: LatLng,
    onRideHere: (PoiEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val distanceMeters = userLocation.distanceTo(LatLng(poi.latitude, poi.longitude))
    val distanceKmFormatted = if (distanceMeters >= 1000) {
        String.format("%.1f km", distanceMeters / 1000.0)
    } else {
        "${distanceMeters.toInt()} m"
    }

    var previewBitmap by remember(poi.osmId) { mutableStateOf<Bitmap?>(staticImageCache["${poi.latitude},${poi.longitude}"]) }

    // Fetch Mapbox Static Satellite/Road snapshot for the POI if online and configured
    LaunchedEffect(poi.osmId) {
        if (previewBitmap == null && ApiConfig.isMapboxConfigured) {
            withContext(Dispatchers.IO) {
                try {
                    val token = ApiConfig.MAPBOX_ACCESS_TOKEN
                    val key = "${poi.latitude},${poi.longitude}"
                    val urlStr = "https://api.mapbox.com/styles/v1/mapbox/streets-v12/static/pin-s+38bdf8(${poi.longitude},${poi.latitude})/${poi.longitude},${poi.latitude},15.5,0,35/600x260@2x?access_token=$token"
                    val conn = URL(urlStr).openConnection() as HttpURLConnection
                    conn.connectTimeout = 4000
                    conn.readTimeout = 4000
                    conn.setRequestProperty("User-Agent", "MotoNav/1.0")
                    if (conn.responseCode == 200) {
                        val bitmap = BitmapFactory.decodeStream(conn.inputStream)
                        if (bitmap != null) {
                            staticImageCache[key] = bitmap
                            withContext(Dispatchers.Main) {
                                previewBitmap = bitmap
                            }
                        }
                    }
                } catch (ignored: Exception) {}
            }
        }
    }

    val categoryInfo = com.motonav.data.model.CategoryResolver.getCategoryInfo(poi.category)
    val categoryBadge = categoryInfo.title to categoryInfo.color

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(CardBackground)
            .border(BorderStroke(1.dp, SpecularBorder), RoundedCornerShape(24.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Subtle top drag handle
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color(0x55FFFFFF))
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Top Bar: Category badge, distance pill, and close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .background(categoryBadge.second.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, categoryBadge.second.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = categoryBadge.first,
                            color = categoryBadge.second,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Proximity Pill
                    Box(
                        modifier = Modifier
                            .background(Color(0x3338BDF8), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "📍 $distanceKmFormatted away",
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // High-visibility Close Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xD91E293B))
                        .border(BorderStroke(1.dp, Color(0x66FFFFFF)), CircleShape)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Shop / Place Title & Brand
            Text(
                text = poi.name,
                color = Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (!poi.brand.isNullOrBlank() && poi.brand != poi.name) {
                Text(
                    text = poi.brand,
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visual Photo / Map Preview Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(115.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF0F172A))
                    .border(BorderStroke(1.dp, Color(0x26FFFFFF)), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (previewBitmap != null) {
                    Image(
                        bitmap = previewBitmap!!.asImageBitmap(),
                        contentDescription = "Map snapshot of ${poi.name}",
                        modifier = Modifier.fillMaxWidth().height(115.dp),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Stylish motorcycle cockpit ambient gradient placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(115.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A), Color(0xFF1E3A8A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🏍️ ${poi.category}",
                                color = Color(0xFF38BDF8),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${poi.latitude.formatCoord()}, ${poi.longitude.formatCoord()}",
                                color = Color(0xFF94A3B8),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Address Details
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = poi.address ?: "Highway Corridor, West Bengal",
                    color = Color(0xFFCBD5E1),
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Hours & Phone row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = poi.openingHours ?: "Open Daily",
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }

                if (!poi.phone.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = poi.phone,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // RIDE HERE button
                Button(
                    onClick = { onRideHere(poi) },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RIDE HERE",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Phone Call button if phone available
                if (!poi.phone.isNullOrBlank()) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${poi.phone}")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0x4D38BDF8))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CALL",
                            color = Color(0xFF38BDF8),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Share Button
                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, poi.name)
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Check out ${poi.name} on MotoNav:\nhttps://www.google.com/maps/search/?api=1&query=${poi.latitude},${poi.longitude}"
                            )
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Location").apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    },
                    modifier = Modifier
                        .size(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

private fun Double.formatCoord(): String = String.format("%.4f", this)
