package com.batterynotify.battery.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.batterynotify.battery.data.alarm.BatteryThresholdEngine
import com.batterynotify.battery.data.battery.BatteryManagerWrapper
import com.batterynotify.battery.data.settings.SettingsRepository
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Red de seguridad: revisa el nivel de batería cada 15 minutos aunque
 * el proceso haya muerto o algún broadcast se haya perdido.
 *
 * Usa el mismo motor y el mismo candado anti-duplicados, así que jamás
 * repite un aviso ya mostrado en el ciclo actual.
 */
class BatteryCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val appContext = applicationContext
        return try {
            val settings = SettingsRepository(appContext).settings.first()
            if (!settings.notificationsEnabled) return Result.success()

            val battery = BatteryManagerWrapper(appContext).getCurrentBatteryLevel()
            if (battery.percentage < 0) return Result.success()

            BatteryThresholdEngine(appContext, SettingsRepository(appContext))
                .onBatteryUpdate(battery.percentage, battery.isCharging, battery.timeRemainingMs)

            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "battery_check"

        /** Programa (o mantiene) la revisión periódica cada 15 minutos. */
        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<BatteryCheckWorker>(
                15, TimeUnit.MINUTES
            )
                .addTag(WORK_NAME)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Cancela la revisión periódica (al desactivar el monitoreo). */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
