package com.vituary.icalendar

import java.lang.IllegalStateException
import java.time.LocalDate
import java.time.Month.*
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class FlexDateTimePropertyTest {
    @Test fun `temporalValue should be LocalDate when type is Date`() {
        val property = FlexDateTimeProperty(DTSTART,"19971102", mapOf(ContentLineToken("VALUE") to "DATE"))
        val expected = LocalDate.of(1997, NOVEMBER, 2)

        val actual = property.temporalValue

        assertEquals(expected, actual)
    }

    @Test fun `temporalValue is Date-Time when value parameter not provided`() {
        val property = FlexDateTimeProperty(DTSTART,"19970903T163000Z", emptyMap())
        val expected = ZonedDateTime.of(1997, SEPTEMBER.value, 3, 16, 30, 0, 0, ZoneId.of("UTC"))

        val actual = property.temporalValue

        assertEquals(expected, actual)
    }

    @Test fun `initialization fails when format is date but type is date-time`() {
        val property = FlexDateTimeProperty(DTSTART,"19971102", emptyMap())

        assertFailsWith<IllegalStateException> {
            property.temporalValue
        }
    }
}
