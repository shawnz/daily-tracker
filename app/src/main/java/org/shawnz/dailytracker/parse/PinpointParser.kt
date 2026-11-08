package org.shawnz.dailytracker.parse

/**
 * A Pinpoint result.
 *
 * @property guesses The guesses taken. Null for a failed play.
 * @property total The guesses allowed.
 * @property success Whether the puzzle was solved. Null when the text doesn't show it.
 */
data class PinpointResult(
    val number: Int,
    val guesses: Int?,
    val total: Int,
    val success: Boolean?,
) : GameResult {
    override val puzzle: PuzzleIdentity get() = PuzzleIdentity.DailyNumber(number)
}

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
    private val header = Regex("""Pinpoint\s*#($GROUPED_NUMBER)""", RegexOption.IGNORE_CASE)
    private val score = Regex("""\(\s*(\d+|X)\s*/\s*(\d+)\s*\)""", RegexOption.IGNORE_CASE)
    private val guessLine = Regex("""\|\s*\d+%\s*match(\s*📌)?""", RegexOption.IGNORE_CASE)

    override fun matches(text: String): Boolean = header.containsMatchIn(text)

    override fun parse(text: String): PinpointResult? {
        val found = header.find(text) ?: return null
        val number = groupedNumberValue(found.groupValues[1]) ?: return null
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
            number = number,
            guesses = guesses,
            total = scored?.groupValues?.get(2)?.toIntOrNull() ?: 5,
            success =
                when {
                    scored != null -> guesses != null
                    lines.isNotEmpty() -> pinned
                    else -> null
                },
        )
    }
}
