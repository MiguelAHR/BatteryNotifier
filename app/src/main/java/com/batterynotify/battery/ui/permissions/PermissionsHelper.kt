package com.batterynotify.battery.ui.permissions

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Centraliza la gestión de permisos de la app:
 * - Notificaciones (runtime en API 33+)
 * - Alarmas exactas (API 31+)
 * - Pantalla completa / full-screen intent (API 34+)
 * - Exclusión de optimización de batería (Doze / ahorro)
 * - Accesos directos a los ajustes del sistema
 */
object PermissionsHelper {

    /** Permisos runtime obligatorios para la funcionalidad básica. */
    val requiredRuntimePermissions: List<String>
        get() = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

    fun areNotificationsGranted(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return context.getSystemService(NotificationManager::class.java)
            ?.areNotificationsEnabled() == true
    }

    /** Indica si el sistema permite programar alarmas exactas. */
    fun canScheduleExactAlarms(context: Context): Boolean {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            manager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    /**
     * Indica si la app está excluida de la optimización de batería.
     * Sin esto, en modo ahorro / Doze las alertas invasivas pueden
     * retrasarse varios minutos.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java)
            ?: return true
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    /** Abre el ajuste del sistema para permitir alarmas exactas. */
    fun openExactAlarmSettings(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Pide al sistema excluir a la app de la optimización de batería.
     * Abre el diálogo del sistema (requiere REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).
     */
    fun requestIgnoreBatteryOptimizations(context: Context) {
        try {
            if (isIgnoringBatteryOptimizations(context)) return
            context.startActivity(
                Intent(
                    Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
            openBatterySaverSettings(context)
        }
    }

    /** Abre los ajustes de notificaciones de la app. */
    fun openAppNotificationSettings(context: Context) {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
            } else {
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
            }
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    /** Abre los ajustes de sonido del sistema. */
    fun openSoundSettings(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_SOUND_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
        }
    }

    /** Abre los ajustes de ahorro de batería. */
    fun openBatterySaverSettings(context: Context) {
        try {
            context.startActivity(
                Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
        }
    }
}
