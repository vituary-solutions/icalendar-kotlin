package com.vituary.icalendar

import java.time.*
import java.time.temporal.TemporalAdjusters
import kotlin.test.assertEquals
import kotlin.test.Test

class RecurrenceGeneratorTest {
    @Test
    fun `first workday of each month`() {
        val startDate = LocalDate.of(2019, 8, 22)
        val recurrenceRule = RecurrenceRuleProperty("FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=1;COUNT=4")
        val generator = RecurrenceGenerator(recurrenceRule, startDate)

        var expectedList = mutableListOf(startDate)
        for (i in 1..3) {
            var nextDate = expectedList.last().plusMonths(1).withDayOfMonth(1)
            if (nextDate.dayOfWeek in DayOfWeek.SATURDAY..DayOfWeek.SUNDAY) {
                nextDate = nextDate.plusDays((8 - nextDate.dayOfWeek.value).toLong())
            }
            expectedList.add(nextDate)
        }

        val actual = generator.calculateRecurrences()

        assertEquals(expectedList, actual)
    }

    @Test fun `last workday of each month`() {
        val startDate = LocalDate.of(2019, 8, 30)
        val recurrenceRule = RecurrenceRuleProperty("FREQ=MONTHLY;BYDAY=MO,TU,WE,TH,FR;BYSETPOS=-1;UNTIL=20191130")
        val generator = RecurrenceGenerator(recurrenceRule, startDate)

        var expectedList = mutableListOf(startDate)
        for (i in 1..3) {
            var nextDate = expectedList.last().plusMonths(1).with(TemporalAdjusters.lastDayOfMonth())
            if (nextDate.dayOfWeek in DayOfWeek.SATURDAY..DayOfWeek.SUNDAY) {
                nextDate = nextDate.minusDays((nextDate.dayOfWeek.value - 5).toLong())
            }
            expectedList.add(nextDate)
        }

        val actual = generator.calculateRecurrences()

        assertEquals(expectedList, actual)
    }

    @Test fun `twice every sunday in January bi-yearly`() {
        val startDate = ZonedDateTime.of(1997, 1,5, 8, 30, 0, 0, ZoneId.of("America/New_York"))
        val recurrenceRule = RecurrenceRuleProperty("FREQ=YEARLY;INTERVAL=2;BYMONTH=1;BYDAY=SU;BYHOUR=8,9;BYMINUTE=30")
        val generator = RecurrenceGenerator(recurrenceRule, startDate)

        val firstSunday1999 = startDate.plusYears(2).with(TemporalAdjusters.firstInMonth(DayOfWeek.SUNDAY))
        val firstSunday2001 = firstSunday1999.plusYears(2).with(TemporalAdjusters.firstInMonth(DayOfWeek.SUNDAY))
        val expected = listOf(
                startDate, startDate.plusWeeks(1), startDate.plusWeeks(2), startDate.plusWeeks(3),
                firstSunday1999, firstSunday1999.plusWeeks(1), firstSunday1999.plusWeeks(2), firstSunday1999.plusWeeks(3), firstSunday1999.plusWeeks(4),
                firstSunday2001, firstSunday2001.plusWeeks(1), firstSunday2001.plusWeeks(2), firstSunday2001.plusWeeks(3)
        ).flatMap { listOf(it, it.plusHours(1)) }

        val actual = generator.calculateRecurrences(LocalDate.of(2001, Month.JANUARY, 1))

        assertEquals(expected, actual)
    }

    @Test fun `yearly on every 5th day in april using BYYEARDAY limiting to thursday and friday`() {
        val startDate = LocalDate.of(2019, 3,23)
        val recurrenceRule = RecurrenceRuleProperty("FREQ=YEARLY;BYYEARDAY=100;BYMONTHDAY=5,10,15,20,25,30;BYDAY=SA,SU;COUNT=5")
        val generator = RecurrenceGenerator(recurrenceRule, startDate)

        val expected = listOf(
                startDate, LocalDate.of(2019, 4,20), LocalDate.of(2020, 4,5),
                LocalDate.of(2020, 4,25), LocalDate.of(2021, 4,10)
        )

        val actual = generator.calculateRecurrences(LocalDate.of(2021, 3,23))

        assertEquals(expected, actual)
    }

    @Test fun `yearly in the 15th and 20th weeks on Saturday and Sunday with weeks starting on Sunday`() {
        val startDate = LocalDate.of(2019, 3,23)
        val recurrenceRule = RecurrenceRuleProperty("FREQ=YEARLY;BYWEEKNO=15,20;BYDAY=SA,SU;WKST=SU;COUNT=8")
        val generator = RecurrenceGenerator(recurrenceRule, startDate)

        val expected = listOf(
                startDate, LocalDate.of(2019, 4,7), LocalDate.of(2019, 4,13),
                LocalDate.of(2019, 5,12), LocalDate.of(2019, 5,18),
                LocalDate.of(2020, 4,5), LocalDate.of(2020, 4,11),
                LocalDate.of(2020, 5,10)
        )

        val actual = generator.calculateRecurrences()

        assertEquals(expected, actual)
    }

    @Test fun `yearly on the first and last three sundays`() {
        val startDate = LocalDate.of(2019, 1,7)
        val recurrenceRule = RecurrenceRuleProperty("FREQ=YEARLY;BYDAY=SU;BYSETPOS=1,2,3,-1,-2,-3;WKST=SU;COUNT=11")
        val generator = RecurrenceGenerator(recurrenceRule, startDate)

        val expected = listOf(
                startDate, LocalDate.of(2019, 1,13), LocalDate.of(2019, 1,20),
                LocalDate.of(2019, 12,15), LocalDate.of(2019, 12,22),
                LocalDate.of(2019, 12,29), LocalDate.of(2020, 1,5),
                LocalDate.of(2020, 1,12), LocalDate.of(2020, 1,19),
                LocalDate.of(2020, 12,13), LocalDate.of(2020, 12,20)
        )

        val actual = generator.calculateRecurrences()

        assertEquals(expected, actual)
    }
}