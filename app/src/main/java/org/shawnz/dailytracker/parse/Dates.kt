package org.shawnz.dailytracker.parse

import java.time.LocalDate
import java.time.YearMonth

private val MONTHS =
    listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private const val MONTH = """(?:Jan|Feb|Mar|Apr|May|June?|July?|Aug|Sept?|Oct|Nov|Dec)"""

/**
 * A regex fragment for a month and a day of the month with no year, in the form of any English
 * locale, as formatted by JavaScript's `toLocaleDateString()` with a short month: `Sep 14`,
 * `14 Sep`, `14 Sept`, `4 July`, and `04 Sept` with a padded day.
 */
internal const val MONTH_AND_DAY = """(?:$MONTH\s+\d{1,2}|\d{1,2}\s+$MONTH)"""

/**
 * Returns the day for [text], a match of [MONTH_AND_DAY], or null when the month has no such
 * day. The year is the latest one where the day is not after the day following [today].
 */
internal fun monthAndDayValue(
    text: String,
    today: LocalDate,
): LocalDate? {
    val name = text.filter { it.isLetter() }
    val month = MONTHS.indexOfFirst { name.startsWith(it) } + 1
    val dayOfMonth = text.filter { it.isDigit() }.toInt()
    val last = today.plusDays(1)
    // Leap days are at most eight years apart.
    return (last.year downTo last.year - 8).firstNotNullOfOrNull { year ->
        if (YearMonth.of(year, month).isValidDay(dayOfMonth)) {
            LocalDate.of(year, month, dayOfMonth).takeIf { !it.isAfter(last) }
        } else {
            null
        }
    }
}
