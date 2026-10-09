package com.batterynotify.battery.domain.model

import kotlinx.datetime.Instant

/**
 * Representa el nivel de batería actual con su estilo visual asociado.
 */
data class BatteryLevel(
    val percentage: Int,           // 0-100
    val isCharging: Boolean,
    val timeRemainingMs: Long? = null,  // Tiempo estimado restante en milisegundos
    val levelStyle: LevelStyle
) {
    /** Indica si la batería está en un nivel crítico que requiere atención. */
    val isCritical: Boolean
        get() = levelStyle == LevelStyle.CRITICAL || levelStyle == LevelStyle.EMERGENCY

    /** Indica si la batería está en nivel de emergencia (< 5%). */
    val isEmergency: Boolean
        get() = levelStyle == LevelStyle.EMERGENCY

    /** Tiempo restante formateado (ej. "2h 30m"). */
    val timeRemainingFormatted: String
        get() = timeRemainingMs?.let { formatDuration(it) } ?: "Desconocido"

    private fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return if (hours > 0) {
            if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
        } else {
            "${minutes}m"
        }
    }
}

/**
 * Estilos visuales según el nivel de batería.
 * Cada estilo tiene un umbral mínimo y define color, prioridad de notificación, etc.
 */
enum class LevelStyle(val minPercentage: Int) {
    /** 30% o más - Normal, verde neón */
    GREEN(30),

    /** 20-29% - Advertencia, amarillo/naranja */
    WARNING(20),

    /** 10-19% - Crítico, rojo neón */
    CRITICAL(10),

    /** Menos de 5% - Emergencia, rojo parpadeante */
    EMERGENCY(5);

    /** Determina el estilo basado en el porcentaje actual. */
    companion object {
        fun fromPercentage(percentage: Int): LevelStyle {
            return when {
                percentage >= GREEN.minPercentage -> GREEN
                percentage >= WARNING.minPercentage -> WARNING
                percentage >= CRITICAL.minPercentage -> CRITICAL
                else -> EMERGENCY
            }
        }

        /** Todos los umbrales disponibles para configuración, ordenados de mayor a menor. */
        val ALL_THRESHOLDS = listOf(GREEN.minPercentage, WARNING.minPercentage, CRITICAL.minPercentage, EMERGENCY.minPercentage)
    }
}

/**
 * Tipo de alerta por umbral.
 */
enum class AlertType {
    /** Solo barra de estado (status bar) */
    NORMAL,

    /** Full-screen intent + sonido/vibración */
    INVASIVE_1,

    /** Invasive_1 + notificaciones continuas cada 1% hasta cargar */
    INVASIVE_3;

    val displayName: String
        get() = when (this) {
            NORMAL -> "Normal"
            INVASIVE_1 -> "Invasivo 1"
            INVASIVE_3 -> "Invasivo 3"
        }

    val description: String
        get() = when (this) {
            NORMAL -> "Solo aparece en la barra de notificaciones"
            INVASIVE_1 -> "Aparece en notificaciones y en medio de la pantalla con sonido y vibración"
            INVASIVE_3 -> "Igual que Invasivo 1 pero notificaciones continuas cada 1% hasta conectar cargador"
        }
}

/**
 * Configuración de notificación para un umbral específico.
 */
data class ThresholdConfig(
    /** Porcentaje que dispara esta notificación (ej. 30, 20, 10, 5) */
    val threshold: Int,

    /** Si esta notificación está habilitada */
    val enabled: Boolean = true,

    /** Tipo de alerta para este umbral */
    val alertType: AlertType = AlertType.NORMAL,

    /** Tipo de sonido para este umbral */
    val soundType: SoundType = SoundType.DEFAULT,

    /** URI de sonido personalizado (si soundType == CUSTOM) */
    val soundUri: String = "",

    /** Si debe vibrar */
    val vibrate: Boolean = true,

    /** Prioridad de la notificación (se deriva de alertType) */
    val priority: NotificationPriority = NotificationPriority.NORMAL,

    /** Si es una notificación persistente/ongoing (se deriva de alertType) */
    val ongoing: Boolean = false
) {
    /** Crea una configuración por defecto para un umbral dado. */
    companion object {
        fun defaults(): List<ThresholdConfig> = LevelStyle.ALL_THRESHOLDS.map { threshold ->
            ThresholdConfig(
                threshold = threshold,
                enabled = true,
                alertType = when (threshold) {
                    LevelStyle.GREEN.minPercentage -> AlertType.NORMAL
                    LevelStyle.WARNING.minPercentage -> AlertType.NORMAL
                    LevelStyle.CRITICAL.minPercentage -> AlertType.INVASIVE_1
                    LevelStyle.EMERGENCY.minPercentage -> AlertType.INVASIVE_3
                    else -> AlertType.NORMAL
                },
                soundType = when (threshold) {
                    LevelStyle.GREEN.minPercentage -> SoundType.SILENT
                    LevelStyle.WARNING.minPercentage -> SoundType.DEFAULT
                    LevelStyle.CRITICAL.minPercentage -> SoundType.DEFAULT
                    LevelStyle.EMERGENCY.minPercentage -> SoundType.DEFAULT
                    else -> SoundType.DEFAULT
                },
                vibrate = threshold <= LevelStyle.WARNING.minPercentage,
                priority = when (threshold) {
                    LevelStyle.GREEN.minPercentage -> NotificationPriority.LOW
                    LevelStyle.WARNING.minPercentage -> NotificationPriority.NORMAL
                    LevelStyle.CRITICAL.minPercentage -> NotificationPriority.HIGH
                    LevelStyle.EMERGENCY.minPercentage -> NotificationPriority.MAX
                    else -> NotificationPriority.NORMAL
                },
                ongoing = threshold <= LevelStyle.CRITICAL.minPercentage
            )
        }
    }
}

/**
 * Prioridad de notificación mapeada a NotificationCompat.
 */
enum class NotificationPriority {
    LOW,      // NotificationCompat.PRIORITY_LOW
    NORMAL,   // NotificationCompat.PRIORITY_DEFAULT
    HIGH,     // NotificationCompat.PRIORITY_HIGH
    MAX       // NotificationCompat.PRIORITY_MAX
}

/**
 * Tipos de sonido disponibles para notificaciones.
 */
enum class SoundType {
    /** Sonido por defecto del sistema */
    DEFAULT,

    /** Sin sonido */
    SILENT,

    /** Archivo de audio elegido por el usuario */
    CUSTOM
}

/**
 * Ajustes generales de la aplicación.
 */
data class BatterySettings(
    /** Si las notificaciones están habilitadas globalmente */
    val notificationsEnabled: Boolean = true,

    /** Configuración por umbral (ordenados de mayor a menor porcentaje) */
    val thresholds: List<ThresholdConfig> = ThresholdConfig.defaults(),

    /** Si mostrar notificación persistente con cuenta regresiva */
    val ongoingEnabled: Boolean = false,

    /** Si el servicio de monitoreo debe estar siempre activo */
    val alwaysMonitoring: Boolean = true,

    /** Si la app debe iniciarse automáticamente al arrancar el dispositivo */
    val autoStartOnBoot: Boolean = false
) {
    /** Obtiene la configuración para un umbral específico. */
    fun configFor(threshold: Int): ThresholdConfig? =
        thresholds.find { it.threshold == threshold }

    /** Verifica si un umbral específico está habilitado. */
    fun isThresholdEnabled(threshold: Int): Boolean =
        configFor(threshold)?.enabled == true
}