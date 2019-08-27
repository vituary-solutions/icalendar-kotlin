package com.vituary.icalendar

import kotlin.test.Test
import kotlin.test.assertEquals

class CalendarComponentBuilderTest {
    @Test fun `adding component after build does not alter built component`() {
        val builder = CalendarComponentBuilder(ContentLineToken("My-Token"), CalendarContext())
        builder.add(CalendarProperty(ContentLineToken("Sub-Token1"), "value"))
        val component = builder.build()

        builder.add(CalendarProperty(ContentLineToken("Sub-Token2"), "value"))

        assertEquals(1, component.properties.size)
    }
}