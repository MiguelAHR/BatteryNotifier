package com.batterynotify.battery.data.alarm

/**
 * Evita notificaciones duplicadas del mismo umbral dentro de un ciclo
 * de descarga.
 *
 * El [BatteryThresholdEngine] del ViewModel y el del servicio en segundo
 * plano viven en el mismo proceso: sin este candado compartido, ambos
 * dispararían el mismo aviso. También cubre el caso de abrir la app con
 * la batería ya por debajo del umbral (sin "cruce" previo que detectar).
 *
 * Un umbral se rearma ([disarm]) cuando la batería vuelve a subir por
 * encima de él, quedando listo para el próximo ciclo de descarga.
 */
object ThresholdNotifyTracker {

    private val notified = mutableSetOf<Int>()

    /**
     * @return true solo la primera vez que se notifica un umbral en el
     * ciclo actual; false si ya se notificó (duplicado a ignorar).
     */
    @Synchronized
    fun shouldNotify(threshold: Int): Boolean = notified.add(threshold)

    /** Rearma un umbral para que pueda volver a avisar. */
    @Synchronized
    fun disarm(threshold: Int) {
        notified.remove(threshold)
    }

    /** Rearma todos los umbrales. */
    @Synchronized
    fun disarmAll() {
        notified.clear()
    }
}
