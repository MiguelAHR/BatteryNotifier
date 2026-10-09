package com.batterynotify.battery.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.batterynotify.battery.data.battery.BatteryManagerWrapper
import com.batterynotify.battery.data.notification.Notifications
import com.batterynotify.battery.data.settings.SettingsRepository
import com.batterynotify.battery.domain.model.BatterySettings
import com.batterynotify.battery.domain.model.LevelStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * Recibe las alarmas de umbrales de batería y muestra las notificaciones correspondientes.
 */
class BatteryThresholdReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        val action = intent?.action ?: return

        // Extraer el umbral del action: "batterynotify.action.THRESHOLD.20"
        val threshold = action.split(".").lastOrNull()?.toIntOrNull() ?: return

        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                val settings = SettingsRepository(appContext).settings.first()
                val batteryManager = BatteryManagerWrapper(appContext)
                val batteryInfo = batteryManager.getCurrentBatteryLevel()
                
                // Convertir BatteryInfo a BatteryLevel del dominio
                val level = toBatteryLevel(batteryInfo)
                
                val config = settings.configFor(threshold) ?: return@launch
                
                if (settings.notificationsEnabled && Notifications.areEnabled(appContext) && config.enabled) {
                    // Verificar que el nivel actual sigue estando por debajo del umbral
                    if (level.percentage <= threshold) {
                        Notifications.showThreshold(
                            context = appContext,
                            level = level,
                            config = config,
                            settings = settings
                        )
                    }
                }

                // Si la notificación es ongoing, actualizarla
                if (settings.ongoingEnabled && config.ongoing) {
                    Notifications.showOngoing(appContext, level, settings)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun toBatteryLevel(info: com.batterynotify.battery.data.battery.BatteryInfo): com.batterynotify.battery.domain.model.BatteryLevel {
        val style = LevelStyle.fromPercentage(info.percentage)
        return com.batterynotify.battery.domain.model.BatteryLevel(
            percentage = info.percentage,
            isCharging = info.isCharging,
            timeRemainingMs = info.timeRemainingMs,
            levelStyle = style
        )
    }

    companion object {
        const val ACTION_THRESHOLD = "com.batterynotify.battery.action.THRESHOLD"
    }
}