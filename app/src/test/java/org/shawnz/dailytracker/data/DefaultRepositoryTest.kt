package org.shawnz.dailytracker.data

import androidx.room3.Room
import androidx.room3.executeSQL
import androidx.room3.useWriterConnection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.catalog.CatalogGame
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The repository against the database it actually uses, so the queries are covered as well as
 * the surrounding Kotlin.
 */
class DefaultRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository

    @BeforeEach
    fun open() {
        db = buildAppDatabase(Room.inMemoryDatabaseBuilder<AppDatabase>())
        repo = DefaultRepository(db, db.gameDao(), db.entryDao())
    }

    @AfterEach
    fun close() {
        db.close()
    }

    @Test
    fun `each game added takes the next position`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            repo.addFromCatalog(CatalogGame.CONNECTIONS)

            val positions = repo.activeGames().map { it.sortOrder }
            assertEquals(listOf(1, 2), positions)
        }
    }

    // Position first, then title, so the order does not depend on insertion.
    @Test
    fun `games are listed by position and then by title`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            repo.addCustom("Alpha", "https://alpha.example")

            val titles = repo.observeActiveGames().first().map { it.title }
            assertEquals(listOf("Wordle", "Alpha"), titles)
        }
    }

    @Test
    fun `an archived game leaves the active list and joins the archived one`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            val game = repo.activeGames().single()

            repo.setArchived(game, true)

            assertTrue(repo.observeActiveGames().first().isEmpty())
            assertEquals(1, repo.observeArchivedGames().first().size)
            assertEquals(1, repo.observeAllGames().first().size)
        }
    }

    // Puzzle 1,234, VINYL.
    private val puzzleDay = LocalDate.of(2024, 11, 4)
    private val solvedInFour = "Wordle 1,234 4/6\n\n⬛⬛⬛🟨⬛\n⬛🟨⬛⬛⬛\n⬛🟩🟩⬛🟨\n🟩🟩🟩🟩🟩"
    private val solvedInThree = "Wordle 1,234 3/6\n\n⬛⬛🟨⬛⬛\n🟨🟩🟩⬛⬛\n🟩🟩🟩🟩🟩"

    // One entry per game per day, so a second save replaces the first.
    @Test
    fun `recording the same day twice replaces the entry`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            val game = repo.activeGames().single()

            val first = repo.record(game, solvedInFour, puzzleDay)
            val second = repo.record(game, solvedInThree, puzzleDay)

            assertEquals(first, second)
            val saved = repo.observeEntries(game.id).first().single()
            assertEquals(solvedInThree, saved.rawShareText)
        }
    }

    // Saving the same text again doesn't count as a new play. Text pasted on the detail page can
    // end in a newline, so one is added by hand.
    @Test
    fun `saving the same text again keeps the time it was saved`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            val game = repo.activeGames().single()
            db.entryDao().insert(
                EntryEntity(
                    gameId = game.id,
                    puzzleDay = puzzleDay,
                    completedAt = Instant.EPOCH,
                    rawShareText = solvedInFour,
                ),
            )

            repo.record(game, "$solvedInFour\n", puzzleDay)

            assertEquals(Instant.EPOCH, repo.entryFor(game, puzzleDay)!!.completedAt)
        }
    }

    // A corrected result is saved at the time of the correction.
    @Test
    fun `saving different text takes the current time`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            val game = repo.activeGames().single()
            db.entryDao().insert(
                EntryEntity(
                    gameId = game.id,
                    puzzleDay = puzzleDay,
                    completedAt = Instant.EPOCH,
                    rawShareText = solvedInFour,
                ),
            )

            repo.record(game, solvedInThree, puzzleDay)

            assertTrue(repo.entryFor(game, puzzleDay)!!.completedAt.isAfter(Instant.EPOCH))
        }
    }

    @Test
    fun `a new entry stores the device's zone`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            val game = repo.activeGames().single()

            repo.record(game, solvedInFour, puzzleDay)

            assertEquals(ZoneId.systemDefault(), repo.entryFor(game, puzzleDay)!!.completedZone)
        }
    }

    // An entry from before schema version 3 with no zone is left that way when the same text is
    // saved again, since the zone it should have had can't be reliably inferred. The insert
    // trigger is dropped to make such an entry for testing, and the update trigger is kept.
    @Test
    fun `saving the same text over an entry with no zone leaves it with no zone`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            val game = repo.activeGames().single()
            db.useWriterConnection { it.executeSQL("DROP TRIGGER entries_zone_on_insert") }
            db.entryDao().insert(
                EntryEntity(gameId = game.id, puzzleDay = puzzleDay, completedZone = null, rawShareText = solvedInFour),
            )

            repo.record(game, solvedInFour, puzzleDay)

            assertNull(repo.entryFor(game, puzzleDay)!!.completedZone)
        }
    }

    // The insert trigger is dropped to make an entry with no zone for testing, and the update
    // trigger is kept.
    @Test
    fun `saving different text over an entry with no zone stores the device's zone`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            val game = repo.activeGames().single()
            db.useWriterConnection { it.executeSQL("DROP TRIGGER entries_zone_on_insert") }
            db.entryDao().insert(
                EntryEntity(gameId = game.id, puzzleDay = puzzleDay, completedZone = null, rawShareText = solvedInFour),
            )

            repo.record(game, solvedInThree, puzzleDay)

            val saved = repo.entryFor(game, puzzleDay)!!
            assertEquals(solvedInThree, saved.rawShareText)
            assertEquals(ZoneId.systemDefault(), saved.completedZone)
        }
    }

    @Test
    fun `the entries on a day are the entries for that puzzle day`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            repo.addFromCatalog(CatalogGame.CONNECTIONS)
            val (wordle, connections) = repo.activeGames()
            repo.record(wordle, null, puzzleDay)
            repo.record(connections, null, puzzleDay)
            repo.record(wordle, null, puzzleDay.minusDays(1))

            val games = repo.observeEntriesOn(puzzleDay).first().mapTo(HashSet()) { it.gameId }

            assertEquals(setOf(wordle.id, connections.id), games)
        }
    }

    @Test
    fun `the played days and the earliest played day leave out archived games`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            repo.addFromCatalog(CatalogGame.CONNECTIONS)
            val (wordle, connections) = repo.activeGames()
            repo.record(wordle, null, puzzleDay)
            repo.record(connections, null, puzzleDay.minusDays(3))
            repo.setArchived(connections, true)

            assertEquals(setOf(puzzleDay), repo.observePlayedDays().first())
            assertEquals(puzzleDay, repo.observeEarliestPlayedDay().first())
        }
    }

    @Test
    fun `with no entries, there is no earliest played day`() {
        runBlocking {
            assertNull(repo.observeEarliestPlayedDay().first())
        }
    }

    @Test
    fun `an entry with no text still records a play`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            val game = repo.activeGames().single()

            repo.record(game, null, LocalDate.of(2026, 9, 14))

            val saved = repo.observeEntries(game.id).first().single()
            assertNull(saved.rawShareText)
        }
    }

    @Test
    fun `recording no text over a saved result removes the text`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            val game = repo.activeGames().single()
            repo.record(game, solvedInFour, puzzleDay)

            repo.record(game, null, puzzleDay)

            val saved = repo.observeEntries(game.id).first().single()
            assertNull(saved.rawShareText)
        }
    }

    @Test
    fun `deleting a game takes its entries with it`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            val game = repo.activeGames().single()
            repo.record(game, solvedInFour, puzzleDay)

            repo.deleteGame(game)

            assertTrue(repo.observeAllEntries().first().isEmpty())
        }
    }

    @Test
    fun `only the days in range are reported as recent`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            val game = repo.activeGames().single()
            val today = LocalDate.of(2026, 9, 20)
            repo.record(game, null, today)
            repo.record(game, null, today.minusDays(40))

            val recent = repo.observeRecentEntries(today.minusDays(30)).first()

            assertEquals(1, recent.size)
        }
    }

    @Test
    fun `a tracked catalog game is reported as tracked`() {
        runBlocking {
            repo.addFromCatalog(CatalogGame.WORDLE)
            repo.addCustom("Alpha", "https://alpha.example")

            assertEquals(
                listOf(CatalogGame.WORDLE),
                repo.observeTrackedCatalogGames().first(),
            )
        }
    }

    // Two plays of the MapTap daily of 4 November 2026, copied from the game.
    private val mapTapDay = LocalDate.of(2026, 11, 4)
    private val mapTap893 = "www.maptap.gg November 4\n94🏅 99🔥 95🏅 90👑 80🌞\nFinal score: 893"
    private val mapTap858 = "www.maptap.gg November 4\n90👑 68🫢 77🤗 96🔥 86🎓\nFinal score: 858"

    @Test
    fun `a user-made game with a catalog URL becomes that catalog game`() {
        runBlocking {
            val id = repo.addCustom("Map Tap", "https://www.maptap.gg/")
            repo.record(repo.getGame(id)!!, mapTap893, mapTapDay)

            repo.migrateCustomGames()

            val game = repo.getGame(id)!!
            assertEquals(CatalogGame.MAPTAP, game.catalogGame)
            assertEquals("MapTap", game.title)
            assertEquals(mapTap893, repo.entryFor(game, mapTapDay)!!.rawShareText)
        }
    }

    @Test
    fun `a URL is matched in any of its forms and against every address of the game`() {
        runBlocking {
            val forms =
                mapOf(
                    "maptap.gg" to CatalogGame.MAPTAP,
                    "http://playhued.com" to CatalogGame.HUED,
                    "https://kindahard.golf/" to CatalogGame.KINDA_HARD_GOLF,
                    "raddle.quest/563" to CatalogGame.RADDLE,
                )
            val ids = forms.keys.associateWith { repo.addCustom(it, it) }

            repo.migrateCustomGames()

            forms.forEach { (url, catalogGame) ->
                assertEquals(catalogGame, repo.getGame(ids.getValue(url))!!.catalogGame, url)
            }
        }
    }

    // FoodGuessr has other dailies under /game, and only /game/daily is in the catalog.
    @Test
    fun `a user-made game with any other URL is left as it is`() {
        runBlocking {
            val alpha = repo.addCustom("Alpha", "https://alpha.example")
            val plateOff = repo.addCustom("Plate-Off", "https://www.foodguessr.com/game/plate-off/daily")

            repo.migrateCustomGames()

            assertNull(repo.getGame(alpha)!!.catalogGame)
            assertNull(repo.getGame(plateOff)!!.catalogGame)
        }
    }

    @Test
    fun `an archived user-made game stays archived`() {
        runBlocking {
            val id = repo.addCustom("Map Tap", "https://maptap.gg")
            repo.setArchived(repo.getGame(id)!!, true)

            repo.migrateCustomGames()

            val game = repo.getGame(id)!!
            assertEquals(CatalogGame.MAPTAP, game.catalogGame)
            assertTrue(game.archived)
        }
    }

    @Test
    fun `with the catalog game tracked too, the entries move to it and the user-made game is deleted`() {
        runBlocking {
            val catalogId = repo.addFromCatalog(CatalogGame.MAPTAP)
            val customId = repo.addCustom("Map Tap", "https://maptap.gg")
            val catalog = repo.getGame(catalogId)!!
            val custom = repo.getGame(customId)!!
            repo.record(catalog, mapTap893, mapTapDay)
            repo.record(custom, null, mapTapDay.minusDays(1))
            repo.record(custom, null, mapTapDay.minusDays(2))

            repo.migrateCustomGames()

            assertNull(repo.getGame(customId))
            val days = repo.observeEntries(catalogId).first().map { it.puzzleDay }
            assertEquals(listOf(mapTapDay, mapTapDay.minusDays(1), mapTapDay.minusDays(2)), days)
        }
    }

    @Test
    fun `an entry that clashes with the catalog game's stays on the user-made game`() {
        runBlocking {
            val catalogId = repo.addFromCatalog(CatalogGame.MAPTAP)
            val customId = repo.addCustom("Map Tap", "https://maptap.gg")
            val catalog = repo.getGame(catalogId)!!
            val custom = repo.getGame(customId)!!
            repo.record(catalog, mapTap893, mapTapDay)
            repo.record(custom, mapTap858, mapTapDay)
            repo.record(custom, null, mapTapDay.minusDays(1))

            repo.migrateCustomGames()

            assertEquals(mapTap893, repo.entryFor(catalog, mapTapDay)!!.rawShareText)
            assertTrue(repo.entryFor(catalog, mapTapDay.minusDays(1)) != null)
            val left = repo.observeEntries(customId).first().single()
            assertEquals(mapTap858, left.rawShareText)
        }
    }

    // Text pasted on the detail page can end in a newline, so one is added by hand to the
    // user-made copy.
    @Test
    fun `an entry with the same text as the catalog game's on its day is deleted`() {
        runBlocking {
            val catalogId = repo.addFromCatalog(CatalogGame.MAPTAP)
            val customId = repo.addCustom("Map Tap", "https://maptap.gg")
            repo.record(repo.getGame(catalogId)!!, mapTap893, mapTapDay)
            repo.record(repo.getGame(customId)!!, "$mapTap893\n", mapTapDay)
            repo.record(repo.getGame(catalogId)!!, null, mapTapDay.minusDays(1))
            repo.record(repo.getGame(customId)!!, null, mapTapDay.minusDays(1))

            repo.migrateCustomGames()

            assertNull(repo.getGame(customId))
            assertEquals(2, repo.observeEntries(catalogId).first().size)
        }
    }

    @Test
    fun `a catalog game's entry with no text is replaced with the user-made entry`() {
        runBlocking {
            val catalogId = repo.addFromCatalog(CatalogGame.MAPTAP)
            val customId = repo.addCustom("Map Tap", "https://maptap.gg")
            val catalog = repo.getGame(catalogId)!!
            repo.record(catalog, null, mapTapDay)
            repo.record(repo.getGame(customId)!!, mapTap893, mapTapDay)

            repo.migrateCustomGames()

            assertNull(repo.getGame(customId))
            assertEquals(mapTap893, repo.entryFor(catalog, mapTapDay)!!.rawShareText)
        }
    }

    @Test
    fun `an entry with no text is deleted when the catalog game's entry has text`() {
        runBlocking {
            val catalogId = repo.addFromCatalog(CatalogGame.MAPTAP)
            val customId = repo.addCustom("Map Tap", "https://maptap.gg")
            val catalog = repo.getGame(catalogId)!!
            repo.record(catalog, mapTap893, mapTapDay)
            repo.record(repo.getGame(customId)!!, null, mapTapDay)

            repo.migrateCustomGames()

            assertNull(repo.getGame(customId))
            assertEquals(mapTap893, repo.entryFor(catalog, mapTapDay)!!.rawShareText)
        }
    }

    @Test
    fun `once a clash is resolved, the user-made game is deleted at the next migration`() {
        runBlocking {
            val catalogId = repo.addFromCatalog(CatalogGame.MAPTAP)
            val customId = repo.addCustom("Map Tap", "https://maptap.gg")
            val catalog = repo.getGame(catalogId)!!
            val custom = repo.getGame(customId)!!
            repo.record(catalog, mapTap893, mapTapDay)
            repo.record(custom, mapTap858, mapTapDay)
            repo.migrateCustomGames()

            repo.deleteEntry(repo.entryFor(catalog, mapTapDay)!!)
            repo.migrateCustomGames()

            assertNull(repo.getGame(customId))
            assertEquals(mapTap858, repo.entryFor(catalog, mapTapDay)!!.rawShareText)
        }
    }
}
