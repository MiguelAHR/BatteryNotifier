package com.batterynotify.battery.data.battery

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleOwner
import androidx.core.app.NotificationCompat
import com.batterynotify.battery.data.alarm.BatteryThresholdEngine
import com.batterynotify.battery.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano para monitorización continua de batería.
 * Mantiene el receptor de batería activo incluso cuando la app está en background
 * y alimenta el motor de umbrales con cada lectura del sistema.
 */
class BatteryMonitoringService : Service(), LifecycleOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private var batteryManager: BatteryManagerWrapper? = null
    private var thresholdEngine: BatteryThresholdEngine? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)

        // Pasar a foreground de inmediato (obligatorio al iniciar
        // con startForegroundService, si no el sistema mata el servicio)
        startForeground(SERVICE_NOTIFICATION_ID, foregroundNotification())

        batteryManager = BatteryManagerWrapper(this)
        thresholdEngine = BatteryThresholdEngine(this, SettingsRepository(this))

        // Iniciar monitorización con el lifecycle del servicio
        batteryManager?.startMonitoring(this)

        // Obtener nivel actual y procesar
        val initialInfo = batteryManager?.getCurrentBatteryLevel()
        initialInfo?.let { info ->
            if (info.percentage >= 0) {
                thresholdEngine?.onBatteryUpdate(info.percentage, info.isCharging)
            }
        }

        // Alimentar el motor con cada lectura posterior del sistema
        scope.launch {
            batteryManager?.batteryLevel?.collect { info ->
                if (info != null && info.percentage >= 0) {
                    thresholdEngine?.onBatteryUpdate(info.percentage, info.isCharging)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Mantener servicio vivo: si el sistema lo mata, lo recrea
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        batteryManager?.stopMonitoring()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    private fun foregroundNotification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                SERVICE_CHANNEL_ID,
                getString(com.batterynotify.battery.R.string.service_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(com.batterynotify.battery.R.string.service_monitoring)
                setShowBadge(false)
                setSound(null, null)
            }
            manager?.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, SERVICE_CHANNEL_ID)
            .setSmallIcon(com.batterynotify.battery.R.drawable.ic_notification)
            .setContentTitle(getString(com.batterynotify.battery.R.string.service_title))
            .setContentText(getString(com.batterynotify.battery.R.string.service_monitoring))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    companion object {
        private const val SERVICE_CHANNEL_ID = "batterynotify.service"
        private const val SERVICE_NOTIFICATION_ID = 1001

        /** Crea una notificación mínima para el servicio en primer plano. */
        fun createForegroundNotification(context: Context): Notification {
            return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
                .setSmallIcon(com.batterynotify.battery.R.drawable.ic_notification)
                .setContentTitle(context.getString(com.batterynotify.battery.R.string.service_title))
                .setContentText(context.getString(com.batterynotify.battery.R.string.service_monitoring))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setOngoing(true)
                .setShowWhen(false)
                .build()
        }
    }
}
