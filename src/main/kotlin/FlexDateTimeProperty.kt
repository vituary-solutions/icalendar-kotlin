package com.vituary.icalendar

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoField
import java.time.temporal.Temporal

private val FLEX_DATE_TIME_TYPE_PARAM = ContentLineToken("VALUE")
enum class FlexDateTimeType (val text: String) {
    DATE("DATE"), DATETIME("DATE-TIME");

    override fun toString(): String {
        return text
    }

    companion object {
        fun findByText(text: String) : FlexDateTimeType {
            return values().find { it.text == text } ?: DATETIME
        }
    }
}

class FlexDateTimeProperty(name: ContentLineToken, value: String, params: Map<ContentLineToken, String> = emptyMap(),
                           context: CalendarContext = CalendarContext(), localTimeZone: ZoneId = ZoneId.systemDefault()) :
        CalendarProperty(name, value, params, context, localTimeZone) {
    val type = FlexDateTimeType.findByText(params[FLEX_DATE_TIME_TYPE_PARAM] ?: "")
    val temporalValue: Temporal
        get() {
            var parseTimeZoneId = localTimeZone
            if (type == FlexDateTimeType.DATETIME) {
                check(!(params.containsKey(TZID) && value.endsWith('Z'))) { "A DATE-TIME property may have a $TZID parameter or end with 'Z', not both" }

                if (params.containsKey(TZID)) {
                    val timeZoneComponent = context.timeZones.find { it.timeZoneId == params[TZID] }
                    check(timeZoneComponent != null) { "The specified $TZID must match a $VTIMEZONE" }
                    parseTimeZoneId = timeZoneComponent.timeZone
                }
            }
            val result = value.parseCalendarTemporal(parseTimeZoneId)
            check((type == FlexDateTimeType.DATETIME && result.isSupported(ChronoField.HOUR_OF_DAY)) || (type == FlexDateTimeType.DATE && result is LocalDate)) {
                "Expected property type is $type but value did not match and was parsed to an instance of ${result.javaClass}: $value"
            }
            return result
        }

    operator fun compareTo(other: FlexDateTimeProperty): Int {
        return when (type) {
            FlexDateTimeType.DATE -> LocalDate.from(temporalValue).compareTo(LocalDate.from(other.temporalValue))
            FlexDateTimeType.DATETIME -> LocalDateTime.from(temporalValue).compareTo(LocalDateTime.from(other.temporalValue))
        }
    }

    companion object Copy {
        val from: (CalendarProperty) -> FlexDateTimeProperty = { source ->
            FlexDateTimeProperty(source.name, source.value, source.params, source.context, source.localTimeZone)
        }
    }
}
