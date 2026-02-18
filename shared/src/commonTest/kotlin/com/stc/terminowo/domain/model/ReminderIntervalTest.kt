package com.stc.terminowo.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReminderIntervalTest {

    @Test
    fun `fromDays returns correct interval`() {
        assertEquals(ReminderInterval.FOURTEEN_DAYS, ReminderInterval.fromDays(14))
        assertEquals(ReminderInterval.SEVEN_DAYS, ReminderInterval.fromDays(7))
        assertEquals(ReminderInterval.ONE_DAY, ReminderInterval.fromDays(1))
        assertEquals(ReminderInterval.DAY_OF, ReminderInterval.fromDays(0))
    }

    @Test
    fun `fromDays returns null for unknown interval`() {
        assertNull(ReminderInterval.fromDays(90))
        assertNull(ReminderInterval.fromDays(30))
        assertNull(ReminderInterval.fromDays(15))
        assertNull(ReminderInterval.fromDays(-1))
    }

    @Test
    fun `default intervals are correct`() {
        val defaults = ReminderInterval.DEFAULT
        assertEquals(4, defaults.size)
        assertEquals(listOf(14, 7, 1, 0), defaults.map { it.days })
    }
}
