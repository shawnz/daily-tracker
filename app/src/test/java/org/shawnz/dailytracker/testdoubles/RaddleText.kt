package org.shawnz.dailytracker.testdoubles

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val RADDLE_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH)

/**
 * Returns Raddle share text with the header of the puzzle of [day]. Raddle #1 is 2025-02-25.
 *
 * The words and the ladder are those of puzzle 567 for every [day], so the text is real only
 * for 2026-09-14.
 */
fun raddleText(day: LocalDate): String {
    val number = day.toEpochDay() - LocalDate.of(2025, 2, 25).toEpochDay() + 1
    return "COSTA → RICA [💯]\nRaddle #$number • ${day.format(RADDLE_DATE)}\n\n🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🙌"
}
