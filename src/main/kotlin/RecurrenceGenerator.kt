package com.vituary.icalendar

import java.time.*
import java.time.Month
import java.time.temporal.*
import java.time.temporal.ChronoField.*
import java.util.*

class RecurrenceGenerator(private val rules: RecurrenceRuleProperty, private val startDate: Temporal) {
    private val weekFields = WeekFields.of(rules.weekStart.dayOfWeek, 4)

    /**
     * Find all the occurrences for the associated rule, including the startDate as the first occurrence
     * @param maxIterationDate - A date to use as the stop date for calculating iterations.  Once the iteration applied
     *                           to startDate has reach this value, no more occurrences will be generated.  The default
     *                           is 1 year plus 1 day past the current date.
     */
    fun calculateRecurrences(maxIterationDate: LocalDate = LocalDate.now().plusYears(1).plusDays(1)) : List<Temporal> {
        val recurrences = TreeSet<Temporal>() // use tree set to ensure uniqueness and ordering as we add iterations
        recurrences.add(startDate)

        var sourceDate = startDate
        recurrences.addAll(calculateSingleIterationRecurrences(sourceDate).filter { it > startDate })

        while (LocalDate.from(sourceDate) < maxIterationDate &&
                (rules.until == null || recurrences.last() <= rules.until) &&
                (rules.count == null || recurrences.size < rules.count)) {
            sourceDate = sourceDate.plus(rules.interval.toLong(), rules.frequency.temporalUnit)
            recurrences.addAll(calculateSingleIterationRecurrences(sourceDate))
        }

        val finalList = when {
            rules.count != null -> recurrences.take(rules.count)
            rules.until != null -> recurrences.filter { it <= rules.until }
            else -> recurrences.toList()
        }

        return finalList
    }

    private fun calculateSingleIterationRecurrences(sourceDate: Temporal): Collection<Temporal> {
        var recurrences: Collection<Temporal> = listOf(sourceDate)

        when (rules.frequency) {
            RecurrenceFrequency.YEARLY -> {
                recurrences = expandByMonth(recurrences)
                recurrences = expandByWeekNumber(recurrences)
                recurrences = expandByYearDay(recurrences)
                recurrences = expandByMonthDay(recurrences)
                recurrences = when {
                    rules.byMonthDayList != null || rules.byYearDayList != null -> limitByWeekDay(recurrences)
                    rules.byWeekNumberList != null -> expandByWeekDayIntoWeek(recurrences)
                    rules.byMonthList != null -> expandByWeekDayIntoMonth(recurrences)
                    else -> expandByWeekDayIntoYear(recurrences)
                }

                recurrences = expandByHour(recurrences)
                recurrences = expandByMinute(recurrences)
                recurrences = expandBySecond(recurrences)
            }
            RecurrenceFrequency.MONTHLY -> {
                recurrences = limitByMonth(recurrences)
                recurrences = expandByMonthDay(recurrences)

                recurrences = when {
                    rules.byMonthDayList != null -> limitByWeekDay(recurrences)
                    else -> expandByWeekDayIntoMonth(recurrences)
                }

                recurrences = expandByHour(recurrences)
                recurrences = expandByMinute(recurrences)
                recurrences = expandBySecond(recurrences)
            }
            RecurrenceFrequency.WEEKLY -> {
                recurrences = limitByMonth(recurrences)
                recurrences = expandByWeekDayIntoWeek(recurrences)
                recurrences = expandByHour(recurrences)
                recurrences = expandByMinute(recurrences)
                recurrences = expandBySecond(recurrences)
            }
            RecurrenceFrequency.DAILY -> {
                recurrences = limitByMonth(recurrences)
                recurrences = limitByMonthDay(recurrences)
                recurrences = limitByWeekDay(recurrences)
                recurrences = expandByHour(recurrences)
                recurrences = expandByMinute(recurrences)
                recurrences = expandBySecond(recurrences)
            }
            RecurrenceFrequency.HOURLY -> {
                recurrences = limitByMonth(recurrences)
                recurrences = limitByYearDay(recurrences)
                recurrences = limitByMonthDay(recurrences)
                recurrences = limitByWeekDay(recurrences)
                recurrences = limitByHour(recurrences)
                recurrences = expandByMinute(recurrences)
                recurrences = expandBySecond(recurrences)
            }
            RecurrenceFrequency.MINUTELY -> {
                recurrences = limitByMonth(recurrences)
                recurrences = limitByYearDay(recurrences)
                recurrences = limitByMonthDay(recurrences)
                recurrences = limitByWeekDay(recurrences)
                recurrences = limitByHour(recurrences)
                recurrences = limitByMinute(recurrences)
                recurrences = expandBySecond(recurrences)
            }
            RecurrenceFrequency.SECONDLY -> {
                recurrences = limitByMonth(recurrences)
                recurrences = limitByYearDay(recurrences)
                recurrences = limitByMonthDay(recurrences)
                recurrences = limitByWeekDay(recurrences)
                recurrences = limitByHour(recurrences)
                recurrences = limitByMinute(recurrences)
                recurrences = limitBySecond(recurrences)
            }
        }

        return limitBySetPosition(recurrences)
    }

