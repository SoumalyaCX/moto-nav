package com.motonav.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AltRoute
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBike
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RoundaboutRight
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.StayCurrentLandscape
import androidx.compose.material.icons.filled.StayCurrentPortrait
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Straight
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Toll
import androidx.compose.material.icons.filled.TurnLeft
import androidx.compose.material.icons.filled.TurnRight
import androidx.compose.material.icons.filled.TurnSharpLeft
import androidx.compose.material.icons.filled.TurnSharpRight
import androidx.compose.material.icons.filled.TurnSlightLeft
import androidx.compose.material.icons.filled.TurnSlightRight
import androidx.compose.material.icons.filled.UTurnLeft
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * MotoNav Instrument Iconography
 * Enterprise-grade, cohesive vector icon system for motorcycle cockpit HUD,
 * telemetry gauges, POI categories, and navigation turn guidance.
 * Replaces all raw emojis with precision-engineered Material vectors.
 */
object MotoNavIcons {
    // Primary Rider & Navigation Instrument Icons
    val Bike: ImageVector = Icons.Default.DirectionsBike
    val Speed: ImageVector = Icons.Default.Speed
    val Compass: ImageVector = Icons.Default.Explore
    val NavigationArrow: ImageVector = Icons.Default.Navigation
    val MyLocationPin: ImageVector = Icons.Default.MyLocation
    val LocationPin: ImageVector = Icons.Default.LocationOn
    val Flag: ImageVector = Icons.Default.Flag

    // Roadside Amenities & Places of Interest
    val Fuel: ImageVector = Icons.Default.LocalGasStation
    val Dhaba: ImageVector = Icons.Default.Restaurant
    val Mechanic: ImageVector = Icons.Default.Build
    val Store: ImageVector = Icons.Default.Store
    val Viewpoint: ImageVector = Icons.Default.Terrain
    val Temple: ImageVector = Icons.Default.AccountBalance
    val Landmark: ImageVector = Icons.Default.Place
    val City: ImageVector = Icons.Default.LocationCity
    val Cafe: ImageVector = Icons.Default.LocalCafe
    val Hotel: ImageVector = Icons.Default.Hotel
    val Park: ImageVector = Icons.Default.Park
    val Atm: ImageVector = Icons.Default.LocalAtm
    val Hospital: ImageVector = Icons.Default.LocalHospital
    val Alert: ImageVector = Icons.Default.Warning

    // Route Intel & Highway Characteristics
    val Highway: ImageVector = Icons.Default.AltRoute
    val Toll: ImageVector = Icons.Default.Toll
    val Twisties: ImageVector = Icons.Default.Timeline
    val Shield: ImageVector = Icons.Default.Shield
    val Fastest: ImageVector = Icons.Default.Bolt
    val Curvy: ImageVector = Icons.Default.AltRoute

    // Navigation Maneuver Turn Icons
    val StraightManeuver: ImageVector = Icons.Default.Straight
    val TurnLeftManeuver: ImageVector = Icons.Default.TurnLeft
    val TurnRightManeuver: ImageVector = Icons.Default.TurnRight
    val TurnSlightLeftManeuver: ImageVector = Icons.Default.TurnSlightLeft
    val TurnSlightRightManeuver: ImageVector = Icons.Default.TurnSlightRight
    val TurnSharpLeftManeuver: ImageVector = Icons.Default.TurnSharpLeft
    val TurnSharpRightManeuver: ImageVector = Icons.Default.TurnSharpRight
    val UTurnManeuver: ImageVector = Icons.Default.UTurnLeft
    val RoundaboutManeuver: ImageVector = Icons.Default.RoundaboutRight

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
    val KeyboardArrowDown: ImageVector = Icons.Default.KeyboardArrowDown
    val KeyboardArrowUp: ImageVector = Icons.Default.KeyboardArrowUp

    // Telemetry & Hardware Sensors
    val BatteryCharging: ImageVector = Icons.Default.BatteryChargingFull
    val BatteryFull: ImageVector = Icons.Default.BatteryFull
    val BatteryAlert: ImageVector = Icons.Default.BatteryAlert
    val ScreenRotation: ImageVector = Icons.Default.ScreenRotation
    val Portrait: ImageVector = Icons.Default.StayCurrentPortrait
    val Landscape: ImageVector = Icons.Default.StayCurrentLandscape
}
