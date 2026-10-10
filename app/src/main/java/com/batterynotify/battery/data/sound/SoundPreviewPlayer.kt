package com.batterynotify.battery.data.sound

import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Reproductor único para vistas previas de tonos.
 *
 * Garantiza que nunca suenen dos previews a la vez y que
 * cada preview se detenga solo tras [previewMs] (defecto 4 s).
 * La UI observa [playingUri] para mostrar play/pausa.
 */
object SoundPreviewPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var ringtone: Ringtone? = null
    private var stopJob: Job? = null

    private val _playingUri = MutableStateFlow<String?>(null)
    val playingUri: StateFlow<String?> = _playingUri.asStateFlow()

    /** Reproduce el tono indicado, deteniendo cualquier preview anterior. */
    fun play(context: Context, uriString: String, previewMs: Long = 4_000L) {
        stop()
        try {
            val r = RingtoneManager.getRingtone(
                context.applicationContext,
                Uri.parse(uriString)
            ) ?: return
            ringtone = r
            _playingUri.value = uriString
            r.play()
            stopJob = scope.launch {
                delay(previewMs)
                if (_playingUri.value == uriString) stop()
            }
        } catch (_: Exception) {
            stop()
        }
    }

    /**
     * Alterna reproducción/pausa para una URI.
     * @return true si quedó sonando, false si se pausó.
     */
    fun toggle(context: Context, uriString: String, previewMs: Long = 4_000L): Boolean {
        if (_playingUri.value == uriString) {
            stop()
            return false
        }
        play(context, uriString, previewMs)
        return true
    }

    /** Detiene cualquier preview en curso. */
    fun stop() {
        stopJob?.cancel()
        stopJob = null
        try {
            if (ringtone?.isPlaying == true) ringtone?.stop()
        } catch (_: Exception) {
        }
        ringtone = null
        _playingUri.value = null
    }

    fun isPlaying(uriString: String): Boolean = _playingUri.value == uriString
}
