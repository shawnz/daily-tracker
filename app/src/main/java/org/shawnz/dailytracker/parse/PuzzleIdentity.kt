package org.shawnz.dailytracker.parse

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import kotlin.math.abs

/** What is known from a share text about which puzzle was played. */
sealed interface PuzzleIdentity {
    /** A number in the game's daily series. */
    data class DailyNumber(
        val number: Int,
    ) : PuzzleIdentity

    data class Date(
        val date: LocalDate,
    ) : PuzzleIdentity

    /** A date written with no year. */
    data class MonthAndDay(
        val monthDay: MonthDay,
    ) : PuzzleIdentity

    /** The latest [weekday] on or before a date written with no year. */
    data class WeekdayOnOrBefore(
        val weekday: DayOfWeek,
        val monthDay: MonthDay,
    ) : PuzzleIdentity

    /** A daily puzzle not identified in the text. */
    data object UnidentifiedDaily : PuzzleIdentity

    /** A play outside the game's daily series, such as a practice run or a custom level. */
    data object NotDaily : PuzzleIdentity
}

/**
 * The numbers of a daily with one puzzle for each day and no gaps.
 *
 * @property firstNumber The number of the puzzle published on [firstDay].
 */
data class DailyNumbering(
    val firstDay: LocalDate,
    val firstNumber: Int = 1,
) {
    /** The day puzzle [number] was published, or null when the series starts after it. */
    fun dayOf(number: Int): LocalDate? = if (number < firstNumber) null else firstDay.plusDays((number - firstNumber).toLong())

    /** The number of the puzzle published on [day], or null when the series starts after it. */
    fun numberOn(day: LocalDate): Int? = if (day < firstDay) null else firstNumber + ChronoUnit.DAYS.between(firstDay, day).toInt()

    companion object {
        val FROM_MAY_1_2024 = DailyNumbering(firstDay = LocalDate.of(2024, 5, 1))
    }
}

/** The most days a month and day can be from the latest day in [resolveDay]. */
private const val HALF_YEAR_DAYS = 183

/**
 * Returns the puzzle day of this identity. Null when it has no day, its day can't be determined,
 * or the day is after [latest].
 *
 * A number is converted with [numbering]. A month and day is given the year in which it is
 * nearest [latest], if that is within half a year. For a weekday, the day is then the latest
 * such weekday on or before that date.
 */
fun PuzzleIdentity.resolveDay(
    numbering: DailyNumbering?,
    latest: LocalDate,
): LocalDate? =
    when (this) {
        is PuzzleIdentity.DailyNumber -> numbering?.dayOf(number)
        is PuzzleIdentity.Date -> date
        is PuzzleIdentity.MonthAndDay -> monthDay.nearest(latest)
        is PuzzleIdentity.WeekdayOnOrBefore -> monthDay.nearest(latest)?.with(TemporalAdjusters.previousOrSame(weekday))
        PuzzleIdentity.UnidentifiedDaily, PuzzleIdentity.NotDaily -> null
    }?.takeIf { !it.isAfter(latest) }

/**
 * Returns this month and day in the year in which it is nearest [day], or null when that date is
 * more than half a year from [day].
 */
private fun MonthDay.nearest(day: LocalDate): LocalDate? =
    (day.year - 1..day.year + 1)
        .filter { isValidYear(it) }
        .map { atYear(it) }
        .minByOrNull { abs(ChronoUnit.DAYS.between(day, it)) }
        ?.takeIf { abs(ChronoUnit.DAYS.between(day, it)) <= HALF_YEAR_DAYS }
