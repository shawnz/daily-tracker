package org.shawnz.dailytracker.data

import androidx.room.Room
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.shawnz.dailytracker.data.catalog.CatalogGame
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The repository against the database it actually uses, so the queries are covered as well as
 * the surrounding Kotlin.
 */
@RunWith(RobolectricTestRunner::class)
class DefaultRepositoryTest {
    private lateinit var db: AppDatabase
    private lateinit var repo: Repository

    @Before
    fun open() {
        db =
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    AppDatabase::class.java,
                ).addCallback(GameSourceRule)
                .addCallback(EntryZoneRule)
                .build()
        repo = DefaultRepository(db.gameDao(), db.entryDao())
    }

    @After
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
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER entries_zone_on_insert")
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
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER entries_zone_on_insert")
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
}
