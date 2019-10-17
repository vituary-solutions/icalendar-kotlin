package com.vituary.icalendar

import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.*
import kotlin.collections.ArrayList

val OUTPUT_LINE_END = "\r\n"
val DATE_FORMAT = "yyyyMMdd";
val TIME_FORMAT = "HHmmss";
val DATE_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT)
val DATETIME_FORMATTER = DateTimeFormatter.ofPattern("$DATE_FORMAT'T'$TIME_FORMAT[X]")
val UTC_TZ = ZoneId.of("UTC")

val BEGIN = ContentLineToken("BEGIN")
val END = ContentLineToken("END")
val VCALENDAR = ContentLineToken("VCALENDAR")
val VEVENT = ContentLineToken("VEVENT")
val VTIMEZONE = ContentLineToken("VTIMEZONE")

val PRODID = ContentLineToken("PRODID")
val VERSION = ContentLineToken("VERSION")
val CALSCALE = ContentLineToken("CALSCALE")
val METHOD = ContentLineToken("METHOD")
val UID = ContentLineToken("UID")
val DTSTAMP = ContentLineToken("DTSTAMP")
val DTSTART = ContentLineToken("DTSTART")
val DTEND = ContentLineToken("DTEND")
val DURATION = ContentLineToken("DURATION")
val RRULE = ContentLineToken("RRULE")
val SUMMARY = ContentLineToken("SUMMARY")
val DESCRIPTION = ContentLineToken("DESCRIPTION")
val LOCATION = ContentLineToken("LOCATION")

val TZID = ContentLineToken("TZID")
val X_LIC_LOCATION = ContentLineToken("X-LIC-LOCATION")

val GREGORIAN_SCALE = "GREGORIAN"
val SUPPORTED_VERSIONS = listOf("2.0")

open class CalendarComponent(val name: ContentLineToken, val components: List<CalendarComponent> = emptyList(), val properties: List<CalendarProperty> = emptyList()) {
    fun findPropertyByName(name: ContentLineToken) : CalendarProperty? {
        val filtered = properties.filter { it.name == name }
        require(filtered.size < 2) { "Expected $name to have only 1 value. Actual: ${filtered.size}" }
        return filtered.firstOrNull()
    }

    fun isPresent(name: ContentLineToken): Boolean {
        return properties.any { it.name == name }
    }

    fun requireProperties(vararg propertyNames: ContentLineToken) {
        propertyNames.forEach { name -> require(isPresent(name)) { "A $name property is required" } }
    }

    fun getPropertyValue(name: ContentLineToken) : String? {
        return findPropertyByName(name)?.value
    }

    override fun toString(): String {
        return (properties + components).joinToString(OUTPUT_LINE_END, "BEGIN:$name$OUTPUT_LINE_END", "${OUTPUT_LINE_END}END:$name")
    }

    inline fun <reified R : CalendarProperty> getTypedProperty(name: ContentLineToken, map: (CalendarProperty) -> R): R? {
        val property = findPropertyByName(name)
        return when (property) {
            is R -> property
            null -> null
            else -> map(property)
        }
    }
}

// TODO variables perhaps shouldn't be optional, in particular context
open class CalendarProperty(val name: ContentLineToken, private val rawValue: String, val params: Map<ContentLineToken, String> = emptyMap(),
                            val context: CalendarContext = CalendarContext(), val localTimeZone: ZoneId = ZoneId.systemDefault()) {
    val value = rawValue.replace("\\n", "\n").replace("\\r", "\r")
            .replace("\\,", ",").replace("\\;", ";").replace("\\:", ":")
            .replace("\\\"", "\"").replace("\\\\", "\\")

    override fun toString(): String {
        val paramStr = if (params.isNotEmpty()) params.toList().joinToString(";", ";" ) { "${it.first}=${it.second}" } else ""
        val unfoldedResult = "$name$paramStr:$rawValue"
        return unfoldedResult.chunked(75).joinToString("$OUTPUT_LINE_END ")
    }
}

class Calendar(components: List<CalendarComponent> = ArrayList(), properties: List<CalendarProperty>) : CalendarComponent(VCALENDAR, components, properties) {
    val events = components.filterIsInstance<Event>()
    val prodId = getPropertyValue(PRODID)
    val version = getPropertyValue(VERSION)
    val calendarScale = getPropertyValue(CALSCALE) ?: GREGORIAN_SCALE
    val method = getPropertyValue(METHOD)

