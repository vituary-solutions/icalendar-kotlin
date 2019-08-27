package com.vituary.icalendar

import java.io.Reader
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.collections.ArrayList

fun Reader.readCalendars() : List<Calendar> {
    return this.readLines().readCalendars()
}

fun List<String>.readCalendars() : List<Calendar> {
    return parseContentLines(this).buildCalendars()
}

private fun parseContentLines(inputLines : List<String>) : List<ContentLine> {
    // Merge content that's broken up into multiple lines, see https://icalendar.org/iCalendar-RFC-5545/3-1-content-lines.html
    val unfoldedLines = inputLines.foldIndexed(ArrayList<Pair<String, Int>>(), { index, unfolded, line ->
        if (line.isNotEmpty()) {
            if (line[0].isWhitespace()) {
                val lastPair = unfolded[unfolded.size - 1]
                unfolded[unfolded.size - 1] = Pair(lastPair.first + line.substring(1), lastPair.second)
            } else {
                unfolded.add(Pair(line, index + 1))
            }
        }
        unfolded
    })

    return unfoldedLines.map { ContentLine(it.first, it.second) }
}

fun List<ContentLine>.buildCalendars() : List<Calendar> {
    val stack : Deque<CalendarComponentBuilder> = LinkedList()
    val result : List<Calendar>
    var calendarContext = CalendarContext()

    result = fold(ArrayList()) { list, contentLine ->
        try {
            when(contentLine.name) {
                BEGIN -> {
                    val componentName = ContentLineToken(contentLine.value)
                    require(stack.isNotEmpty() || componentName == VCALENDAR) { "A $VCALENDAR component was expected but found $componentName" }
                    if (componentName == VCALENDAR) {
                        calendarContext = CalendarContext()
                    }
                    stack.push(CalendarComponentBuilder(ContentLineToken(contentLine.value), calendarContext))
                }
                END -> {
                    val componentName = ContentLineToken(contentLine.value)
                    var builder = stack.pop()
                    require(builder != null && builder.name.equals(componentName)) { "End ${builder.name} component was expected but found ${componentName}" }
                    var component = builder.build()
                    if (stack.isEmpty()) {
                        list.add(component as Calendar)
                    }
                    else {
                        stack.peek().add(component)
                        if (component is CalendarTimeZone) {
                            calendarContext.timeZones.add(component)
                        }
                    }
                }
                else -> {
                    require(stack.isNotEmpty()) { "A $VCALENDAR component was expected but found ${contentLine.name} property" }
                    val property = CalendarPropertyBuilder(contentLine.name, calendarContext).params(contentLine.params).value(contentLine.value).build()
                    stack.peek().add(property)
                    if (property.name == METHOD) {
                        calendarContext.method = property.value
                    }
                }
            }
        }
        catch (ex: Exception) {
            throw IllegalArgumentException("Error processing content at line ${contentLine.lineNumber}: ${ex.message}", ex)
        }

        list
    }
    return result;
}
