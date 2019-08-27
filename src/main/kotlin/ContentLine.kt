package com.vituary.icalendar

import java.lang.IllegalArgumentException

val IANA_TOKEN_FORMAT = "[A-Za-z\\d-]+"
val IANA_TOKEN_REGEX = Regex("^$IANA_TOKEN_FORMAT$")

/**
 * Regular expression that captures values as follows: "<name>;<params>:<value>"
 * - name: ^[A-Za-z\d-]+
 * - params: "<name>=<value>[;]" - may have many params, each starting with ';'
 *   - name: [A-Za-z\d-]+
 *   - value: (?:(?:[^;:,\"]*|(?:\".*\")),?)* - may have multiple comma separate values with each one being either quoted \".*\" or non-quoted [^;:,\"]
 * - value: .*$
 */
val LINE_REGEX = "^($IANA_TOKEN_FORMAT)(?:;((?:$IANA_TOKEN_FORMAT=(?:(?:[^;:,\\\"]*|(?:\\\".*\\\")),?)*;?)*))?:(.*)\$".toRegex()

// TODO exclude lineNumber from equals/hashcode
data class ContentLine (val name: ContentLineToken, val value: String, val params: Map<ContentLineToken, String>, val lineNumber: Int)

fun ContentLine(text: String, lineNumber: Int = 0) : ContentLine {
    val matches = LINE_REGEX.matchEntire(text)
    require (matches != null) { "Unable to parse content line: $text" }

    val name = ContentLineToken(matches.groupValues[1])
    val value = matches.groupValues[3]
    val params = matches.groupValues[2].splitCalendarValueToMap().mapKeys { ContentLineToken(it.key) }

    return ContentLine(name, value, params, lineNumber)
}

data class ContentLineToken (val value: String) {
    init {
        require(value.matches(IANA_TOKEN_REGEX)) { "Invalid token: \"$value\"" }
    }
    override fun toString() = value.toUpperCase()
    override fun equals(other: Any?) = (other is ContentLineToken) && value.equals(other.value, true)
    override fun hashCode(): Int = value.toUpperCase().hashCode()
}
