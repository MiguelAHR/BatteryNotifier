package com.batterynotify.battery.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val BatteryColorScheme = darkColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueDark,
    onPrimaryContainer = Color.White,

    secondary = NeonGreen,
    onSecondary = BgTop,
    tertiary = NeonRed,
    onTertiary = Color.White,

    error = NeonRed,
    onError = Color.White,
    errorContainer = NeonDarkRed,
    onErrorContainer = Color.White,

    background = BgTop,
    onBackground = OnSurfaceLight,
    surface = SurfaceDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OnSurfaceVariantLight
)

@Composable
fun BatteryTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BatteryColorScheme,
        typography = BatteryTypography,
        content = content
    )
}