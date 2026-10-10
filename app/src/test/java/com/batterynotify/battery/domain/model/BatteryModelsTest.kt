package com.batterynotify.battery.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryModelsTest {

    @Test
    fun `fromPercentage mapea los 4 estilos correctamente`() {
        assertEquals(LevelStyle.GREEN, LevelStyle.fromPercentage(100))
        assertEquals(LevelStyle.GREEN, LevelStyle.fromPercentage(30))
        assertEquals(LevelStyle.WARNING, LevelStyle.fromPercentage(29))
        assertEquals(LevelStyle.WARNING, LevelStyle.fromPercentage(20))
        assertEquals(LevelStyle.CRITICAL, LevelStyle.fromPercentage(19))
        assertEquals(LevelStyle.CRITICAL, LevelStyle.fromPercentage(10))
        assertEquals(LevelStyle.EMERGENCY, LevelStyle.fromPercentage(9))
        assertEquals(LevelStyle.EMERGENCY, LevelStyle.fromPercentage(0))
    }

    @Test
    fun `ALL_THRESHOLDS contiene 30 20 10 5 en orden descendente`() {
        assertEquals(
            listOf(30, 20, 10, 5),
            LevelStyle.ALL_THRESHOLDS
        )
    }

    @Test
    fun `defaults crea una config por umbral con AlertType progresivo`() {
        val defaults = ThresholdConfig.defaults()

        assertEquals(4, defaults.size)
        assertEquals(AlertType.NORMAL, defaults.first { it.threshold == 30 }.alertType)
        assertEquals(AlertType.NORMAL, defaults.first { it.threshold == 20 }.alertType)
        assertEquals(AlertType.INVASIVE_1, defaults.first { it.threshold == 10 }.alertType)
        assertEquals(AlertType.INVASIVE_3, defaults.first { it.threshold == 5 }.alertType)
    }

    @Test
    fun `configFor e isThresholdEnabled funcionan`() {
        val settings = BatterySettings()

        assertTrue(settings.isThresholdEnabled(30))
        assertEquals(20, settings.configFor(20)?.threshold)
        assertEquals(null, settings.configFor(99))

        val disabled = settings.copy(
            thresholds = settings.thresholds.map {
                if (it.threshold == 20) it.copy(enabled = false) else it
            }
        )
        assertFalse(disabled.isThresholdEnabled(20))
        assertTrue(disabled.isThresholdEnabled(30))
    }

    @Test
    fun `BatteryLevel expone isCritical e isEmergency`() {
        val green = BatteryLevel(50, false, null, LevelStyle.GREEN)
        val critical = BatteryLevel(15, false, null, LevelStyle.CRITICAL)
        val emergency = BatteryLevel(3, false, null, LevelStyle.EMERGENCY)

        assertFalse(green.isCritical)
        assertFalse(green.isEmergency)
        assertTrue(critical.isCritical)
        assertFalse(critical.isEmergency)
        assertTrue(emergency.isCritical)
        assertTrue(emergency.isEmergency)
    }

    @Test
    fun `AlertType tiene nombre y descripcion no vacios`() {
        AlertType.entries.forEach { type ->
            assertTrue(type.displayName.isNotBlank())
            assertTrue(type.description.isNotBlank())
        }
    }
}
