package com.vituary.icalendar

import java.time.*
import java.time.format.DateTimeParseException
import java.util.*
import kotlin.test.*

class RecurrenceRulePropertyTest {
    @Test fun `can exist with only frequency`() {
        val property = RecurrenceRuleProperty("FREQ=DAILY")

        assertEquals(RecurrenceFrequency.DAILY, property.frequency)
        assertNull(property.count)
        assertNull(property.until)
    }

    @Test fun `frequency is required`() {
        assertFailsWith<IllegalArgumentException> {
            RecurrenceRuleProperty("COUNT=10")
        }
    }

    @Test fun `count is parsed from integer`() {
        val property = RecurrenceRuleProperty("FREQ=DAILY;COUNT=10")

        assertEquals(10, property.count)
    }

    @Test fun `count must be greater than 0`() {
        assertFailsWith<IllegalArgumentException> {
            RecurrenceRuleProperty("FREQ=MONTHLY;COUNT=0")
        }
    }

    @Test fun `until is parsed as UTC when time zone is present`() {
        val property = RecurrenceRuleProperty("FREQ=DAILY;UNTIL=20190819T134500Z")

        val expected = ZonedDateTime.of(2019, Month.AUGUST.value, 19, 13, 45, 0, 0, UTC_TZ)
        assertEquals(expected, property.until)
    }

    @Test fun `until and count may not both be specified`() {
        assertFailsWith<IllegalArgumentException> {
            RecurrenceRuleProperty("FREQ=DAILY;COUNT=10;UNTIL=20190819T134500Z")
        }
    }

    @Test fun `interval is parsed to an integer`() {
        val property = RecurrenceRuleProperty("FREQ=WEEKLY;INTERVAL=2")

        assertEquals(2, property.interval)
    }

    @Test fun `interval must be greater than 0`() {
        assertFailsWith<IllegalArgumentException> {
            RecurrenceRuleProperty("FREQ=MONTHLY;INTERVAL=0")
        }
    }
}

class DurationPropertyTest {
    @Test fun `duration handles weeks`() {
        val property = DurationProperty("P7W")

        assertEquals(Period.ofWeeks(7), property.duration)
    }

    @Test fun `duration handles days`() {
        val property = DurationProperty("P15D")

        assertEquals(Period.ofDays(15), property.duration)
    }

    @Test fun `duration handles hours`() {
        val property = DurationProperty("PT5H")

        assertEquals(Duration.ofHours(5), property.duration)
    }

    @Test fun `duration handles minutes`() {
        val property = DurationProperty("PT45M")

        assertEquals(Duration.ofMinutes(45), property.duration)
    }

    @Test fun `duration handles seconds`() {
        val property = DurationProperty("PT60S")

        assertEquals(Duration.ofSeconds(60), property.duration)
    }

    @Test fun `duration handles mixed day and time fields`() {
        val property = DurationProperty("P15DT5H30M20S")
        val expected = Duration.ZERO.plusDays(15).plusHours(5).plusMinutes(30).plusSeconds(20)

        assertEquals(expected, property.duration)
    }

    @Test fun `duration handles time only`() {
        val property = DurationProperty("PT1H0M0S")
        val expected = Duration.ofHours(1)

        assertEquals(expected, property.duration)
    }

    @Test fun `property initialization fails when 0 value`() {
        assertFailsWith<IllegalArgumentException> {
            DurationProperty("PT0M")
        }
    }

    @Test fun `property initialization fails when weeks mixed with time`() {
        assertFailsWith<DateTimeParseException> {
            DurationProperty("P2WT10H")
        }
    }
}
