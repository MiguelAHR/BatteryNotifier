package com.batterynotify.battery.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batterynotify.battery.R
import com.batterynotify.battery.domain.model.LevelStyle
import com.batterynotify.battery.domain.model.NotificationPriority
import com.batterynotify.battery.domain.model.SoundType
import com.batterynotify.battery.domain.model.ThresholdConfig
import com.batterynotify.battery.ui.components.AlertTypePicker
import com.batterynotify.battery.ui.components.NeonCard
import com.batterynotify.battery.ui.components.SoundPickerDialog
import com.batterynotify.battery.ui.components.SettingsActionButton
import com.batterynotify.battery.ui.theme.BatteryTheme
import com.batterynotify.battery.ui.theme.BgBottom
import com.batterynotify.battery.ui.theme.BgTop
import com.batterynotify.battery.ui.theme.NeonGreen
import com.batterynotify.battery.ui.theme.NeonRed
import com.batterynotify.battery.ui.theme.NeonYellow
import com.batterynotify.battery.ui.theme.OnSurfaceLight
import com.batterynotify.battery.ui.theme.OnSurfaceVariantLight
import com.batterynotify.battery.ui.theme.SurfaceDark
import com.batterynotify.battery.ui.theme.SurfaceVariantDark
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.batterynotify.battery.ui.SettingsViewModelFactory(
            androidx.compose.ui.platform.LocalContext.current
        )
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    BatteryTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Header con botón volver
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.back),
                                tint = OnSurfaceLight
                            )
                        }
                        Text(
                            text = stringResource(R.string.settings_title),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = OnSurfaceLight
                        )
                        Spacer(Modifier.width(48.dp)) // Balance
                    }

                    // Master toggle
                    MasterToggleCard(state, viewModel)

                    // Lista de umbrales
                    ThresholdsSettingsCard(state, viewModel)

                    // Notificación persistente
                    OngoingNotificationCard(state, viewModel)

                    // Monitorización continua
                    AlwaysMonitoringCard(state, viewModel)

                    // Auto-arranque
                    AutoStartCard(state, viewModel)

                    // Accesos rápidos
                    QuickActionsCard()

                    // Botón reset
                    ResetButton(viewModel)
                }
            }
        }
    }
}

@Composable
private fun MasterToggleCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    NeonCard(accent = Color.Cyan) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.notifications_enabled),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                Text(
                    text = stringResource(R.string.notifications_enabled_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight
                )
            }
            Switch(
                checked = state.settings.notificationsEnabled,
                onCheckedChange = { viewModel.onNotificationsEnabledChanged(it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color.Cyan,
                    checkedTrackColor = Color.Cyan.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun ThresholdsSettingsCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    val thresholds = state.settings.thresholds.sortedByDescending { it.threshold }

    NeonCard(accent = MaterialTheme.colorScheme.primary) {
        Text(
            text = stringResource(R.string.thresholds_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceLight
        )
        Text(
            text = stringResource(R.string.thresholds_desc),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariantLight
        )
        Spacer(Modifier.height(16.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            thresholds.forEach { config ->
                ThresholdSettingsItem(
                    config = config,
                    isActive = state.currentPercentage != null && state.currentPercentage!! <= config.threshold,
                    onEnabledChange = { enabled -> viewModel.onThresholdEnabledChanged(config.threshold, enabled) },
                    onSoundChange = { soundType, uri -> viewModel.onThresholdSoundChanged(config.threshold, soundType, uri) },
                    onVibrateChange = { vibrate -> viewModel.onThresholdVibrateChanged(config.threshold, vibrate) },
                    onPriorityChange = { priority -> viewModel.onThresholdPriorityChanged(config.threshold, priority) },
                    onOngoingChange = { ongoing -> viewModel.onThresholdOngoingChanged(config.threshold, ongoing) },
                    onAlertTypeChange = { alertType -> viewModel.onThresholdAlertTypeChanged(config.threshold, alertType) },
                    onTestClick = { viewModel.onTestClick(config.threshold) }
                )
            }
        }
    }
}

@Composable
private fun ThresholdSettingsItem(
    config: ThresholdConfig,
    isActive: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onSoundChange: (SoundType, String) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onPriorityChange: (NotificationPriority) -> Unit,
    onOngoingChange: (Boolean) -> Unit,
    onAlertTypeChange: (com.batterynotify.battery.domain.model.AlertType) -> Unit,
    onTestClick: () -> Unit
) {
    val accent = colorForThreshold(config.threshold)
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header del umbral
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(if (config.enabled) accent else accent.copy(alpha = 0.3f))
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "${config.threshold}% · ${levelNameForThreshold(config.threshold)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (config.enabled) OnSurfaceLight else OnSurfaceVariantLight
                    )
                    if (isActive) {
                        Text(
                            text = stringResource(R.string.threshold_active),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                }
            }

            // Checkbox habilitado
            Checkbox(
                checked = config.enabled,
                onCheckedChange = onEnabledChange,
                colors = androidx.compose.material3.CheckboxDefaults.colors(
                    checkedColor = accent,
                    uncheckedColor = OnSurfaceVariantLight
                )
            )
        }

        // Configuración expandida
        AnimatedVisibility(visible = expanded && config.enabled) {
            Column(modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp)
                .padding(start = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Selector de tipo de alerta
                AlertTypePicker(
                    currentType = config.alertType,
                    onChange = onAlertTypeChange
                )

                // Selector de sonido
                SoundPickerDialog(
                    currentType = config.soundType,
                    currentUri = config.soundUri,
                    onChange = onSoundChange
                )

                // Vibración
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.vibrate),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceLight
                    )
                    Switch(
                        checked = config.vibrate,
                        onCheckedChange = onVibrateChange,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = accent,
                            checkedTrackColor = accent.copy(alpha = 0.5f)
                        )
                    )
                }

                // Prioridad - solo lectura (se deriva de alertType)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.priority)}: ${priorityLabel(config.priority)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceLight
                    )
                }

                // Ongoing - solo lectura (se deriva de alertType)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.persistent_notification),
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceLight
                    )
                    Switch(
                        checked = config.ongoing,
                        onCheckedChange = onOngoingChange,
                        colors = androidx.compose.material3.SwitchDefaults.colors(
                            checkedThumbColor = accent,
                            checkedTrackColor = accent.copy(alpha = 0.5f)
                        )
                    )
                }

                // Botón probar
                androidx.compose.material3.OutlinedButton(
                    onClick = onTestClick,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                        containerColor = SurfaceVariantDark,
                        contentColor = accent
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.VolumeUp,
                        contentDescription = null,
                        tint = accent
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.test_notification),
                        style = MaterialTheme.typography.labelLarge,
                        color = accent
                    )
                }
            }
        }

        // Botón expandir/colapsar
        IconButton(onClick = { expanded = !expanded }) {
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
                tint = OnSurfaceVariantLight
            )
        }
    }
}

