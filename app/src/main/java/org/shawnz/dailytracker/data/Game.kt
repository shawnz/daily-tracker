package org.shawnz.dailytracker.data

import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.parse.ResultParser
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

    val rolloverType: RolloverType get() = catalogGame?.rolloverType ?: RolloverType.UNSCHEDULED
    val rolloverZoneId: String? get() = catalogGame?.rolloverZoneId
    val rolloverMinuteOfDay: Int get() = catalogGame?.rolloverMinuteOfDay ?: 0

    fun currentPuzzleDay(): LocalDate = currentPuzzleDay(rolloverType, rolloverZoneId, rolloverMinuteOfDay)

    fun nextRollover(): Instant? = nextRollover(rolloverType, rolloverZoneId, rolloverMinuteOfDay)
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
