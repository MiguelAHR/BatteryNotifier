package com.batterynotify.battery.ui.main

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.RepeatMode
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batterynotify.battery.R
import com.batterynotify.battery.domain.model.BatteryLevel
import com.batterynotify.battery.domain.model.LevelStyle
import com.batterynotify.battery.ui.components.BatteryLevelChip
import com.batterynotify.battery.ui.components.NeonCard
import com.batterynotify.battery.ui.theme.BatteryTheme
import com.batterynotify.battery.ui.theme.BgBottom
import com.batterynotify.battery.ui.theme.BgTop
import com.batterynotify.battery.ui.theme.NeonGreen
import com.batterynotify.battery.ui.theme.NeonRed
import com.batterynotify.battery.ui.theme.NeonYellow
import com.batterynotify.battery.ui.theme.OnSurfaceLight
import com.batterynotify.battery.ui.theme.OnSurfaceVariantLight
import com.batterynotify.battery.ui.util.formatLocalTime
import com.batterynotify.battery.ui.util.formatShort
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime

@Composable
fun DashboardRoute(
    onOpenSettings: () -> Unit = {},
    zone: TimeZone = TimeZone.currentSystemDefault(),
    viewModel: BatteryViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.batterynotify.battery.ui.BatteryViewModelFactory(
            androidx.compose.ui.platform.LocalContext.current
        )
    )
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.LaunchedEffect(lifecycleOwner) {
        viewModel.startMonitoring(lifecycleOwner)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    DashboardScreen(
        state = state,
        zone = zone,
        onOpenSettings = onOpenSettings,
        onThresholdClick = { threshold -> /* Navegar a settings del umbral */ }
    )
}

@Composable
fun DashboardScreen(
    state: BatteryUiState,
    zone: TimeZone = TimeZone.currentSystemDefault(),
    onOpenSettings: () -> Unit = {},
    onThresholdClick: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val level = state.level
    val accent = level?.levelStyle?.let { colorForStyle(it) } ?: MaterialTheme.colorScheme.primary

    // Animación de pulso para el punto de estado
    val pulseAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1500,
                easing = androidx.compose.animation.core.LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(BgTop, BgBottom)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Header(state, zone, onOpenSettings)

            // Aviso de permisos faltantes (si los hay)
            PermissionsBanner()

            // Tarjeta principal de nivel de batería
            BatteryStatusCard(level, zone, pulseAlpha)

            // Próximo umbral
            NextThresholdCard(level, zone, state.settings)

            // Lista de umbrales configurados
            ThresholdsListCard(state)
        }
    }
}

@Composable
private fun Header(state: BatteryUiState, zone: TimeZone, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.brand_name),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.subtitle),
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceVariantLight
            )
        }
        Text(
            text = Clock.System.now().formatLocalTime(zone, withSeconds = true),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FontFamily.Monospace,
            color = OnSurfaceLight
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onOpenSettings) {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings_open),
                tint = OnSurfaceLight
            )
        }
    }
}

@Composable
private fun PermissionsBanner() {
    val context = androidx.compose.ui.platform.LocalContext.current
    // Se recalcula en cada recomposición para reflejar cambios hechos en Ajustes
    val notificationsOk =
        com.batterynotify.battery.ui.permissions.PermissionsHelper.areNotificationsGranted(context)
    val exactAlarmsOk =
        com.batterynotify.battery.ui.permissions.PermissionsHelper.canScheduleExactAlarms(context)
    val batteryOptOk =
        com.batterynotify.battery.ui.permissions.PermissionsHelper.isIgnoringBatteryOptimizations(context)

    if (notificationsOk && exactAlarmsOk && batteryOptOk) return

    NeonCard(accent = NeonRed) {
        Text(
            text = stringResource(R.string.permissions_missing_title),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = NeonRed
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.permissions_missing_desc),
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceVariantLight
        )
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!notificationsOk) {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        com.batterynotify.battery.ui.permissions.PermissionsHelper
                            .openAppNotificationSettings(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.grant_notifications))
                }
            }
            if (!exactAlarmsOk) {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        com.batterynotify.battery.ui.permissions.PermissionsHelper
                            .openExactAlarmSettings(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.grant_exact_alarms))
                }
            }
            if (!batteryOptOk) {
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        com.batterynotify.battery.ui.permissions.PermissionsHelper
                            .requestIgnoreBatteryOptimizations(context)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.grant_battery_whitelist))
                }
            }
        }
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
@Composable
private fun BatteryStatusCard(level: BatteryLevel?, zone: TimeZone, pulseAlpha: Float) {
    val accent = level?.levelStyle?.let { colorForStyle(it) } ?: MaterialTheme.colorScheme.primary
    val percentage = level?.percentage ?: 0
    val isCharging = level?.isCharging ?: false
    val timeRemainingMs = level?.timeRemainingMs

    NeonCard(accent = accent) {
        // Indicador de estado con pulso
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = pulseAlpha))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(if (isCharging) R.string.status_charging else R.string.status_discharging),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariantLight
            )
        }

        Spacer(Modifier.height(16.dp))

        // Chip de nivel de batería principal
        BatteryLevelChip(
            percentage = percentage,
            isCharging = isCharging,
            style = level?.levelStyle ?: LevelStyle.GREEN
        )

        Spacer(Modifier.height(16.dp))

        // Tiempo restante
        if (timeRemainingMs != null) {
            Row(horizontalArrangement = Arrangement.Center) {
                Icon(
                    imageVector = Icons.Filled.AccessTime,
                    tint = OnSurfaceVariantLight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.remaining_time, timeRemainingMs.formatShort()),
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = OnSurfaceLight
                )
            }
        } else {
            Text(
                text = stringResource(R.string.unknown_time),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceVariantLight
            )
        }

        Spacer(Modifier.height(22.dp))

        // Barra de progreso
        LinearProgressIndicator(
            progress = percentage / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = accent,
            trackColor = accent.copy(alpha = 0.2f)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.battery_level_label),
            style = MaterialTheme.typography.labelSmall,
            color = OnSurfaceVariantLight
        )
    }
}

