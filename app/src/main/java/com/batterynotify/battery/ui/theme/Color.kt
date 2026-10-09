package com.batterynotify.battery.ui.theme

import androidx.compose.ui.graphics.Color

// Brand colors (DeepSeek-inspired)
val BrandBlue = Color(0xFF4D6BFE)
val BrandBlueDark = Color(0xFF2B3FB0)

// Neon battery status colors
val NeonGreen = Color(0xFF00FFA3)      // 30%+ - Normal
val NeonGreenGlow = Color(0xFF00E5FF)
val NeonYellow = Color(0xFFFFFF00)     // 20-29% - Advertencia
val NeonYellowGlow = Color(0xFFFFAA00)
val NeonOrange = Color(0xFFFF8800)     // Transición warning/critical
val NeonRed = Color(0xFFFF2D78)        // 10-19% - Crítico
val NeonRedGlow = Color(0xFFFF6B4D)
val NeonDarkRed = Color(0xFFCC0000)    // <5% - Emergencia

// Surfaces (dark theme)
val BgTop = Color(0xFF0A0E1A)
val BgBottom = Color(0xFF111633)
val SurfaceDark = Color(0xFF121826)
val SurfaceVariantDark = Color(0xFF1B2236)

// Content colors
val OnSurfaceLight = Color(0xFFE6E9F5)
val OnSurfaceVariantLight = Color(0xFF8A93B2)

// Semantic colors for battery levels
val BatteryGreen = NeonGreen
val BatteryYellow = NeonYellow
val BatteryOrange = NeonOrange
val BatteryRed = NeonRed
val BatteryDarkRed = NeonDarkRed