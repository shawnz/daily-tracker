package org.shawnz.dailytracker.data

import androidx.room3.testing.MigrationTestHelper
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Each migration, run on a database created from the exported schema of the older version. */
@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper =
        MigrationTestHelper(
            instrumentation = instrumentation,
            file = instrumentation.targetContext.getDatabasePath("migration-test.db"),
            driver = BundledSQLiteDriver(),
            databaseClass = AppDatabase::class,
        )

    @Test
    fun `entries are kept from version 1 to 2`() {
        runBlocking {
            helper.createDatabase(1).use { db ->
                db.execSQL(
                    "INSERT INTO games (id, catalogGame, sortOrder, archived, remindersEnabled, createdAt) " +
                        "VALUES (1, 'WORDLE', 0, 0, 1, 0)",
                )
                db.execSQL(
                    "INSERT INTO entries (id, gameId, puzzleDay, completedAt, rawShareText, success) " +
                        "VALUES (7, 1, 20000, 5, 'Wordle 1,234 4/6', 1)",
                )
            }

            helper.runMigrationsAndValidate(2).use { db ->
                db.singleRow("SELECT id, gameId, puzzleDay, completedAt, rawShareText FROM entries") { row ->
                    assertEquals(7L, row.getLong(0))
                    assertEquals(1L, row.getLong(1))
                    assertEquals(20000L, row.getLong(2))
                    assertEquals(5L, row.getLong(3))
                    assertEquals("Wordle 1,234 4/6", row.getText(4))
                }
            }
        }
    }

    @Test
    fun `entries are kept from version 2 to 3 with no zone`() {
        runBlocking {
            helper.createDatabase(2).use { db ->
                db.execSQL(
                    "INSERT INTO games (id, catalogGame, sortOrder, archived, remindersEnabled, createdAt) " +
                        "VALUES (1, 'WORDLE', 0, 0, 1, 0)",
                )
                db.execSQL(
                    "INSERT INTO entries (id, gameId, puzzleDay, completedAt, rawShareText) " +
                        "VALUES (7, 1, 20000, 5, 'Wordle 1,234 4/6')",
                )
            }

            helper.runMigrationsAndValidate(3).use { db ->
                db.singleRow("SELECT id, gameId, puzzleDay, completedAt, completedZone, rawShareText FROM entries") { row ->
                    assertEquals(7L, row.getLong(0))
                    assertEquals(1L, row.getLong(1))
                    assertEquals(20000L, row.getLong(2))
                    assertEquals(5L, row.getLong(3))
                    assertTrue(row.isNull(4))
                    assertEquals("Wordle 1,234 4/6", row.getText(5))
                }
            }
        }
    }
}

/** Runs [sql], checks that the result has exactly one row, and passes that row to [check]. */
private fun SQLiteConnection.singleRow(
    sql: String,
    check: (SQLiteStatement) -> Unit,
) {
    prepare(sql).use { row ->
        assertTrue(row.step())
        check(row)
        assertFalse(row.step())
    }
}
