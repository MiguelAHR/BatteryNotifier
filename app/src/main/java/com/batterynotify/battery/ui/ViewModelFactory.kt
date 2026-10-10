package com.batterynotify.battery.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.batterynotify.battery.data.battery.BatteryManagerWrapper
import com.batterynotify.battery.data.settings.SettingsRepository
import com.batterynotify.battery.ui.main.BatteryViewModel
import com.batterynotify.battery.ui.settings.SettingsViewModel

/**
 * Factories para los ViewModels con dependencias.
 *
 * Sin esto, `viewModel()` intenta usar un constructor sin argumentos
 * y la app se cierra al abrir la pantalla.
 */
class BatteryViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = context.applicationContext
        return BatteryViewModel(
            batteryManager = BatteryManagerWrapper(app),
            settingsRepository = SettingsRepository(app)
        ) as T
    }
}

class SettingsViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val app = context.applicationContext
        return SettingsViewModel(
            settingsRepository = SettingsRepository(app),
            batteryManager = BatteryManagerWrapper(app)
        ) as T
    }
}
