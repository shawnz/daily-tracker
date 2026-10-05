package org.shawnz.dailytracker.testdoubles

import java.time.LocalDate
import java.util.Locale

/**
 * Returns Wordle share text for the puzzle of [day], solved on the first guess, which is possible
 * whatever the answer. Wordle #0 is 2021-06-19.
 */
fun wordleText(day: LocalDate): String {
    val number = day.toEpochDay() - LocalDate.of(2021, 6, 19).toEpochDay()
    return "Wordle %,d 1/6\n\n🟩🟩🟩🟩🟩".format(Locale.US, number)
}
