package com.batterynotify.battery.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.batterynotify.battery.domain.model.AlertType
import com.batterynotify.battery.ui.theme.NeonGreen
import com.batterynotify.battery.ui.theme.NeonRed
import com.batterynotify.battery.ui.theme.NeonYellow

@Composable
fun AlertTypePicker(
    currentType: AlertType,
    onChange: (AlertType) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = "${stringResource(R.string.alert_type)}: ${alertTypeName(currentType)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = alertTypeDescription(currentType),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        Column(
            Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AlertType.entries.forEach { type ->
                AlertTypeOption(
                    type = type,
                    isSelected = currentType == type,
                    onClick = { onChange(type) }
                )
            }
        }
    }
}

@Composable
private fun AlertTypeOption(
    type: AlertType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = when (type) {
        AlertType.NORMAL -> NeonGreen
        AlertType.INVASIVE_1 -> NeonYellow
        AlertType.INVASIVE_3 -> NeonRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onClick
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = alertTypeName(type),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = alertTypeDescription(type),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = color
                )
            }
        }
    }
}

@Composable
private fun alertTypeName(type: AlertType): String = when (type) {
    AlertType.NORMAL -> stringResource(R.string.alert_type_normal)
    AlertType.INVASIVE_1 -> stringResource(R.string.alert_type_invasive_1)
    AlertType.INVASIVE_3 -> stringResource(R.string.alert_type_invasive_3)
}

@Composable
private fun alertTypeDescription(type: AlertType): String = when (type) {
    AlertType.NORMAL -> stringResource(R.string.alert_normal_desc)
    AlertType.INVASIVE_1 -> stringResource(R.string.alert_invasive_1_desc)
    AlertType.INVASIVE_3 -> stringResource(R.string.alert_invasive_3_desc)
}
