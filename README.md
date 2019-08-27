This is a partial implementation of iCalendar ([RFC 5545](https://icalendar.org/RFC-Specifications/iCalendar-RFC-5545/)),
capable of parsing a valid ICS file.  While not all properties or components are explicitly supported, the original input
values are maintained.  As a result it is possible to extend the functionality.

Here's a sample that will parse an ICS file and then print it back out in its
original format: 
```kotlin
import com.vituary.icalendar.*
import java.io.File

fun readAndPrintCalendar(inputFile: File) {
    val calendars = inputFile.reader().readCalendars()
    calendars.forEach {
        println(it)
    }
}
```
