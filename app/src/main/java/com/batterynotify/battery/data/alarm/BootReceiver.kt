package com.batterynotify.battery.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.batterynotify.battery.data.battery.BatteryManagerWrapper
import com.batterynotify.battery.data.notification.Notifications
import com.batterynotify.battery.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reagenda las alarmas de umbrales de batería tras reinicio, cambio de hora o zona horaria,
 * o cuando cambia el estado de alimentación.
 *
 * BOOT_COMPLETED y MY_PACKAGE_REPLACED solo se procesan si el usuario activó
 * [autoStartOnBoot][com.batterynotify.battery.domain.model.BatterySettings.autoStartOnBoot].
 * TIME_SET y TIMEZONE_CHANGED siempre se procesan (crítico para alarmas exactas).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val pendingResult = goAsync()
                val appContext = context.applicationContext

                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        val settings = SettingsRepository(appContext).settings.first()
                        // Solo reanudar si el usuario lo autorizó
                        if (!settings.autoStartOnBoot || !settings.notificationsEnabled) return@launch

                        // Revisión periódica (segura en segundo plano tras reinicio)
                        try {
                            com.batterynotify.battery.data.work.BatteryCheckWorker
                                .enqueuePeriodic(appContext)
                        } catch (_: Exception) {
                        }

                        val batteryManager = BatteryManagerWrapper(appContext)
                        val batteryInfo = batteryManager.getCurrentBatteryLevel()
                        rescheduleThresholds(appContext, settings, batteryInfo)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED -> {
                val pendingResult = goAsync()
                val appContext = context.applicationContext

                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        val settings = SettingsRepository(appContext).settings.first()
                        if (!settings.notificationsEnabled) return@launch

                        val batteryManager = BatteryManagerWrapper(appContext)
                        val batteryInfo = batteryManager.getCurrentBatteryLevel()
                        rescheduleThresholds(appContext, settings, batteryInfo)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            Intent.ACTION_POWER_CONNECTED -> {
                // Al conectar el cargador se cancelan las alertas continuas (Invasiva 3)
                Notifications.cancelContinuousAlerts(context.applicationContext)
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                // Al desconectar no se hace nada especial: el monitoreo normal
                // detectará los cruces de umbrales vía BatteryThresholdEngine
            }
        }
    }

    private fun rescheduleThresholds(
        context: Context,
        settings: com.batterynotify.battery.domain.model.BatterySettings,
        batteryInfo: com.batterynotify.battery.data.battery.BatteryInfo
    ) {
        // Para batería, lo más fiable es confiar en el BroadcastReceiver de ACTION_BATTERY_CHANGED
        // que se disparará automáticamente al arrancar.
        // Aquí solo nos aseguramos de que las notificaciones ongoing estén visibles si procede.

        if (settings.ongoingEnabled) {
            val level = toBatteryLevel(batteryInfo)
            Notifications.showOngoing(context, level, settings)
        }
    }

    private fun toBatteryLevel(info: com.batterynotify.battery.data.battery.BatteryInfo): com.batterynotify.battery.domain.model.BatteryLevel {
        val style = com.batterynotify.battery.domain.model.LevelStyle.fromPercentage(info.percentage)
        return com.batterynotify.battery.domain.model.BatteryLevel(
            percentage = info.percentage,
            isCharging = info.isCharging,
            timeRemainingMs = info.timeRemainingMs,
            levelStyle = style
        )
    }
}
