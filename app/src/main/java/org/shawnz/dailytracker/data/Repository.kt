package org.shawnz.dailytracker.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.shawnz.dailytracker.data.catalog.CatalogGame
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/** Reads and writes the tracked games and their plays. */
interface Repository {
    fun observeActiveGames(): Flow<List<Game>>

    fun observeGame(id: Long): Flow<Game?>

    fun observeEntries(gameId: Long): Flow<List<EntryEntity>>

    fun observeRecentEntries(since: LocalDate): Flow<List<EntryEntity>>

    fun observeAllEntries(): Flow<List<EntryEntity>>

    fun observeTrackedCatalogGames(): Flow<List<CatalogGame>>

    fun observeArchivedGames(): Flow<List<Game>>

    /** Every tracked game, including archived ones. */
    fun observeAllGames(): Flow<List<Game>>

    suspend fun getGame(id: Long): Game?

    suspend fun activeGames(): List<Game>

    suspend fun addFromCatalog(catalog: CatalogGame): Long

    suspend fun addCustom(
        title: String,
        url: String,
    ): Long

    suspend fun deleteGame(game: Game)

    suspend fun setArchived(
        game: Game,
        archived: Boolean,
    )

    suspend fun setReminders(
        game: Game,
        enabled: Boolean,
    )

    suspend fun entryFor(
        game: Game,
        day: LocalDate = game.currentPuzzleDay(),
    ): EntryEntity?

    /**
     * Records a play.
     *
     * [rawText] is stored as given, without validation. Only [EntryEntity.success] is derived
     * from it here.
     */
    suspend fun record(
        game: Game,
        rawText: String?,
        day: LocalDate = game.currentPuzzleDay(),
    ): Long

    suspend fun deleteEntry(entry: EntryEntity)
}

class DefaultRepository
    @Inject
    constructor(
        private val gameDao: GameDao,
        private val entryDao: EntryDao,
    ) : Repository {
        override fun observeActiveGames(): Flow<List<Game>> =
            gameDao.observeActive().map { rows ->
                rows.map { it.resolve() }.inListOrder()
            }

        override fun observeGame(id: Long): Flow<Game?> =
            gameDao.observeById(id).map {
                it?.resolve()
            }

        override fun observeEntries(gameId: Long): Flow<List<EntryEntity>> = entryDao.observeForGame(gameId)

        override fun observeRecentEntries(since: LocalDate): Flow<List<EntryEntity>> = entryDao.observeSince(since)

        override fun observeAllEntries(): Flow<List<EntryEntity>> = entryDao.observeAll()

        override fun observeTrackedCatalogGames(): Flow<List<CatalogGame>> = gameDao.observeTrackedCatalogGames()

        override fun observeArchivedGames(): Flow<List<Game>> =
            gameDao.observeArchived().map { rows -> rows.map { it.resolve() }.inListOrder() }

        override fun observeAllGames(): Flow<List<Game>> =
            gameDao.observeAll().map { rows ->
                rows.map {
                    it.resolve()
                }
            }

        override suspend fun getGame(id: Long): Game? = gameDao.getById(id)?.resolve()

        override suspend fun activeGames(): List<Game> =
            gameDao
                .getActive()
                .map {
                    it.resolve()
                }.inListOrder()

        override suspend fun addFromCatalog(catalog: CatalogGame): Long =
            gameDao.insert(
                catalog.toEntity(gameDao.maxSortOrder() + 1),
            )

        override suspend fun addCustom(
            title: String,
            url: String,
        ): Long =
            gameDao.insert(
                GameEntity(
                    title = title.trim(),
                    url = url.trim(),
                    sortOrder = gameDao.maxSortOrder() + 1,
                ),
            )

        override suspend fun deleteGame(game: Game) = gameDao.deleteById(game.id)

        override suspend fun setArchived(
            game: Game,
            archived: Boolean,
        ) = gameDao.setArchived(game.id, archived)

        override suspend fun setReminders(
            game: Game,
            enabled: Boolean,
        ) = gameDao.setReminders(game.id, enabled)

        override suspend fun entryFor(
            game: Game,
            day: LocalDate,
        ): EntryEntity? = entryDao.find(game.id, day)

        override suspend fun record(
            game: Game,
            rawText: String?,
            day: LocalDate,
        ): Long {
            val parsed = rawText?.let { game.parser?.parse(it) }
            val existing = entryDao.find(game.id, day)

            val entry =
                EntryEntity(
                    id = existing?.id ?: 0,
                    gameId = game.id,
                    puzzleDay = day,
                    completedAt = existing?.completedAt ?: Instant.now(),
                    rawShareText = rawText ?: existing?.rawShareText,
                    success = parsed?.success ?: existing?.success,
                )

            return if (existing != null) {
                entryDao.update(entry)
                existing.id
            } else {
                entryDao.insert(entry)
            }
        }

        override suspend fun deleteEntry(entry: EntryEntity) = entryDao.delete(entry)
    }

/** Sorts by [Game.sortOrder], then by [Game.title] (stored only for a user-made game). */
private fun List<Game>.inListOrder(): List<Game> =
    sortedWith(
        compareBy({ it.sortOrder }, { it.title }),
    )
