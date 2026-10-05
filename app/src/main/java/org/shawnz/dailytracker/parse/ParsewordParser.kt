package org.shawnz.dailytracker.parse

import java.time.LocalDate

/** The day of puzzle #1. There is one puzzle a day. */
private val FIRST_DAY: LocalDate = LocalDate.of(2026, 1, 29)

/**
 * A Parseword result.
 *
 * @property seconds Null when the text doesn't include a time.
 * @property assists The number of hints taken. 0 for a perfect run or one with no assists, and
 *   null when the text doesn't give a number.
 * @property secretsFound The number of secrets found, or of eggs found in an Egg Hunt.
 * @property learnMode Played in Learn Mode. Neither this nor [challengeMode] is set at the
 *   default or relaxed difficulty.
 */
data class ParsewordResult(
    override val puzzleNumber: String?,
    val seconds: Int?,
    val perfect: Boolean,
    val assists: Int?,
    val eggHunt: Boolean = false,
    val secretsFound: Int = 0,
    val easterEggs: Int = 0,
    val learnMode: Boolean = false,
    val challengeMode: Boolean = false,
) : GameResult {
    /** Share text exists only for a finished puzzle. */
    override val success: Boolean get() = true

    /** A puzzle with an id in place of a number is outside the daily run and has no day. */
    override val day: LocalDate?
        get() = dayFromNumber(puzzleNumber, FIRST_DAY)

    /** A puzzle finished inside a minute is marked with a lightning bolt. */
    val underAMinute: Boolean get() = seconds != null && seconds <= 60
}

/**
 * Reads the Parseword share text.
 *
 *     Parseword #229
 *     ⏱️ 1m23s
 *     💎 Perfect
 *     🎭 Secret Found
 *     🗿 Challenge Mode
 *
 * An Egg Hunt doesn't have the assists line, and lists the eggs found with no "Found". A puzzle
 * outside the daily run has its id in quotes instead of a number. Texts from before 2026-05-18
 * say "Hints" instead of "Assists".
 */
object ParsewordParser : ResultParser<ParsewordResult> {
    private val HEADER =
        Regex(
            """Parseword\s+(?:#(\d+)|"([^"]+)")""",
            RegexOption.IGNORE_CASE,
        )

    /** A time is written `1m23s` from a minute up, and `45s` below one. */
    private val TIME = Regex("""(?:(\d+)m)?(\d+)s""")

    private val PERFECT = Regex("""💎\s*Perfect""", RegexOption.IGNORE_CASE)
    private val NO_ASSISTS = Regex("""No (?:Assists|Hints)""", RegexOption.IGNORE_CASE)
    private val ASSISTS = Regex("""✅\s*(\d+)\s+(?:Assists?|Hints?)""", RegexOption.IGNORE_CASE)
    private val EGG_HUNT = Regex("""\bEgg Hunt\b""", RegexOption.IGNORE_CASE)
    private val SECRETS = Regex("""^(.*?)\s*Secrets?\s+Found\s*$""", RegexOption.IGNORE_CASE)
    private val EASTER_EGGS = Regex("""(\d+)\s+Easter\s+Eggs?\s+Found""", RegexOption.IGNORE_CASE)
    private val LEARN_MODE = Regex("""Learn Mode""", RegexOption.IGNORE_CASE)
    private val CHALLENGE_MODE = Regex("""Challenge Mode""", RegexOption.IGNORE_CASE)

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): ParsewordResult? {
        val header = HEADER.find(text) ?: return null
        val perfect = PERFECT.containsMatchIn(text)
        val eggHunt = EGG_HUNT.containsMatchIn(text)
        return ParsewordResult(
            puzzleNumber =
                header.groupValues[1]
                    .ifEmpty { header.groupValues[2] }
                    .ifEmpty { null },
            seconds =
                TIME.find(text)?.let {
                    val minutes = it.groupValues[1].toIntOrNull() ?: 0
                    val seconds = it.groupValues[2].toIntOrNull() ?: return@let null
                    minutes * 60 + seconds
                },
            perfect = perfect,
            assists =
                when {
                    perfect || NO_ASSISTS.containsMatchIn(text) -> {
                        0
                    }

                    else -> {
                        ASSISTS
                            .find(text)
                            ?.groupValues
                            ?.get(1)
                            ?.toIntOrNull()
                    }
                },
            eggHunt = eggHunt,
            secretsFound = if (eggHunt) countHuntEggs(text) else countSecrets(text),
            easterEggs =
                EASTER_EGGS
                    .find(text)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull() ?: 0,
            learnMode = LEARN_MODE.containsMatchIn(text),
            challengeMode = CHALLENGE_MODE.containsMatchIn(text),
        )
    }

    /** The number of emoji on the secrets line, which doesn't include a number of its own. */
    private fun countSecrets(text: String): Int =
        text
            .lineSequence()
            .firstNotNullOfOrNull { SECRETS.find(it.trim())?.groupValues?.get(1) }
            ?.let(::countEmoji)
            ?: 0

    /** The eggs are on the first line after "Egg Hunt" that isn't the time. */
    private fun countHuntEggs(text: String): Int {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val hunt = lines.indexOfFirst { EGG_HUNT.containsMatchIn(it) }
        val eggs = lines.drop(hunt + 1).firstOrNull { !TIME.containsMatchIn(it) } ?: return 0
        if (LEARN_MODE.containsMatchIn(eggs) || CHALLENGE_MODE.containsMatchIn(eggs)) return 0
        return countEmoji(eggs)
    }

    private fun countEmoji(line: String): Int = line.split(' ').count { it.isNotBlank() }
}
