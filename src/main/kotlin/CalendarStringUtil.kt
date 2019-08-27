package com.vituary.icalendar

import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.time.temporal.Temporal

fun String.splitCalendarValueToMap(): Map<String, String> {
    if (this.isEmpty()) {
        return emptyMap()
    }

    val pairs = this.split(';').map {
        val split = it.split('=')
        check(split.size == 2) { "Invalid key value pair, should be in format \"<key>=<value>\" separated by ';': \"${this}\"" }
        Pair(split[0], split[1])
    }

    // ensure there are no duplicate keys
    pairs.groupBy { it.first }.forEach { check(it.value.size == 1) { "Duplicate keys found (${it.key}): \"${this}\"" } }

    return pairs.toMap()
}

fun String.parseCalendarTemporal(parseTimeZone: ZoneId = ZoneId.systemDefault()) : Temporal {
    return try {
        LocalDate.parse(this, DATE_FORMATTER)
    } catch (_: DateTimeParseException) {
        val zoneId = if (this.endsWith('Z')) UTC_TZ else parseTimeZone
        ZonedDateTime.parse(this, DATETIME_FORMATTER.withZone(zoneId))
    }
}
