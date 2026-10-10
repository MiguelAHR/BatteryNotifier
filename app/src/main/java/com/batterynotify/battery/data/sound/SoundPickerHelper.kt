package com.batterynotify.battery.data.sound

import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Carga los tonos de notificación del sistema y gestiona URIs
 * de archivos externos con permiso persistible (SAF).
 */
data class SystemSound(
    val title: String,
    val uri: String
)

object SoundPickerHelper {

    /** Lee los tonos de notificación del sistema (llamar desde Dispatchers.IO). */
    suspend fun getSystemSounds(context: Context): List<SystemSound> =
        withContext(Dispatchers.IO) {
            val sounds = mutableListOf<SystemSound>()
            try {
                val manager = RingtoneManager(context)
                manager.setType(RingtoneManager.TYPE_NOTIFICATION)
                val cursor = manager.cursor ?: return@withContext sounds
                cursor.use {
                    while (it.moveToNext()) {
                        val title = try {
                            it.getString(RingtoneManager.TITLE_COLUMN_INDEX) ?: "Tono"
                        } catch (_: Exception) {
                            "Tono"
                        }
                        val uri = manager.getRingtoneUri(it.position)
                        sounds.add(SystemSound(title, uri.toString()))
                    }
                }
            } catch (_: Exception) {
            }
            sounds
        }

    /**
     * Guarda una URI de documento con permiso de lectura persistible
     * para que siga disponible tras reinicios.
     *
     * @return la URI como String para guardar en DataStore, o null si falla.
     */
    fun persistSoundUri(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            uri.toString()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reproduce una vista previa corta del tono indicado.
     * Delega en [SoundPreviewPlayer]: detiene cualquier preview
     * anterior y se auto-detiene a los pocos segundos.
     */
    fun playPreview(context: Context, uriString: String) {
        SoundPreviewPlayer.play(context, uriString)
    }
}
