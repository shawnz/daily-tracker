package org.shawnz.dailytracker.parse

/** The parsed share text of one game. */
interface GameResult {
    /** What is known from the share text about which puzzle was played. */
    val puzzle: PuzzleIdentity
}

/** Parses the share text of one game into [R]. */
interface ResultParser<out R : GameResult> {
    /** Whether [text] is share text for this game, checked without a full parse. */
    fun matches(text: String): Boolean

    fun parse(text: String): R?
}
