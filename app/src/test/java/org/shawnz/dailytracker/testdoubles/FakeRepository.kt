package org.shawnz.dailytracker.testdoubles

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.data.catalog.CatalogGame
import java.time.Instant
import java.time.LocalDate

/**
 * A [Repository] held in memory.
 *
 * Writes go to the same flows the reads come from, so a test can act and then assert on the
 * resulting state.
 */
class FakeRepository(
    games: List<Game> = emptyList(),
    entries: List<EntryEntity> = emptyList(),
) : Repository {
    val games = MutableStateFlow(games)
    val entries = MutableStateFlow(entries)

    private var nextId = 1L

    override fun observeActiveGames(): Flow<List<Game>> =
        games.map { list ->
            list.filterNot {
                it.archived
            }
        }

    override fun observeGame(id: Long): Flow<Game?> =
        games.map { list ->
            list.firstOrNull {
                it.id ==
                    id
            }
        }

    override fun observeEntries(gameId: Long): Flow<List<EntryEntity>> =
        entries.map { list ->
            list.filter {
                it.gameId ==
                    gameId
            }
        }

    override fun observeRecentEntries(since: LocalDate): Flow<List<EntryEntity>> =
        entries.map { list -> list.filter { it.puzzleDay >= since } }

    override fun observeAllEntries(): Flow<List<EntryEntity>> = entries

    override fun observeTrackedCatalogGames(): Flow<List<CatalogGame>> =
        games.map { list ->
            list.mapNotNull {
                it.catalogGame
            }
        }

    override fun observeArchivedGames(): Flow<List<Game>> =
        games.map { list ->
            list.filter {
                it.archived
            }
        }

    override fun observeAllGames(): Flow<List<Game>> = games

    override suspend fun getGame(id: Long): Game? = games.value.firstOrNull { it.id == id }

    override suspend fun activeGames(): List<Game> = games.value.filterNot { it.archived }

    override suspend fun addFromCatalog(catalog: CatalogGame): Long = nextId++

    override suspend fun addCustom(
        title: String,
        url: String,
    ): Long = nextId++

    override suspend fun deleteGame(game: Game) {
        games.value = games.value.filterNot { it.id == game.id }
    }

    override suspend fun setArchived(
        game: Game,
        archived: Boolean,
    ) {
        games.value =
            games.value.map {
                if (it.id == game.id) it.copy(archived = archived) else it
            }
    }

    override suspend fun setReminders(
        game: Game,
        enabled: Boolean,
    ) {
        games.value =
            games.value.map {
                if (it.id == game.id) it.copy(remindersEnabled = enabled) else it
            }
    }

    override suspend fun entryFor(
        game: Game,
        day: LocalDate,
    ): EntryEntity? =
        entries.value.firstOrNull {
            it.gameId == game.id && it.puzzleDay == day
        }

    override suspend fun record(
        game: Game,
        rawText: String?,
        day: LocalDate,
    ): Long {
        val existing = entryFor(game, day)
        val entry =
            EntryEntity(
                id = existing?.id ?: nextId++,
                gameId = game.id,
                puzzleDay = day,
                completedAt = existing?.completedAt ?: Instant.EPOCH,
                rawShareText = rawText ?: existing?.rawShareText,
            )
        entries.value = entries.value.filterNot { it.id == entry.id } + entry
        return entry.id
    }

    override suspend fun deleteEntry(entry: EntryEntity) {
        entries.value = entries.value.filterNot { it.id == entry.id }
    }
}
