package org.shawnz.dailytracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import org.shawnz.dailytracker.data.catalog.CatalogGame
import java.time.Instant
import java.time.LocalDate

/**
 * A game that the user tracks.
 *
 * Contains only the fields that are not in the catalog. A user-made game has no catalog entry,
 * and stores its title and URL here.
 *
 * @property catalogGame The catalog game this is. Null for a user-made game.
 * @property title The title of a user-made game. Null for a catalog game.
 * @property url The URL of a user-made game. Null for a catalog game.
 * @property createdAt Kept for diagnostics. Nothing reads it.
 */
@Entity(tableName = "games")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val catalogGame: CatalogGame? = null,
    val title: String? = null,
    val url: String? = null,
    val sortOrder: Int = 0,
    val archived: Boolean = false,
    val remindersEnabled: Boolean = true,
    val createdAt: Instant = Instant.now(),
)

/**
 * One recorded play.
 *
 * This table stores the text the user saved, along with the fields needed to count plays.
 * Parsed values are not stored, because [rawShareText] is parsed again at each draw.
 *
 * @property puzzleDay The puzzle's date, following this game's rollover rule.
 * @property rawShareText The text the user saved. Null for a play recorded without one.
 */
@Entity(
    tableName = "entries",
    foreignKeys = [
        ForeignKey(
            entity = GameEntity::class,
            parentColumns = ["id"],
            childColumns = ["gameId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["gameId", "puzzleDay"], unique = true), Index("gameId")],
)
data class EntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gameId: Long,
    val puzzleDay: LocalDate,
    val completedAt: Instant = Instant.now(),
    val rawShareText: String? = null,
)
