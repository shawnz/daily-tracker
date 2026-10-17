package org.shawnz.dailytracker.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.shawnz.dailytracker.data.catalog.CatalogGame
import java.time.LocalDate

/**
 * The schema's own rules, which live in SQL rather than in Kotlin: the triggers that limit a
 * game to one source of its details, the cascade from a game to its entries, and the index
 * that allows one entry per game per day.
 */
@RunWith(RobolectricTestRunner::class)
class AppDatabaseTest {
    private lateinit var db: AppDatabase

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
    }

    @After
    fun close() {
        db.close()
    }

    private val catalogRow = GameEntity(catalogGame = CatalogGame.WORDLE)

    private val day = LocalDate.of(2024, 10, 4)

    @Test
    fun `a catalog game is allowed`() {
        runBlocking { assertTrue(db.gameDao().insert(catalogRow) > 0) }
    }

    @Test
    fun `a game with a title and a url is allowed`() {
        runBlocking {
            val row = GameEntity(title = "Hued", url = "https://hued.example")
            assertTrue(db.gameDao().insert(row) > 0)
        }
    }

    @Test
    fun `a game cannot have both a catalog entry and its own details`() {
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                db.gameDao().insert(
                    GameEntity(
                        catalogGame = CatalogGame.WORDLE,
                        title = "Hued",
                        url = "https://hued.example",
                    ),
                )
            }
        }
    }

    @Test
    fun `a game must have one source or the other`() {
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking { db.gameDao().insert(GameEntity()) }
        }
    }

    // The rule covers edits as well, so a row cannot be changed into an invalid one.
    @Test
    fun `a game cannot be edited into having neither source`() {
        val id = runBlocking { db.gameDao().insert(catalogRow) }
        assertThrows(SQLiteConstraintException::class.java) {
            db.openHelper.writableDatabase.execSQL(
                "UPDATE games SET catalogGame = NULL WHERE id = $id",
            )
        }
    }

    @Test
    fun `deleting a game deletes its entries`() {
        runBlocking {
            val id = db.gameDao().insert(catalogRow)
            db.entryDao().insert(EntryEntity(gameId = id, puzzleDay = day))
            assertNotNull(db.entryDao().find(id, day))

            db.gameDao().deleteById(id)

            assertNull(db.entryDao().find(id, day))
        }
    }

    @Test
    fun `one entry per game per day`() {
        val id =
            runBlocking {
                val id = db.gameDao().insert(catalogRow)
                db.entryDao().insert(EntryEntity(gameId = id, puzzleDay = day))
                id
            }
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                db.entryDao().insert(EntryEntity(gameId = id, puzzleDay = day))
            }
        }
    }

    @Test
    fun `the same day on another game is a separate entry`() {
        runBlocking {
            val first = db.gameDao().insert(catalogRow)
            val second = db.gameDao().insert(GameEntity(catalogGame = CatalogGame.CONNECTIONS))

            db.entryDao().insert(EntryEntity(gameId = first, puzzleDay = day))
            db.entryDao().insert(EntryEntity(gameId = second, puzzleDay = day))

            assertNotNull(db.entryDao().find(first, day))
            assertNotNull(db.entryDao().find(second, day))
        }
    }

    @Test
    fun `a new entry must have a zone`() {
        val id = runBlocking { db.gameDao().insert(catalogRow) }
        assertThrows(SQLiteConstraintException::class.java) {
            runBlocking {
                db.entryDao().insert(EntryEntity(gameId = id, puzzleDay = day, completedZone = null))
            }
        }
    }

    @Test
    fun `an entry cannot be edited into having no zone`() {
        val id = runBlocking { db.gameDao().insert(catalogRow) }
        val entry = runBlocking { db.entryDao().insert(EntryEntity(gameId = id, puzzleDay = day)) }
        assertThrows(SQLiteConstraintException::class.java) {
            db.openHelper.writableDatabase.execSQL(
                "UPDATE entries SET completedZone = NULL WHERE id = $entry",
            )
        }
    }

    // An entry saved before schema version 3 has no zone. The insert trigger is dropped to make
    // such an entry for testing, and the update trigger is kept.
    @Test
    fun `an entry with no zone can still be edited`() {
        runBlocking {
            val id = db.gameDao().insert(catalogRow)
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER entries_zone_on_insert")
            val entryId = db.entryDao().insert(EntryEntity(gameId = id, puzzleDay = day, completedZone = null))

            db.entryDao().update(
                EntryEntity(id = entryId, gameId = id, puzzleDay = day, completedZone = null, rawShareText = "edited"),
            )

            assertEquals("edited", db.entryDao().find(id, day)?.rawShareText)
        }
    }

    // COALESCE, so that the first game still gets a position.
    @Test
    fun `an empty table reports a sort order of zero`() {
        runBlocking { assertEquals(0, db.gameDao().maxSortOrder()) }
    }
}
