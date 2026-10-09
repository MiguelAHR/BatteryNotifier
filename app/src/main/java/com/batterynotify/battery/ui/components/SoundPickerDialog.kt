package com.batterynotify.battery.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.batterynotify.battery.R
import com.batterynotify.battery.domain.model.SoundType

@Composable
fun SoundPickerDialog(
    currentType: SoundType,
    currentUri: String,
    onChange: (SoundType, String) -> Unit,
    onDismiss: () -> Unit = {}
) {
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
            SoundType.entries.forEach { type ->
                val isSelected = currentType == type
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    onClick = {
                        onChange(type, if (type == SoundType.CUSTOM) currentUri else "")
                    }
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
                                imageVector = when (type) {
                                    SoundType.DEFAULT -> Icons.Filled.MusicNote
                                    SoundType.SILENT -> Icons.Filled.VolumeOff
                                    SoundType.CUSTOM -> Icons.Filled.Folder
                                },
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            androidx.compose.foundation.layout.Spacer(
                                Modifier.padding(horizontal = 6.dp)
                            )
                            Column {
                                Text(
                                    text = soundTypeName(type),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (type == SoundType.CUSTOM && currentUri.isNotEmpty()) {
                                    Text(
                                        text = currentUri.takeLast(40),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        if (isSelected) {
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
    }
}

@Composable
private fun soundTypeName(type: SoundType): String = when (type) {
    SoundType.DEFAULT -> stringResource(R.string.sound_default)
    SoundType.SILENT -> stringResource(R.string.sound_silent)
    SoundType.CUSTOM -> stringResource(R.string.sound_custom)
}
