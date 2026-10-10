package com.batterynotify.battery.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batterynotify.battery.data.battery.BatteryManagerWrapper
import com.batterynotify.battery.data.battery.BatteryInfo
import com.batterynotify.battery.data.settings.SettingsRepository
import com.batterynotify.battery.domain.model.BatteryLevel
import com.batterynotify.battery.domain.model.BatterySettings
import com.batterynotify.battery.domain.model.LevelStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn

data class BatteryUiState(
    val level: BatteryLevel? = null,
    val settings: BatterySettings = BatterySettings(),
    val isMonitoring: Boolean = false
)

class BatteryViewModel(
    private val batteryManager: BatteryManagerWrapper,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BatteryUiState())
    val state = _state

    // Motor de detección de cruces de umbral. Cada actualización de
    // batería que llega del sistema pasa por aquí para disparar avisos.
    private val thresholdEngine = com.batterynotify.battery.data.alarm.BatteryThresholdEngine(
        batteryManager.context,
        settingsRepository
    )

    init {
        // Combinar battery level y settings
        viewModelScope.launch {
            batteryManager.batteryLevel
                .combine(settingsRepository.settings) { info, settings ->
                    val level = info?.let { toBatteryLevel(it) }
                    BatteryUiState(
                        level = level,
                        settings = settings,
                        isMonitoring = info != null
                    )
                }
                .distinctUntilChanged()
                .onEach { _state.value = it }
                .launchIn(viewModelScope)
        }

        // Alimentar el motor de umbrales con cada lectura del sistema.
        // Sin esto los cruces (ej. llegar al 20%) nunca se detectan.
        viewModelScope.launch {
            batteryManager.batteryLevel.collect { info ->
                if (info != null && info.percentage >= 0) {
                    thresholdEngine.onBatteryUpdate(
                        info.percentage,
                        info.isCharging,
                        info.timeRemainingMs
                    )
                }
            }
        }
    }

    /** Inicia la monitorización de batería. */
    fun startMonitoring(lifecycleOwner: androidx.lifecycle.LifecycleOwner) {
        batteryManager.startMonitoring(lifecycleOwner)
    }

    /** Detiene la monitorización. */
    fun stopMonitoring() {
        batteryManager.stopMonitoring()
    }

    /** Alterna las notificaciones globales. */
    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    /** Alterna un umbral específico. */
    fun toggleThreshold(threshold: Int, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setThresholdEnabled(threshold, enabled)
        }
    }

    /** Alterna notificación persistente. */
    fun toggleOngoing(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOngoingEnabled(enabled)
        }
    }

    /** Alterna monitorización continua. */
    fun toggleAlwaysMonitoring(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAlwaysMonitoring(enabled)
        }
    }

    /** Actualiza configuración de sonido para un umbral. */
    fun updateThresholdSound(threshold: Int, soundType: com.batterynotify.battery.domain.model.SoundType, soundUri: String = "") {
        viewModelScope.launch {
            settingsRepository.setThresholdSound(threshold, soundType, soundUri)
        }
    }

    /** Actualiza vibración para un umbral. */
    fun updateThresholdVibrate(threshold: Int, vibrate: Boolean) {
        viewModelScope.launch {
            settingsRepository.setThresholdVibrate(threshold, vibrate)
        }
    }

    /** Actualiza prioridad para un umbral. */
    fun updateThresholdPriority(threshold: Int, priority: com.batterynotify.battery.domain.model.NotificationPriority) {
        viewModelScope.launch {
            settingsRepository.setThresholdPriority(threshold, priority)
        }
    }

    /** Actualiza ongoing para un umbral. */
    fun updateThresholdOngoing(threshold: Int, ongoing: Boolean) {
        viewModelScope.launch {
            settingsRepository.setThresholdOngoing(threshold, ongoing)
        }
    }

    /** Muestra notificación de prueba para un umbral. */
    fun showTestNotification(threshold: Int) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            com.batterynotify.battery.data.notification.Notifications.showTest(
                context = batteryManager.context,
                settings = settings,
                threshold = threshold
            )
        }
    }

    /** Restablece ajustes a valores por defecto. */
    fun resetToDefaults() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
        }
    }

    private fun toBatteryLevel(info: BatteryInfo): BatteryLevel {
        val style = LevelStyle.fromPercentage(info.percentage)
        return BatteryLevel(
            percentage = info.percentage,
            isCharging = info.isCharging,
            timeRemainingMs = info.timeRemainingMs,
            levelStyle = style
        )
    }
}