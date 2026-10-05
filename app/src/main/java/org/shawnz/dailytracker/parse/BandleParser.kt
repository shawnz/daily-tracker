package org.shawnz.dailytracker.parse

/**
 * A Bandle result.
 *
 * @property step The guess the song was named on. Null when it was never named.
 * @property instruments How many instruments the song has, which is how many guesses were
 *   available.
 * @property emoji One emoji per guess slot, in order. An unreached slot is ⬜.
 * @property bonusWon Bonus rounds won. Null when none was shown.
 * @property bonusAsked Bonus rounds asked. Null when none was shown.
 */
data class BandleResult(
    override val puzzleNumber: String?,
    val step: Int?,
    val instruments: Int?,
    val emoji: String = "",
    val bonusWon: Int? = null,
    val bonusAsked: Int? = null,
) : GameResult {
    override val success: Boolean get() = step != null
}

/**
 * Reads the Bandle share text.
 *
 *     Bandle #1489 3/6
 *     🟥🟨🟩⬜⬜⬜
 *     Found: 1/1 (100%)
 *     Current Streak: 1 (max 1)
 *     #Bandle
 *     https://bandle.app
 *
 * When the song was never named, the guess is `x`.
 *
 * The labels are in the player's language, so they are not read. The bonus line is found by its
 * emoji and the rest by shape.
 */
object BandleParser : ResultParser<BandleResult> {
    private val HEADER =
        Regex(
            """Bandle\s*#(\d+)\s+(x|\d+)\s*/\s*(\d+)""",
            RegexOption.IGNORE_CASE,
        )

    /**
     * 🟥 wrong, 🟨 the right artist and the wrong song, 🟩 correct, ⬛ a turn skipped, and
     * ⬜ a turn never reached.
     */
    private val EMOJI = setOf(0x1F7E5, 0x1F7E8, 0x1F7E9, 0x2B1B, 0x2B1C)

    /** One emoji for each kind of bonus round. */
    private val BONUS_EMOJI =
        setOf(
            0x1F5BC,
            0x1F9D1,
            0x1F30D,
            0x1F9E9,
            0x1F4C5,
            0x1F4BF,
            0x23F1,
            0x1F3B8,
        )

    private val COUNT = Regex("""(\d+)\s*/\s*(\d+)""")

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): BandleResult? {
        val header = HEADER.find(text) ?: return null
        val bonus =
            text
                .lineSequence()
                .map { it.trim() }
                .firstOrNull { line -> line.codePoints().anyMatch { it in BONUS_EMOJI } }
                ?.let { COUNT.find(it) }
        return BandleResult(
            puzzleNumber = header.groupValues[1],
            step = header.groupValues[2].toIntOrNull(),
            instruments = header.groupValues[3].toIntOrNull(),
            emoji =
                text
                    .lineSequence()
                    .map { it.trim() }
                    .firstOrNull { isGridRow(it, EMOJI) }
                    .orEmpty(),
            bonusWon = bonus?.groupValues?.get(1)?.toIntOrNull(),
            bonusAsked = bonus?.groupValues?.get(2)?.toIntOrNull(),
        )
    }
}
