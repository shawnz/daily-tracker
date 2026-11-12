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
    val rolloverRule: RolloverRule
}

/** A game the user typed in. */
data class CustomGame(
    override val title: String,
    override val url: String,
) : GameSource {
    override val rolloverRule: RolloverRule get() = RolloverRule.DEFAULT
}

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

    val rolloverRule: RolloverRule get() = source.rolloverRule

    /**
     * Returns the address to open to play the puzzle for [day], given its [status] and the game's
     * [possiblePuzzleDays]. Null when the puzzle can't be played.
     *
     * For an unplayed puzzle, this is [url] when [day] is the only possible puzzle day, because the
     * current puzzle is there. Otherwise it is that day's puzzle address, or [url] when the game has
     * none. For a missed puzzle, it is that day's puzzle address, and null when the game has none,
     * because [url] shows a later puzzle.
     */
    fun playUrl(
        day: LocalDate,
        status: PuzzleStatus,
        possiblePuzzleDays: ClosedRange<LocalDate>,
    ): String? =
        when (status) {
            is PuzzleStatus.Unplayed -> {
                val onlyPossibleDay = possiblePuzzleDays.start == day && possiblePuzzleDays.endInclusive == day
                (if (onlyPossibleDay) null else catalogGame?.dayUrl(day)) ?: url
            }

            is PuzzleStatus.Missed -> {
                catalogGame?.dayUrl(day)
            }

            is PuzzleStatus.Done, is PuzzleStatus.Unreleased -> {
                null
            }
        }

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

    /** Returns the [PuzzleStatus] of this game's puzzle for [day] at [now], from [entries]. */
    fun statusOn(
        day: LocalDate,
        entries: List<EntryEntity>,
        now: ZonedDateTime = ZonedDateTime.now(),
    ): PuzzleStatus {
        val entry = entries.firstOrNull { it.gameId == id && it.puzzleDay == day }
        if (entry != null) return PuzzleStatus.Done(entry)
        val possible = possiblePuzzleDays(entries, now)
        val nextRollover = rolloverRule.nextRollover(now)
        val currentPuzzleUnplayed =
            listOf(possible.start, possible.endInclusive).any { possibleDay ->
                entries.none { it.gameId == id && it.puzzleDay == possibleDay }
            }
        return when {
            day < possible.start -> {
                PuzzleStatus.Missed(currentPuzzleUnplayed)
            }

            day <= possible.endInclusive -> {
                PuzzleStatus.Unplayed(nextRollover)
            }

            // The puzzle for the day after the last possible one is released at the next rollover.
            day == possible.endInclusive.plusDays(1) -> {
                PuzzleStatus.Unreleased(nextRollover, currentPuzzleUnplayed)
            }

            else -> {
                PuzzleStatus.Unreleased(nextRollover = null, currentPuzzleUnplayed = currentPuzzleUnplayed)
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