    private fun expandByMonth(recurrences: Collection<Temporal>) : Collection<Temporal> {
        return rules.byMonthList?.flatMap { month -> recurrences.map {it.with(Month.of(month)) } } ?: recurrences
    }

    private fun limitByMonth(recurrences: Collection<Temporal>) : Collection<Temporal> {
        return recurrences.filter { recurrence -> rules.byMonthList?.any { it == recurrence.get(MONTH_OF_YEAR) } ?: true }
    }

    private fun expandByWeekNumber(recurrences: Collection<Temporal>) : Collection<Temporal> {
        // TODO handle negative value
        return rules.byWeekNumberList?.flatMap { weekNum -> recurrences.map { it.with(weekFields.weekOfYear(), weekNum.toLong()) } } ?: recurrences
    }

    private fun expandByYearDay(recurrences: Collection<Temporal>) : Collection<Temporal> {
        // TODO handle negative value
        return rules.byYearDayList?.flatMap { yearDay -> recurrences.map { it.with(DAY_OF_YEAR, yearDay.toLong()) } } ?: recurrences
    }

    private fun limitByYearDay(recurrences: Collection<Temporal>) : Collection<Temporal> {
        // TODO handle negative value
        return recurrences.filter { recurrence -> rules.byYearDayList?.any { it == recurrence.get(DAY_OF_YEAR) } ?: true }
    }

    private fun expandByMonthDay(recurrences: Collection<Temporal>) : Collection<Temporal> {
        return rules.byMonthDayList?.flatMap { monthDay -> recurrences.map { it.with(DAY_OF_MONTH, monthDay.toLong()) } } ?: recurrences
    }

    private fun limitByMonthDay(recurrences: Collection<Temporal>) : Collection<Temporal> {
        return recurrences.filter { recurrence -> rules.byMonthDayList?.any { it == recurrence.get(DAY_OF_MONTH) } ?: true }
    }

    private fun expandByWeekDayIntoWeek(recurrences: Collection<Temporal>) : Collection<Temporal> {
        // TODO Handle +/- values if applicable
        return rules.byWeekDayList?.flatMap { weekDayPair -> recurrences.map {
            val weekOfYear = it.getLong(weekFields.weekOfYear())
            it.with(DAY_OF_WEEK, weekDayPair.second.dayOfWeek.value.toLong()).with(weekFields.weekOfYear(), weekOfYear)
        }} ?: recurrences
    }

    private fun expandByWeekDayIntoMonth(recurrences: Collection<Temporal>) : Collection<Temporal> {
        return rules.byWeekDayList?.flatMap { weekDayPair -> recurrences.flatMap { monthTemporal ->
            val dayOfWeek = weekDayPair.second.dayOfWeek
            val timesInMonth = countDaysOfWeekInMonth(dayOfWeek, monthTemporal)
            when {
                weekDayPair.first != 0 -> listOf(monthTemporal.with(TemporalAdjusters.dayOfWeekInMonth(weekDayPair.first, weekDayPair.second.dayOfWeek)))
                else -> (1..timesInMonth).map { monthTemporal.with(TemporalAdjusters.dayOfWeekInMonth(it, weekDayPair.second.dayOfWeek)) }
            }
        }} ?: recurrences
    }

    private fun expandByWeekDayIntoYear(recurrences: Collection<Temporal>) : Collection<Temporal> {
        return rules.byWeekDayList?.flatMap { weekDayPair -> recurrences.flatMap { yearTemporal ->
            val dayOfWeek = weekDayPair.second.dayOfWeek
            val timesInYear = countDaysOfWeekInYear(dayOfWeek, yearTemporal)
            when {
                weekDayPair.first > 0 -> listOf(yearTemporal.with(firstInYear(dayOfWeek)).plus(weekDayPair.first.toLong() - 1, ChronoUnit.WEEKS))
                weekDayPair.first < 0 -> listOf(yearTemporal.with(lastInYear(dayOfWeek)).minus(weekDayPair.first.toLong() - 1, ChronoUnit.WEEKS))
                else -> (1..timesInYear).map { yearTemporal.with(TemporalAdjusters.dayOfWeekInMonth(it, weekDayPair.second.dayOfWeek)) }
            }
        }} ?: recurrences
    }

