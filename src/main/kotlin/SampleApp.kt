package com.vituary.icalendar

import java.io.File
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.Temporal

fun main(args: Array<String>) {
    val input : File
    val startDate : LocalDate
    val endDate : LocalDate

    try {
        require(args.isNotEmpty()) { "arguments is empty" }
        input = File(args[0])
        require(input.exists()) { "File not found: $input" }
        startDate = if (args.size >= 2) LocalDate.parse(args[1], DATE_FORMATTER) else LocalDate.now()
        endDate = if (args.size >= 3) LocalDate.parse(args[2], DATE_FORMATTER) else startDate.plusWeeks(1)

        println("Displaying calendar events from $input with the date range starting $startDate and ending before $endDate")
        val calendars : List<Calendar> = input.reader().readCalendars()
        val displayEventsByDate = calendars.flatMap { calendar ->
            calendar.events.flatMap { event ->
                event.recurrences?.map { DisplayEvent(event, it) } ?: listOf(DisplayEvent(event, event.start!!))
            }.filter {
                localZonedDate(it.start) in startDate..endDate.minusDays(1)
            }
        }.groupBy {
            localZonedDate(it.start)
        }.toSortedMap()
        displayEventsByDate.forEach { entry ->
            println("---------- Events for ${entry.key} ----------")
            val datePartition = entry.value.partition { it.start is LocalDate }
            (datePartition.first + datePartition.second.sortedBy { LocalDateTime.from(it.start) }).forEach {
                printEvent(it)
                println()
            }
        }
    }
    catch (ex : Exception) {
        println("Application failed: $ex")
        ex.printStackTrace()
        println()
        println("EXPECTED PARAMETERS: <inputFile> [startDate] [endDate]\n\tDATE FORMAT: $DATE_FORMAT")
    }
}

private data class DisplayEvent(val event: Event, val start: Temporal)

private fun localZonedDate(temporal: Temporal): LocalDate {
    return if (temporal is LocalDate) temporal else LocalDate.from(ZonedDateTime.from(temporal).withZoneSameInstant(ZoneId.systemDefault()))
}

private fun printEvent(display : DisplayEvent) {
    var df = if (display.event.dateType == FlexDateTimeType.DATE) DateTimeFormatter.ofPattern("MMMM dd, yyyy") else DateTimeFormatter.ofPattern("MMMM dd, yyyy, HH:mm").withZone(ZoneId.systemDefault())
    println("SUMMARY: ${display.event.summary}")
    println("LOCATION: ${display.event.location}")
    println("START: ${df.format(display.start)}")
    if (display.event.end != null) {
        println("END: ${df.format(display.start.plus(
                    if (display.start is LocalDate) Period.between(LocalDate.from(display.event.start), LocalDate.from(display.event.end))
                    else Duration.between(display.event.start, display.event.end)
                ))}")
    }
    println("DESCRIPTION: ${display.event.description}")
}