@Composable
private fun OngoingNotificationCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    NeonCard(accent = Color.Cyan) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.ongoing_notification),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                Text(
                    text = stringResource(R.string.ongoing_notification_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight
                )
            }
            Switch(
                checked = state.settings.ongoingEnabled,
                onCheckedChange = { viewModel.onOngoingEnabledChanged(it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color.Cyan,
                    checkedTrackColor = Color.Cyan.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun AlwaysMonitoringCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    NeonCard(accent = Color.Cyan) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.always_monitoring),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                Text(
                    text = stringResource(R.string.always_monitoring_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight
                )
            }
            Switch(
                checked = state.settings.alwaysMonitoring,
                onCheckedChange = { viewModel.onAlwaysMonitoringChanged(it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color.Cyan,
                    checkedTrackColor = Color.Cyan.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun AutoStartCard(state: SettingsUiState, viewModel: SettingsViewModel) {
    NeonCard(accent = Color.Cyan) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.auto_start_on_boot),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = OnSurfaceLight
                )
                Text(
                    text = stringResource(R.string.auto_start_on_boot_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceVariantLight
                )
            }
            Switch(
                checked = state.settings.autoStartOnBoot,
                onCheckedChange = { viewModel.onAutoStartOnBootChanged(it) },
                colors = androidx.compose.material3.SwitchDefaults.colors(
                    checkedThumbColor = Color.Cyan,
                    checkedTrackColor = Color.Cyan.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun QuickActionsCard() {
    val packageName = LocalContext.current.packageName
    NeonCard(accent = MaterialTheme.colorScheme.primary) {
        Text(
            text = stringResource(R.string.quick_actions_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceLight
        )
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsActionButton(
                title = stringResource(R.string.notification_settings),
                description = stringResource(R.string.notification_settings_desc),
                icon = Icons.Filled.Notifications,
                intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, packageName)
                }
            )
            SettingsActionButton(
                title = stringResource(R.string.sound_settings),
                description = stringResource(R.string.sound_settings_desc),
                icon = Icons.Filled.VolumeUp,
                intent = android.content.Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
            )
            SettingsActionButton(
                title = stringResource(R.string.battery_saver_settings),
                description = stringResource(R.string.battery_saver_settings_desc),
                icon = Icons.Filled.BatterySaver,
                intent = android.content.Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS)
            )
            SettingsActionButton(
                title = stringResource(R.string.notification_access_settings),
                description = stringResource(R.string.notification_access_settings_desc),
                icon = Icons.Filled.NotificationImportant,
                intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            )
        }
    }
}

@Composable
private fun ResetButton(viewModel: SettingsViewModel) {
    TextButton(
        onClick = { viewModel.onResetClick() },
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            contentColor = NeonRed
        )
    ) {
        Text(
            text = stringResource(R.string.reset_defaults),
            style = MaterialTheme.typography.labelLarge,
            color = NeonRed
        )
    }
}

@Composable
private fun levelNameForThreshold(threshold: Int): String = when {
    threshold >= LevelStyle.GREEN.minPercentage -> stringResource(R.string.level_green)
    threshold >= LevelStyle.WARNING.minPercentage -> stringResource(R.string.level_warning)
    threshold >= LevelStyle.CRITICAL.minPercentage -> stringResource(R.string.level_critical)
    else -> stringResource(R.string.level_emergency)
}

@Composable
private fun soundTypeLabel(type: SoundType): String = when (type) {
    SoundType.DEFAULT -> stringResource(R.string.sound_default)
    SoundType.SILENT -> stringResource(R.string.sound_silent)
    SoundType.CUSTOM -> stringResource(R.string.sound_custom)
}

@Composable
private fun priorityLabel(priority: NotificationPriority): String = when (priority) {
    NotificationPriority.LOW -> stringResource(R.string.priority_low)
    NotificationPriority.NORMAL -> stringResource(R.string.priority_normal)
    NotificationPriority.HIGH -> stringResource(R.string.priority_high)
    NotificationPriority.MAX -> stringResource(R.string.priority_max)
}

private fun colorForThreshold(threshold: Int): Color = when {
    threshold >= LevelStyle.GREEN.minPercentage -> NeonGreen
    threshold >= LevelStyle.WARNING.minPercentage -> NeonYellow
    threshold >= LevelStyle.CRITICAL.minPercentage -> com.batterynotify.battery.ui.theme.NeonOrange
    else -> NeonRed
}