    // TODO move out of this class?
    private fun countDaysOfWeekInMonth(dayOfWeek: DayOfWeek, temporal: Temporal): Int {
        val lastInMonth = temporal.with(TemporalAdjusters.lastInMonth(dayOfWeek)).get(DAY_OF_MONTH)
        return if (lastInMonth > 28) 5 else 4
    }

    // TODO move out of this class?
    private fun countDaysOfWeekInYear(dayOfWeek: DayOfWeek, temporal: Temporal): Int {
        val firstInYear = temporal.with(firstInYear(dayOfWeek)).get(DAY_OF_YEAR)
        val lastInYear = temporal.with(lastInYear(dayOfWeek)).get(DAY_OF_YEAR)
        return ((lastInYear - firstInYear) / 7) + 1
    }

    // TODO move out of this class? TemporalAdjusters extension?
    private fun firstInYear(dayOfWeek: DayOfWeek): TemporalAdjuster {
        return TemporalAdjuster { it.with(Month.JANUARY).with(TemporalAdjusters.firstInMonth(dayOfWeek)) }
    }

    // TODO move out of this class? TemporalAdjusters extension?
    private fun lastInYear(dayOfWeek: DayOfWeek): TemporalAdjuster {
        return TemporalAdjuster { it.with(Month.DECEMBER).with(TemporalAdjusters.lastInMonth(dayOfWeek)) }
    }

    private fun limitByWeekDay(recurrences: Collection<Temporal>) : Collection<Temporal> {
        // TODO Handle +/- values if applicable
        return recurrences.filter { recurrence -> rules.byWeekDayList?.any { it.second.dayOfWeek.value == recurrence.get(DAY_OF_WEEK) } ?: true }
    }

    private fun expandByHour(recurrences: Collection<Temporal>) : Collection<Temporal> {
        return rules.byHourList?.flatMap { hourOfDay -> recurrences.map { it.with(HOUR_OF_DAY, hourOfDay.toLong()) } } ?: recurrences
    }

    private fun limitByHour(recurrences: Collection<Temporal>) : Collection<Temporal> {
        return recurrences.filter { recurrence -> rules.byHourList?.any { it == recurrence.get(HOUR_OF_DAY) } ?: true }
    }

    private fun expandByMinute(recurrences: Collection<Temporal>) : Collection<Temporal> {
        return rules.byMinuteList?.flatMap { minute -> recurrences.map { it.with(MINUTE_OF_HOUR, minute.toLong()) } } ?: recurrences
    }

    private fun limitByMinute(recurrences: Collection<Temporal>) : Collection<Temporal> {
        return recurrences.filter { recurrence -> rules.byMinuteList?.any { it == recurrence.get(MINUTE_OF_HOUR) } ?: true }
    }

    private fun expandBySecond(recurrences: Collection<Temporal>) : Collection<Temporal> {
        return rules.bySecondList?.flatMap { second -> recurrences.map { it.with(SECOND_OF_MINUTE, second.toLong()) } } ?: recurrences
    }

    private fun limitBySecond(recurrences: Collection<Temporal>) : Collection<Temporal> {
        return recurrences.filter { recurrence -> rules.bySecondList?.any { it == recurrence.get(SECOND_OF_MINUTE) } ?: true }
    }

    private fun limitBySetPosition(recurrences: Collection<Temporal>) : Collection<Temporal> {
        val sortedUniqueInput = TreeSet(recurrences)
        return sortedUniqueInput.filterIndexed { index, _ -> rules.bySetPositionList?.any { it in listOf(index + 1, index - sortedUniqueInput.size) } ?: true }
    }
}

private operator fun Temporal.compareTo(other: Temporal) : Int {
    val precision: TemporalUnit = this.query(TemporalQueries.precision())
    require(this.query(TemporalQueries.precision()) == other.query(TemporalQueries.precision())) { "$this cannot be compared to $other as their precisions do not match. Expected: $precision" }
    return when (precision) {
        ChronoUnit.DAYS -> LocalDate.from(this).compareTo(LocalDate.from(other))
        else -> LocalDateTime.from(this).compareTo(LocalDateTime.from(other))
    }
}
