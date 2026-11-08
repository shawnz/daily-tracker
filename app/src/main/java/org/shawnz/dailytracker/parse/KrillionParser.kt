package org.shawnz.dailytracker.parse

/** What the game calls a day's score, from the shallowest band to the deepest. */
private val BANDS =
    listOf(
        450 to "One in a Krillion",
        351 to "Deep Cut",
        251 to "Rare",
        151 to "Schooler",
        0 to "Plankton",
    )

private const val MISS = '⬛'

/**
 * A Krillion result.
 *
 * @property number The daily's number. Null for a run from unlimited or a themed pack, where the
 *   number is a count of runs and not of days.
 * @property score The day's total points.
 * @property emoji One emoji per prompt. 🫧 plankton, 🤡 too clever, 🐟 schooler, 🦑 rare,
 *   🏮 deep cut, 🌟 one in a krillion, ⬛ a miss.
 * @property band The game's own word for the score, such as Schooler.
 */
data class KrillionResult(
    val number: Int?,
    val score: Int?,
    val emoji: String = "",
) : GameResult {
    override val puzzle: PuzzleIdentity get() = number?.let(PuzzleIdentity::DailyNumber) ?: PuzzleIdentity.NotDaily

    val misses: Int get() = emoji.count { it == MISS }

    val prompts: Int get() = emoji.codePointCount(0, emoji.length)

    val band: String? get() = score?.let { s -> BANDS.first { s >= it.first }.second }
}

/**
 * Reads the Krillion share text.
 *
 *     Krillion #61 🦐
 *     150
 *
 *     🐟🐟🐟🫧🐟🫧🫧
 *
 * Each part is found by its shape, so a share reflowed onto one line still parses.
 *
 * An archive day has `⟲` before the number. An unlimited run has `∞`, and a themed pack has
 * its own badge, such as `🎬`.
 */
object KrillionParser : ResultParser<KrillionResult> {
    private const val TIERS = "🫧🤡🐟🦑🏮🌟⬛"

    private val HEADER =
        Regex(
            """Krillion\s*(⟲|∞|🎬|🏆|🧭|✒\x{FE0F}?)?\s*#(\d+)\s*🦐\s*(\d+)""",
            RegexOption.IGNORE_CASE,
        )

    private val EMOJI_RUN = Regex("""[$TIERS]+""")

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): KrillionResult? {
        val header = HEADER.find(text) ?: return null
        val sign = header.groupValues[1]
        val daily = sign.isEmpty() || sign == "⟲"
        return KrillionResult(
            number = header.groupValues[2].toIntOrNull()?.takeIf { daily },
            score = header.groupValues[3].toIntOrNull(),
            emoji =
                EMOJI_RUN
                    .findAll(text)
                    .map { it.value }
                    .maxByOrNull { it.length }
                    .orEmpty(),
        )
    }
}
