package com.vituary.icalendar

import java.time.*
import java.time.Month.*
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjuster
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class CalendarComponentTest {
    @Test fun `getPropertyByName should return CalendarProperty with same name`() {
        val property = CalendarProperty(ContentLineToken("MY-PROP"), "some value")
        val component = CalendarComponent(ContentLineToken("SOME-TOKEN"), emptyList(), listOf(property))

        val actual = component.findPropertyByName(ContentLineToken("My-Prop"))

        assertEquals(property, actual)
    }

    @Test fun `getPropertyByName should return null when not found`() {
        val component = CalendarComponent(ContentLineToken("SOME-TOKEN"), emptyList(), emptyList())

        val actual = component.findPropertyByName(ContentLineToken("SOME-PROP"))

        assertNull(actual)
    }
}

class CalendarPropertyTest {
    @Test fun `getLocalDateTimeValue should use the local timezone by default`() {
        val localTimeZone = ZoneId.of("America/New_York")
        val property = FlexDateTimeProperty(ContentLineToken("DTPROP"), "19970714T133005", emptyMap(), CalendarContext(), localTimeZone)

        val expected = buildLocalDateTime(localTimeZone, 1997, JULY, 14, 13, 30, 5)

        val actual = property.temporalValue

        assertEquals(expected, actual)
    }

    @Test fun `getLocalDateTimeValue should use UTC when there is a trailing Z`() {
        val localTimeZone = ZoneId.of("America/New_York")
        val targetTimeZone = ZoneId.of("UTC")
        val property = FlexDateTimeProperty(ContentLineToken("DTPROP"), "19970714T173005Z", emptyMap(), CalendarContext(), localTimeZone)

        val expected = buildLocalDateTime(targetTimeZone, 1997, JULY, 14, 17, 30, 5)

        val actual = property.temporalValue

        assertEquals(expected, actual)
    }

    @Test fun `getLocalDateTimeValue should use TZID property when set`() {
        val localTimeZone = ZoneId.of("America/Los_Angeles")
        val targetTimeZone = ZoneId.of("America/New_York")
        val context = CalendarContext()
        context.timeZones.add(CalendarTimeZone(emptyList(), listOf(CalendarProperty(TZID, "America/New_York"))))
        val property = FlexDateTimeProperty(ContentLineToken("DTPROP"), "19970714T133005", mapOf(TZID to "America/New_York"), context, localTimeZone)
        val expected = buildLocalDateTime(targetTimeZone, 1997, JULY, 14, 13, 30, 5)

        val actual = property.temporalValue

        assertEquals(expected, actual)
    }

    @Test fun `getLocalDateTimeValue local timezone should default to the system default timezone`() {
        val expectedTimeZone = ZoneId.systemDefault()
        val property = FlexDateTimeProperty(ContentLineToken("DTPROP"), "19970714T133005")

        val expected = buildLocalDateTime(expectedTimeZone, 1997, JULY, 14, 13, 30, 5)

        val actual = property.temporalValue

        assertEquals(expected, actual)
    }

    @Test fun `getLocalDateTimeValue should throw an exception if specified TZID not present in context`() {
        val property = FlexDateTimeProperty(ContentLineToken("DTPROP"), "19970714T133005", mapOf(TZID to "America/New_York"), CalendarContext())

        assertFailsWith<IllegalStateException> {
            property.temporalValue
        }
    }

    @Test fun `getLocalDateTimeValue should fail if both TZID and trailing Z are present`() {
        val context = CalendarContext()
        context.timeZones.add(CalendarTimeZone(emptyList(), listOf(CalendarProperty(TZID, "America/New_York"))))
        val property = FlexDateTimeProperty(ContentLineToken("DTPROP"), "19970714T133005Z", mapOf(TZID to "America/New_York"), context)

        assertFailsWith<IllegalStateException> {
            property.temporalValue
        }
    }

    @Test fun `getLocalDateTimeValue should fail if the property is not a date property`() {
        val property = FlexDateTimeProperty(ContentLineToken("STATUS"), "CONFIRMED", emptyMap(), CalendarContext())

        assertFailsWith<DateTimeParseException> {
            property.temporalValue
        }
    }

    private fun buildLocalDateTime(zoneId: ZoneId, year: Int, month: Month, date: Int, hourOfDay: Int, minute: Int, second: Int): ZonedDateTime {
        return ZonedDateTime.of(year, month.value, date, hourOfDay, minute, second, 0, zoneId)
    }
}
