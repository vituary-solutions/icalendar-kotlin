package com.vituary.icalendar

import java.time.ZoneId
import java.util.Collections.unmodifiableList
import kotlin.collections.ArrayList

class CalendarComponentBuilder(var name: ContentLineToken, var context: CalendarContext) {
    private val components: MutableList<CalendarComponent> = ArrayList()
    private val properties: MutableList<CalendarProperty> = ArrayList()

    fun add(component: CalendarComponent) : CalendarComponentBuilder {
        components.add(component)
        return this
    }

    fun add(property: CalendarProperty) : CalendarComponentBuilder {
        properties.add(property)
        return this
    }

    fun build(): CalendarComponent {
        check(components.isNotEmpty() || properties.isNotEmpty()) { "At least one component or property must be provided" }
        val components = unmodifiableList(components.toList())
        val properties = unmodifiableList(properties.toList())
        return when (name) {
            VCALENDAR -> Calendar(components, properties)
            VEVENT -> Event(components, properties, context)
            VTIMEZONE -> CalendarTimeZone(components, properties)
            else -> CalendarComponent(name, components, properties)
        }
    }
}

class CalendarPropertyBuilder(var name: ContentLineToken, var context: CalendarContext, val localTimeZone: ZoneId = ZoneId.systemDefault()) {
    private var params: Map<ContentLineToken, String> = emptyMap()
    private var value: String? = null

    fun params(params: Map<ContentLineToken, String>) : CalendarPropertyBuilder {
        this.params = params
        return this
    }

    fun value(value: String) : CalendarPropertyBuilder {
        this.value = value
        return this
    }

    fun build(): CalendarProperty {
        check(value != null) { "value must be provided" }
        return when (name) {
            DTSTART, DTEND -> FlexDateTimeProperty(name, value.orEmpty(), params, context, localTimeZone)
            DURATION -> DurationProperty(value.orEmpty(), params)
            RRULE -> RecurrenceRuleProperty(value.orEmpty(), params)
            else -> CalendarProperty(name, value.orEmpty(), params, context, localTimeZone)
        }
    }
}