    init {
        require(prodId != null) { "A $PRODID property is required" }
        require(SUPPORTED_VERSIONS.contains(version)) { "The $VERSION property is unsupported. It must be one of: ${SUPPORTED_VERSIONS.joinToString(", ")}" }
        require(calendarScale == GREGORIAN_SCALE) { "Invalid $CALSCALE property provided, only \"$GREGORIAN_SCALE\" is supported" }
        require(components.isNotEmpty()) { "Calendar must have at least 1 component" }
    }
}

class Event(components: List<CalendarComponent> = ArrayList(), properties: List<CalendarProperty>, context: CalendarContext = CalendarContext()) : CalendarComponent(VEVENT, components, properties) {
    private val startTimeProperty = getTypedProperty(DTSTART, FlexDateTimeProperty.from)
    private val endTimeProperty = getTypedProperty(DTEND, FlexDateTimeProperty.from)
    private val durationProperty = getTypedProperty(DURATION, DurationProperty.from)
    private val recurrenceRuleProperty = getTypedProperty(RRULE, RecurrenceRuleProperty.from)

    val uid = getPropertyValue(UID);
    val summary = getPropertyValue(SUMMARY).orEmpty()
    val description = getPropertyValue(DESCRIPTION).orEmpty()
    val location = getPropertyValue(LOCATION).orEmpty()

    val dateType = startTimeProperty?.type
    val start = startTimeProperty?.temporalValue
    val end: Temporal?
    val recurrences: List<Temporal>? = if (start != null && recurrenceRuleProperty != null) calculateRecurrences(start, recurrenceRuleProperty) else null

    init {
        requireProperties(UID)
        require(startTimeProperty != null || context.method != null) { "A $DTSTART property is required when the calendar doesn't have a $METHOD property" }
        require(endTimeProperty == null || durationProperty == null) { "Only one of the $DTEND or $DURATION properties is allowed" }

        if (recurrenceRuleProperty != null) {
            if (dateType == FlexDateTimeType.DATE) {
                require(recurrenceRuleProperty.bySecondList == null && recurrenceRuleProperty.byMinuteList == null && recurrenceRuleProperty.byHourList == null) {
                    "A recurrence may not specify a time component when $DTSTART is ${FlexDateTimeType.DATE} type"
                }
                require(recurrenceRuleProperty.until == null || recurrenceRuleProperty.until is LocalDate) { "Expected $RRULE to have an $UNTIL_KEY property of type ${FlexDateTimeType.DATE} to match $DTSTART" }
            }
            else {
                require(recurrenceRuleProperty.until == null || recurrenceRuleProperty.until is ZonedDateTime) { "Expected $RRULE to have an $UNTIL_KEY property of type ${FlexDateTimeType.DATETIME} to match $DTSTART" }
            }
        }

        end = when {
            endTimeProperty != null -> {
                require(endTimeProperty.type == dateType) { "$DTEND type does not match $DTSTART. Expected $dateType but was ${endTimeProperty.type}" }
                require(startTimeProperty != null) { "$DTEND was provided but $DTSTART is null" }
                require(endTimeProperty > startTimeProperty) { "$DTEND (${endTimeProperty.temporalValue}) must be later than $DTSTART (${startTimeProperty.temporalValue})" }
                endTimeProperty.temporalValue
            }
            durationProperty != null -> {
                require(dateType == FlexDateTimeType.DATETIME || durationProperty.dateType == FlexDateTimeType.DATE) { "When $DTSTART is a ${FlexDateTimeType.DATE} then $DURATION cannot have a time component: ${durationProperty.value}" }
                start?.plus(durationProperty.duration)
            }
            else -> null
        }
    }
}

fun calculateRecurrences(originalStart: Temporal, recurrenceRule: RecurrenceRuleProperty): List<Temporal> {
    return RecurrenceGenerator(recurrenceRule, originalStart).calculateRecurrences()
}

class CalendarTimeZone (components: List<CalendarComponent> = ArrayList(), properties: List<CalendarProperty>) : CalendarComponent(VTIMEZONE, components, properties) {
    private val locationExtension = getPropertyValue(X_LIC_LOCATION)
    val timeZoneId = getPropertyValue(TZID)

    // Check to see if X-LIC-LOCATION is present.  This is non standard so not always available.  Otherwise check the
    // TZID as this is often the a common Time Zone name.  If not, then this library does not yet support the time zone.
    val timeZone: ZoneId = ZoneId.of(if (!locationExtension.isNullOrEmpty()) locationExtension else timeZoneId)
}
