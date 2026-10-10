package com.batterynotify.battery.data.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.batterynotify.battery.MainActivity
import com.batterynotify.battery.R
import com.batterynotify.battery.data.alarm.BatteryThresholdReceiver
import com.batterynotify.battery.domain.model.BatteryLevel
import com.batterynotify.battery.domain.model.BatterySettings
import com.batterynotify.battery.domain.model.LevelStyle
import com.batterynotify.battery.domain.model.NotificationPriority
import com.batterynotify.battery.domain.model.SoundType
import com.batterynotify.battery.domain.model.ThresholdConfig
import com.batterynotify.battery.ui.util.formatShort
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

object Notifications {

    private const val THRESHOLD_ID_BASE = 3000
    const val ONGOING_ID = 3100
    const val TEST_ID = 3101
    const val CONTINUOUS_ID_BASE = 4000

    fun areEnabled(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            areSystemNotificationsEnabled(context)
        }

    private fun areSystemNotificationsEnabled(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)
            ?.areNotificationsEnabled() == true

    fun showThreshold(
        context: Context,
        level: BatteryLevel,
        config: ThresholdConfig,
        settings: BatterySettings
    ) {
        if (!settings.notificationsEnabled || !config.enabled) return
        if (!areEnabled(context)) return

        val channelId = NotificationChannels.channelIdFor(
            config.threshold,
            config.alertType,
            config.soundType,
            config.soundUri,
            config.vibrate,
            config.priority
        )
        NotificationChannels.createAll(context, settings.thresholds)

        val styleName = when (level.levelStyle) {
            LevelStyle.GREEN -> context.getString(R.string.level_green)
            LevelStyle.WARNING -> context.getString(R.string.level_warning)
            LevelStyle.CRITICAL -> context.getString(R.string.level_critical)
            LevelStyle.EMERGENCY -> context.getString(R.string.level_emergency)
        }

        val title = context.getString(R.string.notif_threshold_title, config.threshold, styleName)
        val body = context.getString(R.string.notif_threshold_body, level.percentage, level.timeRemainingMs?.formatShort() ?: context.getString(R.string.unknown_time))

        val priority = when (config.priority) {
            NotificationPriority.LOW -> NotificationCompat.PRIORITY_LOW
            NotificationPriority.NORMAL -> NotificationCompat.PRIORITY_DEFAULT
            NotificationPriority.HIGH -> NotificationCompat.PRIORITY_HIGH
            NotificationPriority.MAX -> NotificationCompat.PRIORITY_MAX
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(iconForStyle(level.levelStyle))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(priority)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(false)
            .setContentIntent(openAppIntent(context))

        when (config.alertType) {
            com.batterynotify.battery.domain.model.AlertType.NORMAL -> {
                builder.setPriority(priority)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
            }
            com.batterynotify.battery.domain.model.AlertType.INVASIVE_1 -> {
                builder.setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(fullScreenIntent(context, config.threshold), true)
            }
            com.batterynotify.battery.domain.model.AlertType.INVASIVE_3 -> {
                builder.setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(fullScreenIntent(context, config.threshold), true)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
            }
        }

        notify(context, THRESHOLD_ID_BASE + config.threshold, builder.build())

        if (config.alertType == com.batterynotify.battery.domain.model.AlertType.INVASIVE_3) {
            scheduleContinuousAlerts(context, config.threshold)
        }
    }

    fun showOngoing(context: Context, level: BatteryLevel, settings: BatterySettings) {
        if (!settings.notificationsEnabled || !settings.ongoingEnabled) return
        if (!areEnabled(context)) return

        NotificationChannels.createAll(context, settings.thresholds)

        val styleName = when (level.levelStyle) {
            LevelStyle.GREEN -> context.getString(R.string.level_green)
            LevelStyle.WARNING -> context.getString(R.string.level_warning)
            LevelStyle.CRITICAL -> context.getString(R.string.level_critical)
            LevelStyle.EMERGENCY -> context.getString(R.string.level_emergency)
        }

        val title = context.getString(R.string.notif_ongoing_title, styleName)
        val body = context.getString(
            R.string.notif_ongoing_text,
            level.percentage,
            level.timeRemainingMs?.formatShort() ?: context.getString(R.string.unknown_time)
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.ONGOING)
            .setSmallIcon(iconForStyle(level.levelStyle))
            .setContentTitle(title)
            .setContentText(body)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setUsesChronometer(true)
            .setChronometerCountDown(false)
            .setWhen(level.timeRemainingMs?.let { Clock.System.now().toEpochMilliseconds() + it } ?: System.currentTimeMillis())
            .setContentIntent(openAppIntent(context))
            .build()

        notify(context, ONGOING_ID, notification)
    }

    fun cancelOngoing(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(ONGOING_ID)
    }

    fun cancelThreshold(context: Context, threshold: Int) {
        context.getSystemService(NotificationManager::class.java)?.cancel(THRESHOLD_ID_BASE + threshold)
    }

    fun cancelAllThresholds(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        for (threshold in LevelStyle.ALL_THRESHOLDS) {
            manager.cancel(THRESHOLD_ID_BASE + threshold)
        }
    }

    fun showTest(context: Context, settings: BatterySettings, threshold: Int? = null) {
        if (!areEnabled(context)) return

        val config = threshold?.let { settings.configFor(it) }
            ?: settings.thresholds.firstOrNull { it.enabled && it.threshold <= LevelStyle.CRITICAL.minPercentage }
            ?: settings.thresholds.firstOrNull { it.enabled }
            ?: return

        val channelId = NotificationChannels.channelIdFor(
            config.threshold,
            config.alertType,
            config.soundType,
            config.soundUri,
            config.vibrate,
            config.priority
        )
        NotificationChannels.createAll(context, settings.thresholds)

        val styleName = when {
            config.threshold >= LevelStyle.GREEN.minPercentage -> context.getString(R.string.level_green)
            config.threshold >= LevelStyle.WARNING.minPercentage -> context.getString(R.string.level_warning)
            config.threshold >= LevelStyle.CRITICAL.minPercentage -> context.getString(R.string.level_critical)
            else -> context.getString(R.string.level_emergency)
        }

        val body = context.getString(
            R.string.notif_test_body,
            config.threshold,
            styleName,
            context.getString(if (config.soundType == SoundType.SILENT) {
                R.string.notif_test_silent
            } else {
                R.string.notif_test_with_sound
            })
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.notif_test_title))
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent(context))
            .build()

