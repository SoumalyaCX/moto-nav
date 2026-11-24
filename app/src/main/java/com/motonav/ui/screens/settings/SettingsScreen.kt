package com.motonav.ui.screens.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.motonav.data.local.DarkModePreference
import com.motonav.data.local.PreferencesStore
import com.motonav.integrations.whatsapp.WANotificationService
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    preferencesStore: PreferencesStore,
    onBack: () -> Unit,
    onNavigateToDownloads: () -> Unit,
    onNavigateToRideHistory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val darkModePref by preferencesStore.darkModePreferenceFlow.collectAsState(initial = DarkModePreference.AUTO)
    val ridingStyle by preferencesStore.ridingStyleFlow.collectAsState(initial = "SPORT")
    val avoidHighways by preferencesStore.avoidHighwaysFlow.collectAsState(initial = true)
    val avoidTolls by preferencesStore.avoidTollsFlow.collectAsState(initial = false)
    val batteryRideMode by preferencesStore.isBatteryRideModeEnabledFlow.collectAsState(initial = true)
    val bluetoothDucking by preferencesStore.isBluetoothDuckingEnabledFlow.collectAsState(initial = true)

    val isWaPermissionGranted = WANotificationService.isNotificationAccessGranted(context)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
            .padding(16.dp)
    ) {
        // Header
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
            Text(
                text = "MotoNav Settings",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Offline Vector Map Downloads (Moved from map HUD)
            item {
                SettingsCard(title = "📥 Offline Vector Maps") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToDownloads() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Manage Downloaded Regions",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Download state PMTiles for 100% offline navigation without cellular network",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = onNavigateToDownloads,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Open", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Ride Logbook & Telemetry History
            item {
                SettingsCard(title = "🏍️ Ride Logbook & Telemetry History") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Saved Rides & Cornering Stats",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Review distance, top speed, and maximum cornering lean angles",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = onNavigateToRideHistory,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("View", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 1. Dark Mode / Theme Section (Auto + Manual as requested)
            item {
                SettingsCard(title = "🌓 Map Theme & Dark Mode") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ThemeOptionRow(
                            title = "Auto (Sunset / Sunrise)",
                            subtitle = "Switches to dark mode between 6 PM - 6 AM",
                            selected = darkModePref == DarkModePreference.AUTO,
                            onSelect = { scope.launch { preferencesStore.setDarkModePreference(DarkModePreference.AUTO) } }
                        )
                        ThemeOptionRow(
                            title = "Always Dark (OLED / Night Riding)",
                            subtitle = "Best for battery saving and night contrast",
                            selected = darkModePref == DarkModePreference.DARK,
                            onSelect = { scope.launch { preferencesStore.setDarkModePreference(DarkModePreference.DARK) } }
                        )
                        ThemeOptionRow(
                            title = "Always Light (Daylight Glare)",
                            subtitle = "Maximum contrast under direct midday sun",
                            selected = darkModePref == DarkModePreference.LIGHT,
                            onSelect = { scope.launch { preferencesStore.setDarkModePreference(DarkModePreference.LIGHT) } }
                        )
                    }
                }
            }

            // 2. Motorcycle Routing Preferences
            item {
                SettingsCard(title = "🏍️ Motorcycle Routing Engine") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Riding Profile Weighting",
                            color = Color(0xFF38BDF8),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val styles = listOf(
                            "SPORT" to "Sport (Max curves, avoid boring highways)",
                            "TOURING" to "Touring (Comfortable pace, scenic state roads)",
                            "COMMUTE" to "Commute (Direct, fastest arrival)",
                            "ADVENTURE" to "Adventure (Trails, dirt & gravel friendly)"
                        )

                        styles.forEach { (key, desc) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { scope.launch { preferencesStore.setRidingStyle(key) } }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = ridingStyle == key,
                                    onClick = { scope.launch { preferencesStore.setRidingStyle(key) } },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                                )
                                Text(text = desc, color = Color.White, fontSize = 14.sp)
                            }
                        }

                        Divider(color = Color.DarkGray, modifier = Modifier.padding(vertical = 8.dp))

                        SwitchRow(
                            title = "Avoid Multi-lane Highways",
                            subtitle = "Routes via secondary state roads & twisty corridors",
                            checked = avoidHighways,
                            onCheckedChange = { scope.launch { preferencesStore.setAvoidHighways(it) } }
                        )
                    }
                }
            }

            // Places & Business Search Provider
            item {
                SettingsCard(title = "🔍 Places & POI Search Engine") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val isMapbox = com.motonav.data.ApiConfig.isMapboxConfigured
                                Text(
                                    text = if (isMapbox) "Mapbox Enhanced Search" else "Photon + Overpass OSM",
                                    color = Color(0xFF10B981),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (isMapbox)
                                        "Embedded Mapbox key active in code."
                                    else
                                        "100% Free & Keyless live search for Indian shops, petrol pumps, mechanics, and dhabas.",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F172A))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "ACTIVE",
                                    color = Color(0xFF38BDF8),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // 3. Audio & Helmet Bluetooth
            item {
                SettingsCard(title = "🎧 Helmet Intercom & Audio Guidance") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = "Ducking on Voice Guidance",
                            subtitle = "Lowers Spotify / music volume automatically when turn instructions play",
                            checked = bluetoothDucking,
                            onCheckedChange = { /* toggle in prefs */ }
                        )
                        Text(
                            text = "Voice Language: English (India) - Offline TTS",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // 4. WhatsApp Location Peek Permission
            item {
                SettingsCard(title = "💬 WhatsApp Location Link Peek") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Notification Access",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (isWaPermissionGranted) "Active (Ready to peek location links)" else "Permission Required to read location links",
                                    color = if (isWaPermissionGranted) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    fontSize = 13.sp
                                )
                            }

                            if (!isWaPermissionGranted) {
                                Button(
                                    onClick = { context.startActivity(WANotificationService.getNotificationSettingsIntent()) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                ) {
                                    Text("Grant", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Battery Saver Ride Mode
            item {
                SettingsCard(title = "🔋 Battery Ride Optimization") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        SwitchRow(
                            title = "Enable Ride Preservation Mode",
                            subtitle = "Locks screen on at calibrated 75% brightness, optimizes background processes",
                            checked = batteryRideMode,
                            onCheckedChange = { scope.launch { preferencesStore.setBatteryRideModeEnabled(it) } }
                        )
                        Text(
                            text = "Whitelisted background apps: Spotify, WhatsApp",
                            color = Color(0xFF38BDF8),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
fun ThemeOptionRow(
    title: String,
    subtitle: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = Color.LightGray, fontSize = 12.sp)
        }
    }
}

@Composable
fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = Color.LightGray, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF38BDF8),
                checkedTrackColor = Color(0xFF0369A1)
            )
        )
    }
}
