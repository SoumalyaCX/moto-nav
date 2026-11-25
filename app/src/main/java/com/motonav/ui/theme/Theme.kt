package com.motonav.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val CockpitObsidian = Color(0xFF0A0F1D)
val CockpitSurface = Color(0xFF131D31)
val CockpitElectricCyan = Color(0xFF38BDF8)
val CockpitAmber = Color(0xFFF59E0B)
val CockpitEmerald = Color(0xFF10B981)
val CockpitRose = Color(0xFFF43F5E)

private val DarkCockpitColorScheme = darkColorScheme(
    primary = CockpitElectricCyan,
    onPrimary = Color.Black,
    secondary = CockpitEmerald,
    onSecondary = Color.Black,
    tertiary = CockpitAmber,
    background = CockpitObsidian,
    onBackground = Color.White,
    surface = CockpitSurface,
    onSurface = Color.White
)

private val LightCockpitColorScheme = darkColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    tertiary = Color(0xFFD97706),
    background = Color(0xFF0F172A),
    onBackground = Color.White,
    surface = Color(0xFF1E293B),
    onSurface = Color.White
)

@Composable
fun MotoNavTheme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (isDarkTheme) DarkCockpitColorScheme else LightCockpitColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CockpitTypography,
        content = content
    )
}
