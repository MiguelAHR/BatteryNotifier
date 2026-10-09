package com.batterynotify.battery.data.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.batterynotify.battery.data.notification.Notifications
import com.batterynotify.battery.data.settings.SettingsRepository
import com.batterynotify.battery.domain.model.BatterySettings
import com.batterynotify.battery.domain.model.LevelStyle
import com.batterynotify.battery.domain.model.ThresholdConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Motor que detecta cruces de umbrales de batería y programa alarmas exactas.
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

    fun onBatteryUpdate(percentage: Int, isCharging: Boolean) {
        val settings = getSettingsSync()

        val currentActiveThresholds = settings.thresholds
            .filter { it.enabled }
            .map { it.threshold }
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
            settings.thresholds
                .filter { it.enabled }
                .map { it.threshold }
                .filter { lastKnownPercentage <= it && percentage > it }
        } else {
            emptyList()
        }

        for (threshold in newlyCrossedDown) {
            val config = settings.configFor(threshold) ?: continue
            scheduleExactNotification(threshold, config, percentage, isCharging)
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

    private fun scheduleExactNotification(
        threshold: Int,
        config: ThresholdConfig,
        currentPercentage: Int,
        isCharging: Boolean
    ) {
        if (isCharging) return

        val triggerAt = estimateThresholdTime(currentPercentage, threshold)

        val requestCode = REQUEST_THRESHOLD_BASE + threshold
        val action = "${BatteryThresholdReceiver.ACTION_THRESHOLD}.$threshold"

        setExactAlarm(
            triggerAt = triggerAt,
            requestCode = requestCode,
            action = action,
            threshold = threshold
        )
    }

    private fun estimateThresholdTime(currentPercentage: Int, targetThreshold: Int): Instant {
        val percentDiff = currentPercentage - targetThreshold
        val estimatedMinutes = (percentDiff * 12.0).toLong()
        val estimatedMs = estimatedMinutes * 60L * 1000L
        val delayMs = estimatedMs.coerceAtLeast(60_000L)
        return Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds() + delayMs)
    }

    private fun setExactAlarm(
        triggerAt: Instant,
        requestCode: Int,
        action: String,
        threshold: Int
    ) {
        alarmManager ?: return

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        val operation = pendingIntent(requestCode, action)
        val triggerAtMs = triggerAt.toEpochMilliseconds()

        try {
            if (canScheduleExact) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, operation)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, operation)
            }
        } catch (e: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMs, operation)
        }
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

    private fun getSettingsSync(): BatterySettings {
        return runBlocking(Dispatchers.IO) {
            settingsRepository.settings.first()
        }
    }

    companion object {
        private const val REQUEST_THRESHOLD_BASE = 4000
    }
}