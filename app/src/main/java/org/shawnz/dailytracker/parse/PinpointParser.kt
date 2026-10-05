package org.shawnz.dailytracker.parse

import java.time.LocalDate

/**
 * A Pinpoint result.
 *
 * @property guesses The guesses taken. Null for a failed play.
 * @property total The guesses allowed.
 */
data class PinpointResult(
    override val puzzleNumber: String?,
    val guesses: Int?,
    val total: Int,
    override val success: Boolean?,
    override val day: LocalDate? = null,
) : GameResult

/**
 * Reads the Pinpoint share text, which has one of two layouts.
 *
 *     Pinpoint #873
 *     🤔 🤔 🤔 🤔 📌 (5/5)
 *     lnkd.in/pinpoint.
 *
 * Each wrong guess is a thinking face, the right one a pin, and each guess not needed a blank
 * square. A failed play has (X/5).
 *
 *     Pinpoint #2 | 3 guesses
 *     1️⃣ | 13% match
 *     2️⃣ | 79% match
 *     3️⃣ | 100% match 📌
 *     lnkd.in/pinpoint.
 *
 * Signed in, each guess has a line, and the right one ends in a pin. A failed play doesn't have a
 * pin.
 *
 * Pinpoint is scored by guesses and doesn't have a time, so it doesn't use [LinkedInParser].
 * From #1,000 the number is written with the locale's digit grouping.
 */
object PinpointParser : ResultParser<PinpointResult> {
    // Puzzle #1, worked back from the puzzle ID shown for one day.
    private val FIRST_DAY: LocalDate = LocalDate.of(2024, 5, 1)

    private val header = Regex("""Pinpoint\s*#($GROUPED_NUMBER)""", RegexOption.IGNORE_CASE)
    private val score = Regex("""\(\s*(\d+|X)\s*/\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)
    private val guessLine = Regex("""\|\s*\d+%\s*match(\s*📌)?""", RegexOption.IGNORE_CASE)

    override fun matches(text: String): Boolean = header.containsMatchIn(text)

    override fun parse(text: String): PinpointResult? {
        val found = header.find(text) ?: return null
        val number = groupedNumberValue(found.groupValues[1])?.toString()
        val scored = score.find(text)
        val lines = guessLine.findAll(text).toList()
        val pinned = lines.any { it.groupValues[1].isNotEmpty() }
        val guesses =
            when {
                scored != null -> scored.groupValues[1].toIntOrNull()
                pinned -> lines.size
                else -> null
            }
        return PinpointResult(
            puzzleNumber = number,
            guesses = guesses,
            total = scored?.groupValues?.get(2)?.toIntOrNull() ?: 5,
            success =
                when {
                    scored != null -> guesses != null
                    lines.isNotEmpty() -> pinned
                    else -> null
                },
            day = dayFromNumber(number, FIRST_DAY),
        )
    }
}
