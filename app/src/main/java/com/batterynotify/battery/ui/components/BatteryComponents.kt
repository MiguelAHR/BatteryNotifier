package com.batterynotify.battery.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.batterynotify.battery.ui.theme.SurfaceDark

/** Tarjeta oscura con borde luminoso del color de acento. */
@Composable
fun NeonCard(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

/** Chip pequeño con estilo neón para mostrar umbrales. */
@Composable
fun NeonChip(
    label: String,
    accent: Color,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val alpha = if (enabled) 1f else 0.4f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(
                color = SurfaceDark.copy(alpha = alpha),
                shape = RoundedCornerShape(16.dp)
            )
            .border(
                width = 1.dp,
                color = accent.copy(alpha = 0.5f * alpha),
                shape = RoundedCornerShape(16.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = androidx.compose.material3.MaterialTheme.typography.labelLarge,
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
            if (onClick != null) {
                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    contentDescription = null
                )
            }
        }
    }
}

/** Componente para mostrar el nivel de batería con estilo visual. */
@Composable
fun BatteryLevelChip(
    percentage: Int,
    isCharging: Boolean,
    style: com.batterynotify.battery.domain.model.LevelStyle,
    modifier: Modifier = Modifier
) {
    val accent = colorForStyle(style)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        // Texto del porcentaje grande
        Text(
            text = "$percentage%",
            style = androidx.compose.material3.MaterialTheme.typography.displayLarge,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
            color = accent
        )

        // Indicador de carga
        if (isCharging) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = -20.dp, end = -20.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.FlashOn,
                    tint = accent,
                    contentDescription = "Cargando",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

private fun colorForStyle(style: com.batterynotify.battery.domain.model.LevelStyle): Color = when (style) {
    com.batterynotify.battery.domain.model.LevelStyle.GREEN -> com.batterynotify.battery.ui.theme.NeonGreen
    com.batterynotify.battery.domain.model.LevelStyle.WARNING -> com.batterynotify.battery.ui.theme.NeonYellow
    com.batterynotify.battery.domain.model.LevelStyle.CRITICAL -> com.batterynotify.battery.ui.theme.NeonOrange
    com.batterynotify.battery.domain.model.LevelStyle.EMERGENCY -> com.batterynotify.battery.ui.theme.NeonRed
}