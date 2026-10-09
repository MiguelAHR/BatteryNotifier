package com.batterynotify.battery.data.battery

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LifecycleOwner
import androidx.core.app.NotificationCompat
import com.batterynotify.battery.data.alarm.BatteryThresholdEngine
import com.batterynotify.battery.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.time.ExperimentalTime

/**
 * Servicio en primer plano para monitorización continua de batería.
 * Mantiene el receptor de batería activo incluso cuando la app está en background.
 */
@ExperimentalTime
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
        
        batteryManager = BatteryManagerWrapper(this)
        thresholdEngine = BatteryThresholdEngine(this, SettingsRepository(this))
        
        // Iniciar monitorización con el lifecycle del servicio
        batteryManager?.startMonitoring(this)
        
        // Obtener nivel actual y procesar
        val initialInfo = batteryManager?.getCurrentBatteryLevel()
        initialInfo?.let { info ->
            thresholdEngine?.onBatteryUpdate(info.percentage, info.isCharging)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Mantener servicio vivo
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        batteryManager?.stopMonitoring()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    /** Crea una notificación mínima para el servicio en primer plano. */
    companion object {
        fun createForegroundNotification(context: Context): Notification {
            return NotificationCompat.Builder(context, "batterynotify.service")
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