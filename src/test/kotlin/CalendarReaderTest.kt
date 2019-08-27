package com.vituary.icalendar

import org.junit.Test
import java.io.InputStream
import java.lang.IllegalArgumentException
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CalendarReaderTest {
    @Test fun `Reader readCalendars should successfully read minimal file`() {
        loadResourceAsStream("minimal.ics").use { stream ->
            val calendars = stream.reader().readCalendars()

            assertEquals(1, calendars.size)
            assertEquals(2, calendars[0].properties.size)
            assertEquals(1, calendars[0].components.size)
            assertTrue(calendars[0].components[0] is Event)
        }
    }

    @Test fun `Reader readCalendars unfolds multi-line values`() {
        loadResourceAsStream("folded-lines.ics").use { stream ->
            val calendars = stream.reader().readCalendars()

            assertEquals("-//hacksw/handcal//NONSGML v1.0//EN", calendars[0].prodId)
            assertEquals("19970610T172345Z-AF23B2@example.com", calendars[0].events[0].uid)
            assertEquals(
                    " The quick brown fox jumps over the lazy dog. The preceding was a simple sentence often used in typography. It's a panagram, which users every character in the alphabet.",
                    calendars[0].events[0].summary
            )
        }
    }

    @Test fun `readCalendars returns error when root component is not VCALENDAR`() {
        loadResourceAsStream("event-root.ics").use { stream ->
            assertFailsWith<IllegalArgumentException> {
                stream.reader().readCalendars()
            }
        }
    }

    @Test fun `readCalendars returns error when property at root level`() {
        loadResourceAsStream("property-root.ics").use { stream ->
            assertFailsWith<IllegalArgumentException> {
                stream.reader().readCalendars()
            }
        }
    }

    @Test fun `readCalendars returns error when component endings are misplaced`() {
        loadResourceAsStream("incorrect-end-line.ics").use { stream ->
            assertFailsWith<IllegalArgumentException> {
                stream.reader().readCalendars()
            }
        }
    }

    private fun loadResourceAsStream(name: String) : InputStream {
        return this::class.java.classLoader.getResourceAsStream(name)
    }
}
