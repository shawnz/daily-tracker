package org.shawnz.dailytracker.parse

import java.time.Month
import java.time.MonthDay

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
 * Returns the month and day in [text], a match of [MONTH_AND_DAY], or null when no year has
 * that day in that month.
 */
internal fun monthAndDayValue(text: String): MonthDay? {
    val name = text.filter { it.isLetter() }
    val month = MONTHS.indexOfFirst { name.startsWith(it) } + 1
    val dayOfMonth = text.filter { it.isDigit() }.toInt()
    return if (dayOfMonth in 1..Month.of(month).maxLength()) MonthDay.of(month, dayOfMonth) else null
}
