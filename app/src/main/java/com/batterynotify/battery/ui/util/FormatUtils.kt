package com.batterynotify.battery.ui.util

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

private val locale: Locale get() = Locale.getDefault()

fun Instant.formatLocalTime(zone: TimeZone = TimeZone.currentSystemDefault(), withSeconds: Boolean = false): String {
    val time = toLocalDateTime(zone)
    return if (withSeconds) {
        String.format(locale, "%02d:%02d:%02d", time.hour, time.minute, time.second)
    } else {
        String.format(locale, "%02d:%02d", time.hour, time.minute)
    }
}

fun formatLocalRange(start: Instant, end: Instant, zone: TimeZone = TimeZone.currentSystemDefault()): String {
    return "${start.formatLocalTime(zone)} – ${end.formatLocalTime(zone)}"
}

fun Long.formatShort(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    return if (hours > 0) {
        if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
    } else {
        "${minutes}m"
    }
}

fun Long.formatCountdown(): String {
    val totalSeconds = (this / 1000L).coerceAtLeast(0L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return String.format(locale, "%02d:%02d:%02d", hours, minutes, seconds)
}

fun Int.formatPercentage(): String = "$this%"
