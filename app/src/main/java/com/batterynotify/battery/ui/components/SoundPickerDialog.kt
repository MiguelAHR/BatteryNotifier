package com.batterynotify.battery.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.batterynotify.battery.R
import com.batterynotify.battery.data.sound.SoundPickerHelper
import com.batterynotify.battery.data.sound.SoundPreviewPlayer
import com.batterynotify.battery.data.sound.SystemSound
import com.batterynotify.battery.domain.model.SoundType

@Composable
fun SoundPickerDialog(
    currentType: SoundType,
    currentUri: String,
    onChange: (SoundType, String) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val context = LocalContext.current
    var systemSounds by remember { mutableStateOf<List<SystemSound>>(emptyList()) }
    var loadingSounds by remember { mutableStateOf(false) }
    val playingUri by SoundPreviewPlayer.playingUri.collectAsStateWithLifecycle()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val persisted = SoundPickerHelper.persistSoundUri(context, uri)
            if (persisted != null) {
                SoundPickerHelper.playPreview(context, persisted)
                onChange(SoundType.CUSTOM, persisted)
            }
        }
    }

    LaunchedEffect(Unit) {
        loadingSounds = true
        systemSounds = SoundPickerHelper.getSystemSounds(context)
        loadingSounds = false
    }

    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.sound_type),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Opción: Por defecto
            SoundTypeOption(
                selected = currentType == SoundType.DEFAULT,
                icon = Icons.Filled.MusicNote,
                title = stringResource(R.string.sound_default),
                subtitle = null,
                onClick = { onChange(SoundType.DEFAULT, "") }
            )

            // Opción: Silencioso
            SoundTypeOption(
                selected = currentType == SoundType.SILENT,
                icon = Icons.Filled.VolumeOff,
                title = stringResource(R.string.sound_silent),
                subtitle = null,
                onClick = { onChange(SoundType.SILENT, "") }
            )

            // Opción: Archivo personalizado (abre SAF)
            SoundTypeOption(
                selected = currentType == SoundType.CUSTOM,
                icon = Icons.Filled.Folder,
                title = stringResource(R.string.sound_custom),
                subtitle = if (currentUri.isNotEmpty()) {
                    stringResource(R.string.selected_file, currentUri.takeLast(32))
                } else {
                    stringResource(R.string.select_custom_sound)
                },
                onClick = {
                    filePickerLauncher.launch(arrayOf("audio/*"))
                }
            )

            // Lista de tonos del sistema con vista previa
            if (systemSounds.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.system_sounds),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                systemSounds.take(15).forEach { sound ->
                    val isCurrentCustom = currentType == SoundType.CUSTOM && currentUri == sound.uri
                    val isPlayingThis = playingUri == sound.uri
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrentCustom) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                        onClick = {
                            // Tocar la fila solo SELECCIONA el tono (sin reproducir).
                            onChange(SoundType.CUSTOM, sound.uri)
                        }
                    ) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sound.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    SoundPreviewPlayer.toggle(context, sound.uri)
                                }
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThis) {
                                        Icons.Filled.Pause
                                    } else {
                                        Icons.Filled.PlayArrow
                                    },
                                    contentDescription = stringResource(
                                        if (isPlayingThis) {
                                            R.string.stop_preview
                                        } else {
                                            R.string.tap_to_preview
                                        }
                                    ),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isCurrentCustom) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // Botón Examinar explícito
            OutlinedButton(
                onClick = { filePickerLauncher.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.browse))
            }
        }
    }
}

@Composable
private fun SoundTypeOption(
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        onClick = onClick
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(12.dp))
                androidx.compose.foundation.layout.Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
