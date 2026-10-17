package org.shawnz.dailytracker.data

import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.parse.ResultParser
import org.shawnz.dailytracker.time.RolloverRule
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/** A game's details, whether from the catalog or typed by the user. */
interface GameSource {
    val title: String
    val url: String
}

/** A game the user typed in. */
data class CustomGame(
    override val title: String,
    override val url: String,
) : GameSource

/** One tracked game, with its details from [source]. */
data class Game(
    val id: Long,
    val source: GameSource,
    val sortOrder: Int,
    val archived: Boolean,
    val remindersEnabled: Boolean,
) {
    val title: String get() = source.title
    val url: String get() = source.url

    /** The catalog entry for this game. Null for a user-made game. */
    val catalogGame: CatalogGame? get() = source as? CatalogGame

    /** Every address for this game. A user-made game has only the one. */
    val urls: List<String> get() = catalogGame?.urls ?: listOf(url)

    /** Null when no parser exists for this game. */
    val parser: ResultParser<*>? get() = catalogGame?.parser

    val rolloverRule: RolloverRule get() = catalogGame?.rolloverRule ?: RolloverRule.DEFAULT

    fun possiblePuzzleDays(rolloverObserved: Boolean): ClosedRange<LocalDate> = rolloverRule.possiblePuzzleDays(rolloverObserved)

    /**
     * Returns whether [entries] contains a result of this game for the day after the date in its
     * rule's zone. Such a result is evidence that the next puzzle is out.
     */
    fun rolloverObserved(entries: List<EntryEntity>): Boolean {
        if (rolloverRule.time != null) return false
        val nextDay = possiblePuzzleDays(rolloverObserved = false).endInclusive
        return entries.any { it.gameId == id && it.puzzleDay == nextDay }
    }

    fun nextRollover(): Instant? = rolloverRule.nextRollover()

    fun nextPossiblePuzzleDaysChange(clock: Clock = Clock.systemDefaultZone()): Instant = rolloverRule.nextPossiblePuzzleDaysChange(clock)
}

/** Fails on a row with neither a catalog game nor a title and URL. */
fun GameEntity.resolve(): Game {
    val source: GameSource =
        catalogGame ?: CustomGame(
            title = requireNotNull(title) { "Game $id has no catalog game and no title" },
            url = requireNotNull(url) { "Game $id has no catalog game and no URL" },
        )
    return Game(
        id = id,
        source = source,
        sortOrder = sortOrder,
        archived = archived,
        remindersEnabled = remindersEnabled,
    )
}
