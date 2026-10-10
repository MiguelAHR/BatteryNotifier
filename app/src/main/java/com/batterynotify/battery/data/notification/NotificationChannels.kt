package com.batterynotify.battery.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.batterynotify.battery.R
import com.batterynotify.battery.domain.model.AlertType
import com.batterynotify.battery.domain.model.LevelStyle
import com.batterynotify.battery.domain.model.NotificationPriority
import com.batterynotify.battery.domain.model.SoundType

object NotificationChannels {

    /** Prefijo base para canales de umbrales de batería */
    private const val THRESHOLD_PREFIX = "batterynotify.threshold"

    /** Canal para notificación persistente/ongoing */
    const val ONGOING = "batterynotify.ongoing"

    /** Canal para notificaciones de prueba */
    const val TEST = "batterynotify.test"

    /**
     * Genera el ID del canal para un umbral específico y configuración de sonido.
     * Los canales son inmutables en Android, así que cambiar el sonido requiere un nuevo canal.
     */
    fun channelIdFor(threshold: Int, soundType: SoundType, soundUri: String): String {
        val soundKey = when (soundType) {
            SoundType.DEFAULT -> "default"
            SoundType.SILENT -> "silent"
            SoundType.CUSTOM -> "custom-" + soundUri.hashCode().toUInt().toString(16)
        }
        return "$THRESHOLD_PREFIX.$threshold.$soundKey"
    }

    /**
     * Genera el ID del canal incluyendo tipo de alerta, sonido, vibración
     * y prioridad.
     *
     * Los canales son inmutables en Android: si la vibración o la
     * prioridad no formaran parte del ID, cambiar esos ajustes en la
     * app no tendría ningún efecto real.
     */
    fun channelIdFor(
        threshold: Int,
        alertType: AlertType,
        soundType: SoundType,
        soundUri: String,
        vibrate: Boolean = true,
        priority: NotificationPriority = NotificationPriority.NORMAL
    ): String {
        val soundKey = when (soundType) {
            SoundType.DEFAULT -> "default"
            SoundType.SILENT -> "silent"
            SoundType.CUSTOM -> "custom-" + soundUri.hashCode().toUInt().toString(16)
        }
        val alertKey = when (alertType) {
            AlertType.NORMAL -> "n"
            AlertType.INVASIVE_1 -> "i1"
            AlertType.INVASIVE_3 -> "i3"
        }
        val vibKey = if (vibrate) "v1" else "v0"
        val priKey = when (priority) {
            NotificationPriority.LOW -> "p0"
            NotificationPriority.NORMAL -> "p1"
            NotificationPriority.HIGH -> "p2"
            NotificationPriority.MAX -> "p3"
        }
        return "$THRESHOLD_PREFIX.$threshold.$alertKey.$soundKey.$vibKey.$priKey"
    }

    /** Crea todos los canales necesarios para las notificaciones de batería. */
    fun createAll(context: Context, thresholdConfigs: List<com.batterynotify.battery.domain.model.ThresholdConfig>) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        // Canal para notificación persistente (ongoing)
        val ongoingChannel = NotificationChannel(
            ONGOING,
            context.getString(R.string.channel_ongoing_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_ongoing_desc)
            setShowBadge(false)
            setSound(null, null)
        }

        // Canal para notificaciones de prueba
        val testChannel = NotificationChannel(
            TEST,
            context.getString(R.string.channel_test_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_test_desc)
            enableVibration(true)
            setShowBadge(true)
        }

        // Canales para cada umbral configurado
        val thresholdChannels = thresholdConfigs.map { config ->
            val importance = when (config.alertType) {
                AlertType.NORMAL -> NotificationManager.IMPORTANCE_DEFAULT
                AlertType.INVASIVE_1 -> NotificationManager.IMPORTANCE_HIGH
                AlertType.INVASIVE_3 -> NotificationManager.IMPORTANCE_HIGH
            }.coerceAtLeast(
                when (config.priority) {
                    NotificationPriority.LOW -> NotificationManager.IMPORTANCE_LOW
                    NotificationPriority.NORMAL -> NotificationManager.IMPORTANCE_DEFAULT
                    NotificationPriority.HIGH -> NotificationManager.IMPORTANCE_HIGH
                    NotificationPriority.MAX -> NotificationManager.IMPORTANCE_HIGH
                }
            )

            NotificationChannel(
                channelIdFor(
                    config.threshold,
                    config.alertType,
                    config.soundType,
                    config.soundUri,
                    config.vibrate,
                    config.priority
                ),
                context.getString(R.string.channel_threshold_name, config.threshold),
                importance
            ).apply {
                description = context.getString(R.string.channel_threshold_desc, config.threshold)
                enableVibration(config.vibrate)
                setShowBadge(true)
                when (config.soundType) {
                    SoundType.DEFAULT -> Unit
                    SoundType.SILENT -> setSound(null, null)
                    SoundType.CUSTOM -> setSound(
                        Uri.parse(config.soundUri),
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build()
                    )
                }
            }
        }

        // Limpiar canales antiguos que ya no se usan
        val keepIds = thresholdChannels.map { it.id } + ONGOING + TEST
        manager.notificationChannels
            ?.filter { it.id.startsWith(THRESHOLD_PREFIX) && it.id !in keepIds }
            ?.forEach { manager.deleteNotificationChannel(it.id) }

        // Crear todos los canales
        manager.createNotificationChannels(listOf(ongoingChannel, testChannel) + thresholdChannels)
    }

    /** Crea solo el canal de prueba. */
    fun createTestChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        
        val testChannel = NotificationChannel(
            TEST,
            context.getString(R.string.channel_test_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_test_desc)
            enableVibration(true)
            setShowBadge(true)
        }
        
        manager.createNotificationChannel(testChannel)
    }
}