        notify(context, TEST_ID, notification)
    }

    private fun notify(context: Context, id: Int, notification: android.app.Notification) {
        context.getSystemService(NotificationManager::class.java)?.notify(id, notification)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun fullScreenIntent(context: Context, threshold: Int): PendingIntent {
        val intent = Intent(context, com.batterynotify.battery.ui.alert.FullScreenAlertActivity::class.java).apply {
            putExtra("THRESHOLD", threshold)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun iconForStyle(style: LevelStyle): Int = when (style) {
        LevelStyle.GREEN -> R.drawable.ic_battery_green
        LevelStyle.WARNING -> R.drawable.ic_battery_warning
        LevelStyle.CRITICAL -> R.drawable.ic_battery_critical
        LevelStyle.EMERGENCY -> R.drawable.ic_battery_emergency
    }

    /**
     * Muestra o actualiza la notificación continua de un umbral Invasivo 3
     * con el porcentaje actual. Es ongoing y solo alerta la primera vez.
     */
    fun showContinuousLevel(context: Context, threshold: Int, percentage: Int) {
        if (!areEnabled(context)) return

        val style = LevelStyle.fromPercentage(percentage)
        val styleName = when (style) {
            LevelStyle.GREEN -> context.getString(R.string.level_green)
            LevelStyle.WARNING -> context.getString(R.string.level_warning)
            LevelStyle.CRITICAL -> context.getString(R.string.level_critical)
            LevelStyle.EMERGENCY -> context.getString(R.string.level_emergency)
        }

        val title = context.getString(R.string.notif_threshold_title, threshold, styleName)
        val body = context.getString(R.string.notif_threshold_body, percentage, "")

        val notification = NotificationCompat.Builder(context, NotificationChannels.ONGOING)
            .setSmallIcon(iconForStyle(style))
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent(context))
            .build()

        notify(context, CONTINUOUS_ID_BASE + threshold, notification)
    }

    fun cancelContinuousLevel(context: Context, threshold: Int) {
        context.getSystemService(NotificationManager::class.java)
            ?.cancel(CONTINUOUS_ID_BASE + threshold)
    }

    /**
     * Programa la cadena de revisiones periódicas (WorkManager) para un
     * umbral Invasivo 3. La cadena se detiene sola al cargar, al subir
     * por encima del umbral o al deshabilitar el umbral.
     */
    fun scheduleContinuousAlerts(context: Context, threshold: Int) {
        com.batterynotify.battery.data.work.ContinuousAlertWorker.enqueue(
            context,
            threshold,
            com.batterynotify.battery.data.work.ContinuousAlertWorker.CHECK_INTERVAL_MINUTES
        )
    }

    fun cancelContinuousAlerts(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        for (threshold in LevelStyle.ALL_THRESHOLDS) {
            manager?.cancel(CONTINUOUS_ID_BASE + threshold)
            com.batterynotify.battery.data.work.ContinuousAlertWorker.cancel(context, threshold)
            if (alarmManager != null) {
                val intent = Intent(context, BatteryThresholdReceiver::class.java).apply {
                    action = "${BatteryThresholdReceiver.ACTION_THRESHOLD}.$threshold.CONTINUOUS"
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    CONTINUOUS_ID_BASE + threshold,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                alarmManager.cancel(pendingIntent)
            }
        }
    }
}
