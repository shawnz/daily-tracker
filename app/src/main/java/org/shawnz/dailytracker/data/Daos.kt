package org.shawnz.dailytracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.shawnz.dailytracker.data.catalog.CatalogGame
import java.time.LocalDate

@Dao
interface GameDao {
    @Query("SELECT * FROM games WHERE archived = 0 ORDER BY sortOrder ASC")
    fun observeActive(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games WHERE id = :id")
    fun observeById(id: Long): Flow<GameEntity?>

    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getById(id: Long): GameEntity?

    @Query("SELECT * FROM games WHERE archived = 0")
    suspend fun getActive(): List<GameEntity>

    @Query("SELECT * FROM games WHERE archived = 1 ORDER BY sortOrder ASC")
    fun observeArchived(): Flow<List<GameEntity>>

    @Query("SELECT * FROM games")
    fun observeAll(): Flow<List<GameEntity>>

    @Query("SELECT catalogGame FROM games WHERE catalogGame IS NOT NULL")
    fun observeTrackedCatalogGames(): Flow<List<CatalogGame>>

    @Query("SELECT * FROM games WHERE catalogGame IS NULL")
    suspend fun getCustom(): List<GameEntity>

    @Query("SELECT * FROM games WHERE catalogGame = :catalogGame LIMIT 1")
    suspend fun findCatalog(catalogGame: CatalogGame): GameEntity?

    @Update
    suspend fun update(game: GameEntity)

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM games")
    suspend fun maxSortOrder(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(game: GameEntity): Long

    @Query("UPDATE games SET archived = :archived WHERE id = :id")
    suspend fun setArchived(
        id: Long,
        archived: Boolean,
    )

    @Query("UPDATE games SET remindersEnabled = :enabled WHERE id = :id")
    suspend fun setReminders(
        id: Long,
        enabled: Boolean,
    )

    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE gameId = :gameId ORDER BY puzzleDay DESC")
    fun observeForGame(gameId: Long): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE gameId = :gameId")
    suspend fun getForGame(gameId: Long): List<EntryEntity>

    @Query("SELECT * FROM entries WHERE puzzleDay >= :minDay")
    fun observeSince(minDay: LocalDate): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries")
    fun observeAll(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM entries WHERE puzzleDay = :day")
    fun observeOnDay(day: LocalDate): Flow<List<EntryEntity>>

    /** Null when no active game has an entry. */
    @Query(
        "SELECT MIN(entries.puzzleDay) FROM entries " +
            "JOIN games ON games.id = entries.gameId WHERE games.archived = 0",
    )
    fun observeEarliestActiveDay(): Flow<LocalDate?>

    @Query(
        "SELECT DISTINCT entries.puzzleDay FROM entries " +
            "JOIN games ON games.id = entries.gameId WHERE games.archived = 0",
    )
    fun observeActiveDays(): Flow<List<LocalDate>>

    @Query("SELECT * FROM entries WHERE gameId = :gameId AND puzzleDay = :puzzleDay LIMIT 1")
    suspend fun find(
        gameId: Long,
        puzzleDay: LocalDate,
    ): EntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entry: EntryEntity): Long

    @Update
    suspend fun update(entry: EntryEntity)

    @Delete
    suspend fun delete(entry: EntryEntity)
}
