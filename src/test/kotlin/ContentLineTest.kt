package com.vituary.icalendar

import kotlin.IllegalArgumentException
import kotlin.test.*

class ContentLineTest {
    @Test fun `Parse content line without parameters`() {
        val actual = ContentLine("START:VEVENT")
        val expected = ContentLine(ContentLineToken("START"), "VEVENT", emptyMap(), 0)
        assertEquals(expected, actual)
    }

    @Test fun `Parse content line with single parameter`() {
        val actual = ContentLine("DTEND;TZID=America/Vancouver:20190508T174500")
        val expected = ContentLine(ContentLineToken("DTEND"), "20190508T174500", mapOf(Pair(ContentLineToken("TZID"), "America/Vancouver")), 0)
        assertEquals(expected, actual)
    }

    @Test fun `Parse content line with multiple parameters`() {
        val actual = ContentLine("DTEND;KEY1=Value1;KEY2=Value2:20190508T174500")
        val expected = ContentLine(ContentLineToken("DTEND"), "20190508T174500", mapOf(Pair(ContentLineToken("KEY1"), "Value1"), Pair(ContentLineToken("KEY2"), "Value2")), 0)
        assertEquals(expected, actual)
    }

    @Test fun `Parse content line with list parameter value`() {
        val actual = ContentLine("DTEND;KEY=Value1,Value2,Value3:20190508T174500")
        val expected = ContentLine(ContentLineToken("DTEND"), "20190508T174500", mapOf(Pair(ContentLineToken("KEY"), "Value1,Value2,Value3")), 0)
        assertEquals(expected, actual)
    }

    @Test fun `parse content line with several mailtos`() {
        val actual = ContentLine("ATTENDEE;MEMBER=\"mailto:projectA@example.com\",\"mailto:projectB@example.com\":mailto:janedoe@example.com")
        val expected = ContentLine(ContentLineToken("ATTENDEE"), "mailto:janedoe@example.com", mapOf(Pair(ContentLineToken("MEMBER"), "\"mailto:projectA@example.com\",\"mailto:projectB@example.com\"")), 0)
        assertEquals(expected, actual)
    }
}

class ContentLineTokenTest {
    @Test fun `Name can contain uppercase letters`() {
        val testValue = "HELLO"
        val actual = ContentLineToken(testValue)
        assertEquals(testValue, actual.value)
    }

    @Test fun `Name can contain digits`() {
        val testValue = "HELL0W0R1D"
        val actual = ContentLineToken(testValue)
        assertEquals(testValue, actual.value)
    }

    @Test fun `Name can contain dashes`() {
        val testValue = "HELLO-WORLD"
        val actual = ContentLineToken(testValue)
        assertEquals(testValue, actual.value)
    }

    @Test fun `Name can contain lower case letters`() {
        val testValue = "Hello"
        val actual = ContentLineToken(testValue)
        assertEquals(testValue, actual.value)
    }

    @Test fun `Name cannot contain underscores`() {
        val testValue = "HELLO_WORLD"
        val ex = assertFailsWith<IllegalArgumentException> {ContentLineToken(testValue) }
        assertTrue(ex.message?.contains(testValue) ?: false)
    }
}
