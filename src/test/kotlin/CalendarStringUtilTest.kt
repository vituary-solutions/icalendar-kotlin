package com.vituary.icalendar

import java.lang.IllegalArgumentException
import java.lang.IllegalStateException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CalendarStringUtilTest {
    @Test fun `splitCalendarValueToMap() should return an empty map when empty`() {
        val input = ""
        val expected = emptyMap<String, String>()

        val actual = input.splitCalendarValueToMap()

        assertEquals(expected, actual)
    }

    @Test fun `splitCalendarValueToMap() handles a single entry value`() {
        val input = "KEY=VALUE"
        val expected = mapOf("KEY" to "VALUE")

        val actual = input.splitCalendarValueToMap()

        assertEquals(expected, actual)
    }

    @Test fun `splitCalendarValueToMap() handles multiple values`() {
        val input = "KEY=VALUE;ANOTHER=ONE;Yet=again"
        val expected = mapOf("KEY" to "VALUE", "ANOTHER" to "ONE", "Yet" to "again")

        val actual = input.splitCalendarValueToMap()

        assertEquals(expected, actual)
    }

    @Test fun `splitCalendarValueToMap() should fail when '=' missing`() {
        val input = "KEY=VALUE;ANOTHER TWO"

        assertFailsWith<IllegalStateException> {
            input.splitCalendarValueToMap()
        }
    }

    @Test fun `splitCalendarValueToMap() should fail when duplicate keys present`() {
        val input = "KEY=ONE;KEY=TWO"

        assertFailsWith<IllegalStateException> {
            input.splitCalendarValueToMap()
        }
    }
}