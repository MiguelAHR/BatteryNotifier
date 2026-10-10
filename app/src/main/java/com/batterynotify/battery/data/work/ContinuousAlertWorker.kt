package com.batterynotify.battery.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.batterynotify.battery.data.battery.BatteryManagerWrapper
import com.batterynotify.battery.data.notification.Notifications
import com.batterynotify.battery.data.settings.SettingsRepository
import com.batterynotify.battery.domain.model.AlertType
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Revisa periódicamente un umbral Invasivo 3 y actualiza su
 * notificación continua. Se detiene solo cuando:
 * - el dispositivo empieza a cargar,
 * - el nivel sube por encima del umbral,
 * - el umbral se deshabilita o cambia de tipo.
 *
 * Cada ejecución exitosa reprograma la siguiente (cadena de
 * OneTimeWorkRequest) para no depender de trabajos periódicos
 * y respetar Doze de forma eficiente.
 */
class ContinuousAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val threshold = inputData.getInt(KEY_THRESHOLD, -1)
        if (threshold < 0) return Result.failure()

        val appContext = applicationContext
        val settings = SettingsRepository(appContext).settings.first()
        val config = settings.configFor(threshold) ?: return Result.success()

        if (!settings.notificationsEnabled || !config.enabled) {
            Notifications.cancelContinuousLevel(appContext, threshold)
            return Result.success()
        }
        if (config.alertType != AlertType.INVASIVE_3) {
            Notifications.cancelContinuousLevel(appContext, threshold)
            return Result.success()
        }

        val battery = BatteryManagerWrapper(appContext).getCurrentBatteryLevel()

        // Al conectar el cargador se detiene la cadena
        if (battery.isCharging || battery.isPlugged) {
            Notifications.cancelContinuousAlerts(appContext)
            return Result.success()
        }

        // Si subió por encima del umbral, se detiene
        if (battery.percentage < 0 || battery.percentage > threshold) {
            Notifications.cancelContinuousLevel(appContext, threshold)
            return Result.success()
        }

        // Actualiza la notificación continua con el % actual
        Notifications.showContinuousLevel(appContext, threshold, battery.percentage)

        // Reprograma la siguiente revisión
        enqueue(appContext, threshold, CHECK_INTERVAL_MINUTES)

        return Result.success()
    }

    companion object {
        const val KEY_THRESHOLD = "threshold"
        const val CHECK_INTERVAL_MINUTES = 15L

        private fun workName(threshold: Int) = "continuous_alert_$threshold"

        /** Programa (o reemplaza) la cadena de revisiones para un umbral. */
        fun enqueue(context: Context, threshold: Int, delayMinutes: Long = 0L) {
            val input = Data.Builder()
                .putInt(KEY_THRESHOLD, threshold)
                .build()
            val request = OneTimeWorkRequestBuilder<ContinuousAlertWorker>()
                .setInputData(input)
                .apply {
                    if (delayMinutes > 0) {
                        setInitialDelay(delayMinutes, TimeUnit.MINUTES)
                    }
                }
                .addTag(workName(threshold))
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                workName(threshold),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }

        /** Cancela la cadena de revisiones de un umbral. */
        fun cancel(context: Context, threshold: Int) {
            WorkManager.getInstance(context).cancelUniqueWork(workName(threshold))
        }

        /** Cancela todas las cadenas de alertas continuas. */
        fun cancelAll(context: Context) {
            // Nota: los tags reales son "continuous_alert_<umbral>", por eso
            // se cancela uno por uno en vez de por tag genérico.
            for (threshold in com.batterynotify.battery.domain.model.LevelStyle.ALL_THRESHOLDS) {
                cancel(context, threshold)
            }
        }
    }
}
