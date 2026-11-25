package com.motonav.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Build
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * MotoNav Instrument Iconography
 * Unified high-contrast icons for motorcycle cockpit HUD, telemetry, and roadside POIs.
 */
object MotoNavIcons {
    val Speed: ImageVector = Icons.Default.Speed
    val Compass: ImageVector = Icons.Default.Explore
    val NavigationArrow: ImageVector = Icons.Default.Navigation
    val MyLocationPin: ImageVector = Icons.Default.MyLocation
    val LocationPin: ImageVector = Icons.Default.LocationOn

    // Roadside Amenities
    val Fuel: ImageVector = Icons.Default.LocalGasStation
    val Dhaba: ImageVector = Icons.Default.Restaurant
    val Mechanic: ImageVector = Icons.Default.Build
    val Store: ImageVector = Icons.Default.Store
    val Viewpoint: ImageVector = Icons.Default.Terrain
    val Alert: ImageVector = Icons.Default.Warning

    // Cockpit & Media Controls
    val Play: ImageVector = Icons.Default.PlayArrow
    val Pause: ImageVector = Icons.Default.Pause
    val Next: ImageVector = Icons.Default.SkipNext
    val Previous: ImageVector = Icons.Default.SkipPrevious
    val Music: ImageVector = Icons.Default.MusicNote

    // Actions & Tools
    val Search: ImageVector = Icons.Default.Search
    val Close: ImageVector = Icons.Default.Close
    val Call: ImageVector = Icons.Default.Call
    val Share: ImageVector = Icons.Default.Share
    val Settings: ImageVector = Icons.Default.Settings
    val ChevronLeft: ImageVector = Icons.Default.ChevronLeft
    val ChevronRight: ImageVector = Icons.Default.ChevronRight
}
