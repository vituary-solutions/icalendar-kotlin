package com.vituary.icalendar

import java.lang.IllegalArgumentException
import java.time.*
import java.time.temporal.TemporalField
import java.time.temporal.ChronoField.*
import java.time.temporal.TemporalAmount
import kotlin.math.absoluteValue

val FREQUENCY_KEY = "FREQ"
val COUNT_KEY = "COUNT"
val UNTIL_KEY = "UNTIL"
val INTERVAL_KEY = "INTERVAL"
val WEEKSTART_KEY = "WKST"

val BYSECOND_KEY = "BYSECOND"
val BYMINUTE_KEY = "BYMINUTE"
val BYHOUR_KEY = "BYHOUR"
val BYWEEKDAY_KEY = "BYDAY"
val BYMONTHDAY_KEY = "BYMONTHDAY"
val BYYEARDAY_KEY = "BYYEARDAY"
val BYMONTH_KEY = "BYMONTH"
val BYWEEKNUMBER_KEY = "BYWEEKNO"
val BYSETPOSITION_KEY = "BYSETPOS"

enum class RecurrenceFrequency(temporalField: TemporalField) {
    SECONDLY(SECOND_OF_MINUTE),
    MINUTELY(MINUTE_OF_HOUR),
    HOURLY(HOUR_OF_DAY),
    DAILY(DAY_OF_MONTH),
    WEEKLY(ALIGNED_WEEK_OF_YEAR),
    MONTHLY(MONTH_OF_YEAR),
    YEARLY(YEAR);
    val temporalUnit = temporalField.baseUnit
}

enum class DurationScope(val symbol: Char) {
    WEEK('W'), DAY('D'), HOUR('H'), MINUTE('M'), SECOND('S')
}

enum class Weekday() {
    MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY;
    val symbol = this.name.substring(0, 2)
    val dayOfWeek = DayOfWeek.of(ordinal + 1)

    override fun toString(): String {
        return symbol
    }

    companion object {
        fun findBySymbol(symbol: String): Weekday {
            var result = Weekday.values().find { it.symbol == symbol }
            require(result != null) { "Weekday not recognized: $symbol" }
            return result
        }
    }
}

class RecurrenceRuleProperty(value: String, params: Map<ContentLineToken, String> = emptyMap(), localTimeZone: ZoneId = ZoneId.systemDefault()) : CalendarProperty(RRULE, value, params) {
    private val parts = value.splitCalendarValueToMap()

    val frequency: RecurrenceFrequency = RecurrenceFrequency.valueOf(parts[FREQUENCY_KEY] ?: throw IllegalArgumentException("$FREQUENCY_KEY is required"))
    val interval: Int = parts[INTERVAL_KEY]?.toInt() ?: 1
    val count: Int? = parts[COUNT_KEY]?.toIntOrNull()
    val until = parts[UNTIL_KEY]?.parseCalendarTemporal(localTimeZone)
    val weekStart: Weekday = Weekday.findBySymbol(parts[WEEKSTART_KEY] ?: Weekday.MONDAY.symbol)

    val bySecondList = partAsIntList(BYSECOND_KEY, 0..60)
    val byMinuteList = partAsIntList(BYMINUTE_KEY, 0..59)
    val byHourList = partAsIntList(BYHOUR_KEY, 0..23)
    val byMonthDayList = partAsIntList(BYMONTHDAY_KEY, 1..31, true)
    val byYearDayList = partAsIntList(BYYEARDAY_KEY, 1..366, true)
    val byWeekNumberList = partAsIntList(BYWEEKNUMBER_KEY, 1..53, true)
    val byMonthList = partAsIntList(BYMONTH_KEY, 1..12)
    val bySetPositionList = partAsIntList(BYSETPOSITION_KEY, 1..366, true)
    val byWeekDayList = parts[BYWEEKDAY_KEY]?.split(',')?.map {
        val weekday = Weekday.findBySymbol(it.takeLast(2))
        val number = if (it.length > 2) it.dropLast(2).toInt() else 0
        require(number in -53..53)
        Pair(number, weekday)
    }

    init {
        require(count == null || count > 0) { "$COUNT_KEY must be > 0" }
        require(interval > 0) { "$INTERVAL_KEY must be > 0" }
        require(count == null || until == null) { "Only one of $COUNT_KEY or $UNTIL_KEY may be provided, not both" }
        require(byWeekNumberList == null || frequency == RecurrenceFrequency.YEARLY) { "$BYWEEKNUMBER_KEY may only be specified when the $FREQUENCY_KEY is ${RecurrenceFrequency.WEEKLY}" }
        require(byYearDayList == null || !(frequency in listOf(RecurrenceFrequency.DAILY, RecurrenceFrequency.WEEKLY, RecurrenceFrequency.MONTHLY))) { "$BYYEARDAY_KEY cannot be specified with a $FREQUENCY_KEY of $frequency" }
        require(byMonthDayList == null || frequency != RecurrenceFrequency.WEEKLY) { "$BYMONTHDAY_KEY cannot be specified with a $FREQUENCY_KEY of ${RecurrenceFrequency.WEEKLY}" }
    }

    private fun partAsIntList(key: String, validRange: IntRange, allowNegative: Boolean = false): List<Int>? {
        val result = parts[key]?.split(',')?.map { it.toInt() }
        result?.map { if (allowNegative) it.absoluteValue else it }?.forEach { require(it in validRange) { "Invalid $key value, must be in range of ${validRange.start} to ${validRange.endInclusive}: $it" } }
        return result
    }

    companion object Copy {
        val from: (CalendarProperty) -> RecurrenceRuleProperty = { source ->
            require(source.name == RRULE) { "Expected property name was $RRULE, received ${source.name}" }
            RecurrenceRuleProperty(source.value, source.params)
        }
    }
}

val PERIOD_REGEX = "^P\\d+(?:W|D)\$".toRegex()
class DurationProperty(value: String, params: Map<ContentLineToken, String> = emptyMap())
    : CalendarProperty(DURATION, value, params) {
    val duration : TemporalAmount
    val dateType : FlexDateTimeType

    init {
        val isPeriod = PERIOD_REGEX.matches(value)
        duration = if (isPeriod) Period.parse(value) else Duration.parse(value)
        dateType = if (isPeriod) FlexDateTimeType.DATE else FlexDateTimeType.DATETIME

        val lazyNonPositiveMessage = { "$DURATION must be a positive non-zero value: $value" }
        when (duration) {
            is Duration -> require(duration > Duration.ZERO, lazyNonPositiveMessage)
            is Period -> require(!(duration.isNegative || duration.isZero), lazyNonPositiveMessage)
        }
    }

    companion object Copy {
        val from: (CalendarProperty) -> DurationProperty = { source ->
            require(source.name == DURATION) { "Expected property name was $DURATION, received ${source.name}" }
            DurationProperty(source.value, source.params)
        }
    }
}
