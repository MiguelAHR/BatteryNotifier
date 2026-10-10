package com.batterynotify.battery.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun `formatShort muestra minutos cuando hay menos de una hora`() {
        assertEquals("5m", (5L * 60L * 1000L).formatShort())
        assertEquals("0m", 0L.formatShort())
    }

    @Test
    fun `formatShort muestra horas y minutos`() {
        assertEquals("2h 30m", ((2L * 3600L + 30L * 60L) * 1000L).formatShort())
        assertEquals("2h", (2L * 3600L * 1000L).formatShort())
    }

    @Test
    fun `formatCountdown usa formato HH MM SS`() {
        assertEquals("02:30:15", ((2L * 3600L + 30L * 60L + 15L) * 1000L).formatCountdown())
        assertEquals("00:05:00", (5L * 60L * 1000L).formatCountdown())
    }

    @Test
    fun `formatPercentage agrega el simbolo de porcentaje`() {
        assertEquals("30%", 30.formatPercentage())
        assertEquals("100%", 100.formatPercentage())
    }
}
