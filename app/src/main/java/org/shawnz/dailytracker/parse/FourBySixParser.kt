package org.shawnz.dailytracker.parse

/**
 * A 4×6 result.
 *
 * @property medal The game's own emoji for how the board was cleared. 🛟 spoiled, 💯 perfect,
 *   🥇 within par, 🥈 within two of par, ✅ otherwise. 🥇 and 🥈 need a run without tools.
 * @property streakDays Null below two days, where it is not printed.
 * @property grid The shelves, one string per row.
 */
data class FourBySixResult(
    val moves: Int?,
    val par: Int?,
    val medal: String?,
    val points: Int?,
    val streakDays: Int?,
    val grid: List<String> = emptyList(),
) : GameResult {
    /** Share text exists only for a cleared board. */
    override val success: Boolean get() = true

    /** The board was cleared after the answer was shown. */
    val spoiled: Boolean get() = medal == "🛟"

    /** Cleared in the fewest moves the board allows. */
    val perfect: Boolean get() = medal == "💯"
}

/**
 * Reads the 4×6 share text.
 *
 *     4×6 · Sun Sep 13
 *     🟨🟨🟨🟨
 *     🟧🟧🟧🟧
 *     🟩🟩🟩🟩
 *     🟦🟦🟦🟦
 *     🟪🟪🟪🟪
 *     🟥🟥🟥🟥
 *     21/22 moves 🥇 · 259 pts · 🔥4
 *     hankgreen.com/4x6
 *
 * The date has no year, so it is not read here. The streak is shown from two days up.
 */
object FourBySixParser : ResultParser<FourBySixResult> {
    private val HEADER = Regex("""4\s*[×x]\s*6\s*·""", RegexOption.IGNORE_CASE)
    private val MOVES = Regex("""(\d+)/(\d+)\s+moves\s*(\S+)?""", RegexOption.IGNORE_CASE)
    private val POINTS = Regex("""(\d+)\s*pts""", RegexOption.IGNORE_CASE)
    private val STREAK = Regex("""🔥\s*(\d+)""")

    /** The six candy colours, and the white of an empty slot. */
    private val TILES = setOf(0x1F7E8, 0x1F7E7, 0x1F7E9, 0x1F7E6, 0x1F7EA, 0x1F7E5, 0x2B1C)

    override fun matches(text: String): Boolean = HEADER.containsMatchIn(text)

    override fun parse(text: String): FourBySixResult? {
        if (!HEADER.containsMatchIn(text)) return null
        val moves = MOVES.find(text)
        return FourBySixResult(
            moves = moves?.groupValues?.get(1)?.toIntOrNull(),
            par = moves?.groupValues?.get(2)?.toIntOrNull(),
            medal = moves?.groupValues?.get(3)?.takeIf { it.isNotBlank() },
            points =
                POINTS
                    .find(text)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull(),
            streakDays =
                STREAK
                    .find(text)
                    ?.groupValues
                    ?.get(1)
                    ?.toIntOrNull(),
            grid =
                text
                    .lineSequence()
                    .map { it.trim() }
                    .filter { isGridRow(it, TILES) }
                    .toList(),
        )
    }
}
