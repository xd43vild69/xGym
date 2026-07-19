package com.d13.xgym

import com.d13.xgym.ui.formatMs
import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatTest {
    @Test
    fun formatsSecondsAndMinutes() {
        assertEquals("0:00", formatMs(0))
        assertEquals("0:59", formatMs(59_999))
        assertEquals("1:00", formatMs(60_000))
        assertEquals("12:05", formatMs(725_000))
    }
}
