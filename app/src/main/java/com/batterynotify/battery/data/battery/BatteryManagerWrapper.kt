package com.batterynotify.battery.data.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Wrapper para obtener información de batería del sistema Android.
 * Expone el nivel de batería como un Flow reactivo.
 */
class BatteryManagerWrapper(val context: Context) {

    private val _batteryLevel = MutableStateFlow<BatteryInfo?>(null)
    val batteryLevel = _batteryLevel.asStateFlow()

    private val receiver = BatteryBroadcastReceiver()
    private var isRegistered = false

    /**
     * Inicia la monitorización de batería.
     * Debe llamarse desde un componente con lifecycle (Activity, Service).
     */
    fun startMonitoring(lifecycleOwner: LifecycleOwner) {
        if (isRegistered) return
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        
        // Registrar receptor para recibir sticky broadcast de batería.
        // RECEIVER_NOT_EXPORTED solo existe desde API 33: en versiones
        // anteriores se usa la sobrecarga sin flags.
        val stickyIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        if (stickyIntent != null) {
            receiver.onReceive(context, stickyIntent)
        }
        
        isRegistered = true
        
        // Observar lifecycle para limpiar al destruir
        lifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                stopMonitoring()
            }
        })
    }

    /** Detiene la monitorización. */
    fun stopMonitoring() {
        if (isRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // Ya estaba desregistrado
            }
            isRegistered = false
        }
    }

    /** Obtiene el nivel actual de batería de forma síncrona. */
    fun getCurrentBatteryLevel(): BatteryInfo {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val intent = context.registerReceiver(null, filter)
        return parseBatteryIntent(intent)
    }

    private inner class BatteryBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val info = parseBatteryIntent(intent)
            _batteryLevel.value = info
        }
    }

    private fun parseBatteryIntent(intent: Intent?): BatteryInfo {
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val percentage = if (level >= 0 && scale > 0) {
            (level * 100 / scale).coerceIn(0, 100)
        } else {
            -1
        }

        val isCharging = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN) 
            ?.let { status -> status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL } 
            ?: false

        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val isPlugged = plugged != 0

        // Estimar tiempo restante en milisegundos (aproximado)
        val timeRemainingMs = estimateTimeRemainingMs(percentage, isCharging)

        return BatteryInfo(
            percentage = percentage,
            isCharging = isCharging,
            isPlugged = isPlugged,
            timeRemainingMs = timeRemainingMs,
            timestamp = Clock.System.now()
        )
    }

    /** Estimación simple del tiempo restante en milisegundos. */
    private fun estimateTimeRemainingMs(percentage: Int, isCharging: Boolean): Long? {
        if (percentage < 0) return null
        // Estimación muy básica: ~1% por 10-15 min en uso normal
        // En carga: ~1% por 1-2 min
        val minutesPerPercent = if (isCharging) 1.5 else 12.0
        val totalMinutes = (percentage * minutesPerPercent).toLong()
        return totalMinutes * 60 * 1000 // Convertir a milisegundos
    }
}

/** Información de batería parseada del sistema. */
data class BatteryInfo(
    val percentage: Int,
    val isCharging: Boolean,
    val isPlugged: Boolean,
    val timeRemainingMs: Long?,
    val timestamp: Instant
)