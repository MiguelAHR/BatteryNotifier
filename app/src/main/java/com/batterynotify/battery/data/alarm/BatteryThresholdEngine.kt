package com.batterynotify.battery.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.batterynotify.battery.data.notification.Notifications
import com.batterynotify.battery.data.settings.SettingsRepository
import com.batterynotify.battery.domain.model.BatterySettings
import com.batterynotify.battery.domain.model.LevelStyle
import com.batterynotify.battery.domain.model.ThresholdConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Motor que detecta umbrales de batería ya superados y dispara sus avisos.
 */
class BatteryThresholdEngine(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    private val _currentLevel = MutableStateFlow<ThresholdState?>(null)
    val currentLevel = _currentLevel.asStateFlow()

    private var lastKnownPercentage = -1
    private var lastKnownCharging = false

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    data class ThresholdState(
        val percentage: Int,
        val isCharging: Boolean,
        val activeThresholds: List<Int>,
        val triggeredThresholds: List<Int>
    )

    /**
     * Suspend: lee los ajustes sin bloquear el hilo que llama
     * (antes usaba runBlocking y podía congelar la UI en cada broadcast).
     */
    suspend fun onBatteryUpdate(
        percentage: Int,
        isCharging: Boolean,
        timeRemainingMs: Long? = null
    ) {
        val settings = settingsRepository.settings.first()

        val enabledThresholds = settings.thresholds
            .filter { it.enabled }
            .map { it.threshold }

        val currentActiveThresholds = enabledThresholds
            .filter { percentage <= it }
            .sortedDescending()

        val newlyCrossedDown = if (lastKnownPercentage >= 0 && !lastKnownCharging && isCharging.not()) {
            currentActiveThresholds.filter { threshold ->
                lastKnownPercentage > threshold && percentage <= threshold
            }
        } else {
            emptyList()
        }

        val newlyCrossedUp = if (lastKnownPercentage >= 0 && isCharging && !lastKnownCharging) {
            enabledThresholds
                .filter { lastKnownPercentage <= it && percentage > it }
        } else {
            emptyList()
        }

        // Si las notificaciones están apagadas, rearmar todo para que al
        // reactivarlas vuelvan a avisar de los umbrales superados.
        if (!settings.notificationsEnabled) {
            ThresholdNotifyTracker.disarmAll()
        }

        // Detección por NIVEL (no solo por cruce): si la batería ya está
        // por debajo de un umbral habilitado y aún no se avisó en este
        // ciclo, se avisa. Cubre app abierta tarde, broadcasts perdidos,
        // saltos de porcentaje y reinicios del proceso.
        // El candado compartido evita duplicados entre ViewModel y servicio.
        if (settings.notificationsEnabled && !isCharging) {
            for (threshold in currentActiveThresholds) {
                val config = settings.configFor(threshold) ?: continue
                if (ThresholdNotifyTracker.shouldNotify(threshold)) {
                    notifyNow(config, settings, percentage, timeRemainingMs)
                } else if (config.alertType == com.batterynotify.battery.domain.model.AlertType.INVASIVE_3 &&
                    lastKnownPercentage >= 0 && percentage < lastKnownPercentage
                ) {
                    // Invasiva 3: actualiza la continua con cada 1% menos
                    Notifications.showContinuousLevel(context, threshold, percentage)
                }
            }
        }

        // Rearmar los umbrales que quedaron por encima (próximo ciclo)
        for (threshold in enabledThresholds) {
            if (percentage > threshold) {
                ThresholdNotifyTracker.disarm(threshold)
            }
        }
        // Los umbrales deshabilitados siempre se rearman
        for (cfg in settings.thresholds) {
            if (!cfg.enabled) ThresholdNotifyTracker.disarm(cfg.threshold)
        }

        for (threshold in newlyCrossedUp) {
            cancelThresholdAlarm(threshold)
            Notifications.cancelThreshold(context, threshold)
        }

        // Si está cargando, cancelar alertas continuas (Invasive 3)
        if (isCharging) {
            Notifications.cancelContinuousAlerts(context)
        }

        lastKnownPercentage = percentage
        lastKnownCharging = isCharging

        _currentLevel.value = ThresholdState(
            percentage = percentage,
            isCharging = isCharging,
            activeThresholds = currentActiveThresholds,
            triggeredThresholds = newlyCrossedDown
        )
    }

    /** Muestra la notificación del umbral en este instante. */
    private fun notifyNow(
        config: ThresholdConfig,
        settings: BatterySettings,
        percentage: Int,
        timeRemainingMs: Long? = null
    ) {
        if (!settings.notificationsEnabled || !config.enabled) return
        if (!Notifications.areEnabled(context)) return

        val level = com.batterynotify.battery.domain.model.BatteryLevel(
            percentage = percentage,
            isCharging = false,
            timeRemainingMs = timeRemainingMs,
            levelStyle = LevelStyle.fromPercentage(percentage)
        )
        Notifications.showThreshold(
            context = context,
            level = level,
            config = config,
            settings = settings
        )
    }

    fun cancelThresholdAlarm(threshold: Int) {
        alarmManager ?: return
        val requestCode = REQUEST_THRESHOLD_BASE + threshold
        val action = "${BatteryThresholdReceiver.ACTION_THRESHOLD}.$threshold"
        alarmManager.cancel(pendingIntent(requestCode, action))
    }

    fun cancelAllAlarms() {
        alarmManager ?: return
        for (threshold in LevelStyle.ALL_THRESHOLDS) {
            cancelThresholdAlarm(threshold)
        }
    }

    private fun pendingIntent(requestCode: Int, action: String): PendingIntent {
        val intent = Intent(context, BatteryThresholdReceiver::class.java).setAction(action)
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val REQUEST_THRESHOLD_BASE = 4000
    }
}