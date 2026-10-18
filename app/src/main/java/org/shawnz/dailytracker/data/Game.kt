package org.shawnz.dailytracker.data

import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.parse.ResultParser
import org.shawnz.dailytracker.time.RolloverRule
import java.time.Instant
import java.time.LocalDate
import java.time.ZonedDateTime

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

    /**
     * Returns the range of days the puzzle day can be at [now]. A result of this game in [entries]
     * is evidence that the puzzle for its day is out, so the range starts no earlier than the
     * latest such day.
     */
    fun possiblePuzzleDays(
        entries: List<EntryEntity>,
        now: ZonedDateTime = ZonedDateTime.now(),
    ): ClosedRange<LocalDate> {
        val possible = rolloverRule.possiblePuzzleDays(now)
        val lastPlayed = entries.filter { it.gameId == id && it.puzzleDay in possible }.maxOfOrNull { it.puzzleDay }
        return (lastPlayed ?: possible.start)..possible.endInclusive
    }

    /**
     * Returns this game's entry in [entries] for the start of [possiblePuzzleDays] at [now], or
     * null if there is none.
     */
    fun currentEntry(
        entries: List<EntryEntity>,
        now: ZonedDateTime = ZonedDateTime.now(),
    ): EntryEntity? {
        val day = possiblePuzzleDays(entries, now).start
        return entries.firstOrNull { it.gameId == id && it.puzzleDay == day }
    }

    /**
     * Returns this game's [TodayStatus] at [now], from [entries]. The date an entry was saved on is
     * taken in its [EntryEntity.completedZone], or in the zone of [now] if it has none.
     */
    fun todayStatus(
        entries: List<EntryEntity>,
        now: ZonedDateTime = ZonedDateTime.now(),
    ): TodayStatus {
        val entry = currentEntry(entries, now)
        val nextRollover = rolloverRule.nextRollover(now)
        return when {
            entry != null && entry.zonedCompletedAt(now.zone).toLocalDate() == now.toLocalDate() -> {
                TodayStatus.Done(entry)
            }

            entry != null && nextRollover != null -> {
                TodayStatus.Unreleased(nextRollover)
            }

            else -> {
                TodayStatus.Unplayed(nextRollover)
            }
        }
    }

    fun nextPossiblePuzzleDaysChange(now: ZonedDateTime = ZonedDateTime.now()): Instant = rolloverRule.nextPossiblePuzzleDaysChange(now)
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
