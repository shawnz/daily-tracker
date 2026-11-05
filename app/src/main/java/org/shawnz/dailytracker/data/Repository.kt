package org.shawnz.dailytracker.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.shawnz.dailytracker.data.catalog.CatalogGame
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Reads and writes the tracked games and their plays. */
interface Repository {
    fun observeActiveGames(): Flow<List<Game>>

    fun observeGame(id: Long): Flow<Game?>

    fun observeEntries(gameId: Long): Flow<List<EntryEntity>>

    fun observeRecentEntries(since: LocalDate): Flow<List<EntryEntity>>

    fun observeAllEntries(): Flow<List<EntryEntity>>

    fun observeEntriesOn(day: LocalDate): Flow<List<EntryEntity>>

    /** The earliest puzzle day with an entry of an active game, or null when there is none. */
    fun observeEarliestPlayedDay(): Flow<LocalDate?>

    /** Every puzzle day with an entry of an active game. */
    fun observePlayedDays(): Flow<Set<LocalDate>>

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
        day: LocalDate,
    ): EntryEntity?

    /**
     * Records a play.
     *
     * [rawText] is stored as given, without validation. If the entry already has text that
     * [EntryEntity.hasText] matches, its completion time and zone are not changed. Otherwise they
     * are set to the current time and zone.
     */
    suspend fun record(
        game: Game,
        rawText: String?,
        day: LocalDate,
    ): Long

    suspend fun deleteEntry(entry: EntryEntity)

    /**
     * Moves each user-made game whose URL matches a catalog game onto that catalog game, so its
     * entries are read with the catalog's parser.
     *
     * Each entry moves to the catalog game's row, unless that row has an entry on the same day:
     * - When the user-made entry has no text, or text that [EntryEntity.hasText] matches, it is
     *   deleted.
     * - When only the catalog game's entry has no text, it is replaced with the user-made entry.
     * - When the texts differ, the user-made entry stays on the user-made row.
     *
     * The user-made row is deleted when no entries are left on it. When the catalog game has no
     * row, the user-made row is updated in place instead: its catalog game is set, its title and
     * URL are cleared, and its entries and settings are kept.
     */
    suspend fun migrateCustomGames()
}

class DefaultRepository
    @Inject
    constructor(
        private val database: AppDatabase,
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

        override fun observeEntriesOn(day: LocalDate): Flow<List<EntryEntity>> = entryDao.observeOnDay(day)

        override fun observeEarliestPlayedDay(): Flow<LocalDate?> = entryDao.observeEarliestActiveDay()

        override fun observePlayedDays(): Flow<Set<LocalDate>> = entryDao.observeActiveDays().map { it.toSet() }

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
            val existing = entryDao.find(game.id, day)

            val entry =
                when {
                    existing == null -> {
                        EntryEntity(gameId = game.id, puzzleDay = day, rawShareText = rawText)
                    }

                    existing.hasText(rawText) -> {
                        existing.copy(rawShareText = rawText)
                    }

                    else -> {
                        existing.copy(
                            completedAt = Instant.now(),
                            completedZone = ZoneId.systemDefault(),
                            rawShareText = rawText,
                        )
                    }
                }

            return if (existing != null) {
                entryDao.update(entry)
                existing.id
            } else {
                entryDao.insert(entry)
            }
        }

        override suspend fun deleteEntry(entry: EntryEntity) = entryDao.delete(entry)

        override suspend fun migrateCustomGames() =
            database.withTransaction {
                for (custom in gameDao.getCustom()) {
                    val url = custom.url ?: continue
                    val catalog =
                        CatalogGame.entries.firstOrNull { game -> game.urls.any { urlMatches(it, url) } }
                            ?: continue
                    val target = gameDao.findCatalog(catalog)
                    if (target == null) {
                        gameDao.update(custom.copy(catalogGame = catalog, title = null, url = null))
                        continue
                    }
                    val targetEntries = entryDao.getForGame(target.id).associateBy { it.puzzleDay }
                    var left = 0
                    for (entry in entryDao.getForGame(custom.id)) {
                        val clash = targetEntries[entry.puzzleDay]
                        when {
                            clash == null -> {
                                entryDao.update(entry.copy(gameId = target.id))
                            }

                            // No text, or the same text as the catalog game's entry.
                            entry.rawShareText == null || clash.hasText(entry.rawShareText) -> {
                                entryDao.delete(entry)
                            }

                            // The catalog game's entry has no text, so it is replaced with this one.
                            clash.rawShareText == null -> {
                                entryDao.delete(clash)
                                entryDao.update(entry.copy(gameId = target.id))
                            }

                            // Different text on each side, so this entry stays for the user to resolve.
                            else -> {
                                left++
                            }
                        }
                    }
                    if (left == 0) gameDao.deleteById(custom.id)
                }
            }
    }

/** Sorts by [Game.sortOrder], then by [Game.title] (stored only for a user-made game). */
private fun List<Game>.inListOrder(): List<Game> =
    sortedWith(
        compareBy({ it.sortOrder }, { it.title }),
    )
