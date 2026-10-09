package com.batterynotify.battery.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batterynotify.battery.data.settings.SettingsRepository
import com.batterynotify.battery.domain.model.BatterySettings
import com.batterynotify.battery.domain.model.NotificationPriority
import com.batterynotify.battery.domain.model.SoundType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn

data class SettingsUiState(
    val settings: BatterySettings = BatterySettings(),
    val currentPercentage: Int? = null
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val batteryManager: com.batterynotify.battery.data.battery.BatteryManagerWrapper
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state = _state

    init {
        // Combinar settings y battery level
        viewModelScope.launch {
            settingsRepository.settings
                .combine(batteryManager.batteryLevel) { settings, info ->
                    SettingsUiState(
                        settings = settings,
                        currentPercentage = info?.percentage
                    )
                }
                .distinctUntilChanged()
                .onEach { _state.value = it }
                .launchIn(viewModelScope)
        }
    }

    fun onNotificationsEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationsEnabled(enabled)
        }
    }

    fun onThresholdEnabledChanged(threshold: Int, enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setThresholdEnabled(threshold, enabled)
        }
    }

    fun onThresholdSoundChanged(threshold: Int, soundType: SoundType, soundUri: String) {
        viewModelScope.launch {
            settingsRepository.setThresholdSound(threshold, soundType, soundUri)
        }
    }

    fun onThresholdVibrateChanged(threshold: Int, vibrate: Boolean) {
        viewModelScope.launch {
            settingsRepository.setThresholdVibrate(threshold, vibrate)
        }
    }

    fun onThresholdPriorityChanged(threshold: Int, priority: NotificationPriority) {
        viewModelScope.launch {
            settingsRepository.setThresholdPriority(threshold, priority)
        }
    }

    fun onThresholdOngoingChanged(threshold: Int, ongoing: Boolean) {
        viewModelScope.launch {
            settingsRepository.setThresholdOngoing(threshold, ongoing)
        }
    }

    fun onThresholdAlertTypeChanged(threshold: Int, alertType: com.batterynotify.battery.domain.model.AlertType) {
        viewModelScope.launch {
            settingsRepository.setThresholdAlertType(threshold, alertType)
        }
    }

    fun onOngoingEnabledChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setOngoingEnabled(enabled)
        }
    }

    fun onAlwaysMonitoringChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAlwaysMonitoring(enabled)
        }
    }

    fun onAutoStartOnBootChanged(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoStartOnBoot(enabled)
        }
    }

    fun onTestClick(threshold: Int) {
        viewModelScope.launch {
            val settings = settingsRepository.settings.first()
            com.batterynotify.battery.data.notification.Notifications.showTest(
                context = batteryManager.context,
                settings = settings,
                threshold = threshold
            )
        }
    }

    fun onResetClick() {
        viewModelScope.launch {
            settingsRepository.resetToDefaults()
        }
    }
}