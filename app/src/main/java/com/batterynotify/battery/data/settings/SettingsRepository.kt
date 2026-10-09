package com.batterynotify.battery.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.batterynotify.battery.domain.model.BatterySettings
import com.batterynotify.battery.domain.model.NotificationPriority
import com.batterynotify.battery.domain.model.SoundType
import com.batterynotify.battery.domain.model.ThresholdConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore by preferencesDataStore(name = "battery_notifier_settings")

class SettingsRepository(private val context: Context) {

    val settings: Flow<BatterySettings> = context.dataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(emptyPreferences())
            else throw throwable
        }

        .map { prefs ->
            val thresholds = mutableListOf<ThresholdConfig>()

            for (threshold in com.batterynotify.battery.domain.model.LevelStyle.ALL_THRESHOLDS) {
                val enabledKey = booleanPreferencesKey("threshold_${threshold}_enabled")
                val soundTypeKey = stringPreferencesKey("threshold_${threshold}_sound_type")
                val soundUriKey = stringPreferencesKey("threshold_${threshold}_sound_uri")
                val vibrateKey = booleanPreferencesKey("threshold_${threshold}_vibrate")
                val priorityKey = stringPreferencesKey("threshold_${threshold}_priority")
                val ongoingKey = booleanPreferencesKey("threshold_${threshold}_ongoing")
                val alertTypeKey = stringPreferencesKey("threshold_${threshold}_alert_type")

                thresholds.add(ThresholdConfig(
                    threshold = threshold,
                    enabled = prefs[enabledKey] ?: true,
                    soundType = prefs[soundTypeKey]
                        ?.let { name -> SoundType.entries.firstOrNull { it.name == name } }
                        ?: SoundType.DEFAULT,
                    soundUri = prefs[soundUriKey] ?: "",
                    vibrate = prefs[vibrateKey] ?: (threshold <= com.batterynotify.battery.domain.model.LevelStyle.WARNING.minPercentage),
                    priority = prefs[priorityKey]
                        ?.let { name -> NotificationPriority.entries.firstOrNull { it.name == name } }
                        ?: when (threshold) {
                            com.batterynotify.battery.domain.model.LevelStyle.GREEN.minPercentage -> NotificationPriority.LOW
                            com.batterynotify.battery.domain.model.LevelStyle.WARNING.minPercentage -> NotificationPriority.NORMAL
                            com.batterynotify.battery.domain.model.LevelStyle.CRITICAL.minPercentage -> NotificationPriority.HIGH
                            com.batterynotify.battery.domain.model.LevelStyle.EMERGENCY.minPercentage -> NotificationPriority.MAX
                            else -> NotificationPriority.NORMAL
                        },
                    ongoing = prefs[ongoingKey] ?: (threshold <= com.batterynotify.battery.domain.model.LevelStyle.CRITICAL.minPercentage),
                    alertType = prefs[alertTypeKey]
                        ?.let { name -> com.batterynotify.battery.domain.model.AlertType.entries.firstOrNull { it.name == name } }
                        ?: when (threshold) {
                            com.batterynotify.battery.domain.model.LevelStyle.GREEN.minPercentage -> com.batterynotify.battery.domain.model.AlertType.NORMAL
                            com.batterynotify.battery.domain.model.LevelStyle.WARNING.minPercentage -> com.batterynotify.battery.domain.model.AlertType.NORMAL
                            com.batterynotify.battery.domain.model.LevelStyle.CRITICAL.minPercentage -> com.batterynotify.battery.domain.model.AlertType.INVASIVE_1
                            com.batterynotify.battery.domain.model.LevelStyle.EMERGENCY.minPercentage -> com.batterynotify.battery.domain.model.AlertType.INVASIVE_3
                            else -> com.batterynotify.battery.domain.model.AlertType.NORMAL
                        }
                ))
            }

            BatterySettings(
                notificationsEnabled = prefs[KEY_ENABLED] ?: true,
                thresholds = thresholds,
                ongoingEnabled = prefs[KEY_ONGOING] ?: false,
                alwaysMonitoring = prefs[KEY_ALWAYS_MONITORING] ?: true,
                autoStartOnBoot = prefs[KEY_AUTO_START] ?: false
            )
        }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ENABLED] = enabled }
    }

    suspend fun setThresholdEnabled(threshold: Int, enabled: Boolean) {
        context.dataStore.edit { it[booleanPreferencesKey("threshold_${threshold}_enabled")] = enabled }
    }

    suspend fun setThresholdSound(threshold: Int, soundType: SoundType, soundUri: String = "") {
        context.dataStore.edit {
            it[stringPreferencesKey("threshold_${threshold}_sound_type")] = soundType.name
            it[stringPreferencesKey("threshold_${threshold}_sound_uri")] = soundUri
        }
    }

    suspend fun setThresholdVibrate(threshold: Int, vibrate: Boolean) {
        context.dataStore.edit { it[booleanPreferencesKey("threshold_${threshold}_vibrate")] = vibrate }
    }

    suspend fun setThresholdPriority(threshold: Int, priority: NotificationPriority) {
        context.dataStore.edit { it[stringPreferencesKey("threshold_${threshold}_priority")] = priority.name }
    }

    suspend fun setThresholdOngoing(threshold: Int, ongoing: Boolean) {
        context.dataStore.edit { it[booleanPreferencesKey("threshold_${threshold}_ongoing")] = ongoing }
    }

    suspend fun setThresholdAlertType(threshold: Int, alertType: com.batterynotify.battery.domain.model.AlertType) {
        context.dataStore.edit { it[stringPreferencesKey("threshold_${threshold}_alert_type")] = alertType.name }
    }

    suspend fun setOngoingEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ONGOING] = enabled }
    }

    suspend fun setAlwaysMonitoring(enabled: Boolean) {
        context.dataStore.edit { it[KEY_ALWAYS_MONITORING] = enabled }
    }

    suspend fun setAutoStartOnBoot(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AUTO_START] = enabled }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("notifications_enabled")
        val KEY_ONGOING = booleanPreferencesKey("ongoing_enabled")
        val KEY_ALWAYS_MONITORING = booleanPreferencesKey("always_monitoring")
        val KEY_AUTO_START = booleanPreferencesKey("auto_start_on_boot")
    }
}