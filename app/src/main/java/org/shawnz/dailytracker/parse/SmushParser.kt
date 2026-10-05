package org.shawnz.dailytracker.parse

/**
 * A Smush result.
 *
 * Smush puzzles have no number, so [puzzleNumber] is always null. Every chunk is worth some
 * points, so [success] is always null.
 *
 * @property badges The game's own words for what went well, in the order printed.
 * @property grid The rows of tiles, one string per row.
 */
data class SmushResult(
    val points: Int?,
    val badges: List<String> = emptyList(),
    val grid: List<String> = emptyList(),
) : GameResult {
    /**
     * A perfect game needs the pangram first, a clean plate and no hints, and its share text has
     * only the PERFECT badge in place of those three.
     */
    val perfect: Boolean get() = has("PERFECT")

    /** Finding the pangram and finding it first are separate badges. */
    val pangram: Boolean get() = perfect || has("pangram")
    val pangramFirst: Boolean get() = perfect || has("pangram first")

    val cleanPlate: Boolean get() = perfect || has("clean plate")
    val noHints: Boolean get() = perfect || has("no hints")
    val iceCold: Boolean get() = has("ICE COLD")

    /** Null below two days, where the streak is not printed. */
    val streakDays: Int? get() =
        badges
            .firstNotNullOfOrNull { STREAK.find(it) }
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()

    private fun has(badge: String) = badges.any { it.contains(badge, ignoreCase = true) }
}

private val STREAK = Regex("""(\d+)-day streak""", RegexOption.IGNORE_CASE)

/**
 * Reads the Smush share text.
 *
 *     hankgreen.com/smush · Sep 14
 *     298 pts · ★★ pangram first
 *     🟩🟨🥞
 *     🥞⭐🥞
 *     🥞🥞🟨
 *
 * The date is written in the device's own language, so it is not read here. A star marks the
 * hub chunk and a pancake marks one that ran out of lives.
 */
object SmushParser : ResultParser<SmushResult> {
    private val HEADER = Regex("""hankgreen\.com/smush""", RegexOption.IGNORE_CASE)
    private val POINTS = Regex("""(\d+)\s*pts""", RegexOption.IGNORE_CASE)

    /** Green, yellow, the pancake of a spent chunk, and the star of the hub. */
    private val TILES = setOf(0x1F7E9, 0x1F7E8, 0x1F95E, 0x2B50)

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): SmushResult? {
        if (!HEADER.containsMatchIn(text)) return null
        val scoreLine = text.lineSequence().firstOrNull { POINTS.containsMatchIn(it) }
        return SmushResult(
            points =
                scoreLine
                    ?.let { POINTS.find(it) }
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull(),
            badges =
                scoreLine
                    ?.split('·')
                    ?.drop(1)
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList(),
            grid =
                text
                    .lineSequence()
                    .map { it.trim() }
                    .filter { isGridRow(it, TILES) }
                    .toList(),
        )
    }
}
