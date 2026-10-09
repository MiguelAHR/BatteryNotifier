package com.batterynotify.battery.data.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.batterynotify.battery.data.battery.BatteryManagerWrapper
import com.batterynotify.battery.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Reagenda las alarmas de umbrales de batería tras reinicio, cambio de hora o zona horaria,
 * o cuando cambia el estado de alimentación.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED -> {
                val pendingResult = goAsync()
                val appContext = context.applicationContext
                
                CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                    try {
                        val settings = SettingsRepository(appContext).settings.first()
                        val batteryManager = BatteryManagerWrapper(appContext)
                        val batteryInfo = batteryManager.getCurrentBatteryLevel()
                        
                        // Re-evaluar umbrales activos y reprogramar alarmas necesarias
                        rescheduleThresholds(appContext, settings, batteryInfo)
                    } finally {
                        pendingResult.finish()
                    }
                }
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
            com.batterynotify.battery.data.notification.Notifications.showOngoing(context, level, settings)
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