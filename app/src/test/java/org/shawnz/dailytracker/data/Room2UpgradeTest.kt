package org.shawnz.dailytracker.data

import androidx.room3.Room
import androidx.sqlite.SQLiteException
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.shawnz.dailytracker.data.catalog.CatalogGame
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZoneId

/**
 * A database file written by Room 2.8.5 at schema version 3, as installed by version 1.7 of the
 * app, opened with [buildAppDatabase].
 *
 * The file was written with the app's Room 2 builder and DAOs. It has a catalog game with one
 * entry and a user-made game.
 */
class Room2UpgradeTest {
    @TempDir
    lateinit var directory: Path

    private lateinit var db: AppDatabase

    @BeforeEach
    fun open() {
        val file = directory.resolve("daily-tracker.db")
        javaClass.getResourceAsStream("/room2-version-3.db")!!.use { Files.copy(it, file) }
        db = buildAppDatabase(Room.databaseBuilder<AppDatabase>(file.toString()))
    }

    @AfterEach
    fun close() {
        db.close()
    }

    @Test
    fun `the games and entries are read`() {
        runBlocking {
            val games = db.gameDao().getActive().sortedBy { it.sortOrder }
            assertEquals(listOf(CatalogGame.WORDLE, null), games.map { it.catalogGame })
            assertEquals("https://alpha.example", games[1].url)

            val entry = db.entryDao().find(games[0].id, LocalDate.of(2024, 11, 4))!!
            assertEquals(ZoneId.of("America/Toronto"), entry.completedZone)
            assertEquals("Wordle 1,234 4/6", entry.rawShareText?.lineSequence()?.first())
        }
    }

    // The read is on the first connection opened, so the write is on a later one. The open
    // callbacks are run on the first connection only.
    @Test
    fun `the rules apply to a write after a read`() {
        runBlocking {
            db.gameDao().getActive()
            db.entryDao().getForGame(1)
            assertThrows(SQLiteException::class.java) { runBlocking { db.gameDao().insert(GameEntity()) } }
        }
    }

    @Test
    fun `the rules apply to writes`() {
        runBlocking {
            assertThrows(SQLiteException::class.java) { runBlocking { db.gameDao().insert(GameEntity()) } }
            val id =
                db
                    .gameDao()
                    .getActive()
                    .first()
                    .id
            assertThrows(SQLiteException::class.java) {
                runBlocking {
                    db.entryDao().insert(EntryEntity(gameId = id, puzzleDay = LocalDate.of(2024, 11, 5), completedZone = null))
                }
            }
        }
    }
}
