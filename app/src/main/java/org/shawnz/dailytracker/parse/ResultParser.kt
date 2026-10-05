package org.shawnz.dailytracker.parse

import java.time.LocalDate

/** The parsed share text of one game, with the properties common to all games. */
@Suppress("SameReturnValue") // Overrides in constructor parameters aren't counted.
interface GameResult {
    /** The puzzle number stated in the text, or null when it doesn't state one. */
    val puzzleNumber: String? get() = null

    /** Whether the player solved the puzzle. Null when there is no win or loss, or the text doesn't show it. */
    val success: Boolean? get() = null

    /** The puzzle day stated in the text, or null when it doesn't state a day. */
    val day: LocalDate? get() = null
}

/** Parses the share text of one game into [R]. */
interface ResultParser<out R : GameResult> {
    /** Whether [text] is share text for this game, checked without a full parse. */
    fun matches(text: String): Boolean

    fun parse(text: String): R?
}