@Composable
private fun NextThresholdCard(
    level: BatteryLevel?,
    zone: TimeZone,
    settings: com.batterynotify.battery.domain.model.BatterySettings
) {
    // Encontrar el siguiente umbral por debajo del actual
    val nextThreshold = level?.let { current ->
        settings.thresholds
            .filter { it.enabled && it.threshold < current.percentage }
            .maxByOrNull { it.threshold }
    }

    nextThreshold?.let { config ->
        val accent = colorForThreshold(config.threshold)

        NeonCard(accent = accent) {
            Text(
                text = stringResource(R.string.next_threshold),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = OnSurfaceVariantLight
            )
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${config.threshold}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.threshold_will_trigger),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceVariantLight
                )
            }

            Spacer(Modifier.height(12.dp))

            // Configuración del umbral
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                ThresholdConfigBadge(
                    label = stringResource(if (config.soundType == com.batterynotify.battery.domain.model.SoundType.SILENT) R.string.silent else R.string.with_sound),
                    icon = if (config.soundType == com.batterynotify.battery.domain.model.SoundType.SILENT)
                        Icons.Filled.VolumeOff
                        else Icons.Filled.VolumeUp,
                    color = accent
                )
                if (config.vibrate) {
                    ThresholdConfigBadge(
                        label = stringResource(R.string.vibrate),
                        icon = Icons.Filled.Vibration,
                        color = accent
                    )
                }
                if (config.ongoing) {
                    ThresholdConfigBadge(
                        label = stringResource(R.string.persistent),
                        icon = Icons.Filled.NotificationsActive,
                        color = accent
                    )
                }
            }
        }
    }
}

@Composable
private fun ThresholdConfigBadge(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun ThresholdsListCard(state: BatteryUiState) {
    val thresholds = state.settings.thresholds.filter { it.enabled }.sortedByDescending { it.threshold }

    if (thresholds.isEmpty()) return

    NeonCard(accent = MaterialTheme.colorScheme.primary) {
        Text(
            text = stringResource(R.string.configured_thresholds),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = OnSurfaceVariantLight
        )
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            thresholds.forEach { config ->
                val accent = colorForThreshold(config.threshold)
                val isActive = state.level?.percentage != null && state.level!!.percentage <= config.threshold

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Indicador de color
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (isActive) accent else accent.copy(alpha = 0.3f))
                    )
                    Spacer(Modifier.width(12.dp))

                    // Info del umbral
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${config.threshold}% · ${levelNameForThreshold(config.threshold)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isActive) OnSurfaceLight else OnSurfaceVariantLight
                        )
                        Text(
                            text = thresholdConfigSummary(config),
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceVariantLight
                        )
                    }

                    // Estado activo/inactivo
                    if (isActive) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                    }
                }
            }
        }
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
private fun thresholdConfigSummary(config: com.batterynotify.battery.domain.model.ThresholdConfig): String {
    val parts = mutableListOf<String>()
    if (config.soundType != com.batterynotify.battery.domain.model.SoundType.SILENT) parts.add(stringResource(R.string.with_sound))
    if (config.vibrate) parts.add(stringResource(R.string.vibrate))
    if (config.ongoing) parts.add(stringResource(R.string.persistent))
    return parts.joinToString(" · ") { it }.ifEmpty { stringResource(R.string.silent_only) }
}

private fun colorForStyle(style: LevelStyle): Color = when (style) {
    LevelStyle.GREEN -> NeonGreen
    LevelStyle.WARNING -> NeonYellow
    LevelStyle.CRITICAL -> com.batterynotify.battery.ui.theme.NeonOrange
    LevelStyle.EMERGENCY -> NeonRed
}

private fun colorForThreshold(threshold: Int): Color = when {
    threshold >= LevelStyle.GREEN.minPercentage -> NeonGreen
    threshold >= LevelStyle.WARNING.minPercentage -> NeonYellow
    threshold >= LevelStyle.CRITICAL.minPercentage -> com.batterynotify.battery.ui.theme.NeonOrange
    else -> NeonRed
}