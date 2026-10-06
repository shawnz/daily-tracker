package org.shawnz.dailytracker.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Each migration, run on a database created from the exported schema of the older version. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AppDatabaseMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AppDatabase::class.java,
        )

    @Test
    fun `entries are kept from version 1 to 2`() {
        helper.createDatabase(NAME, 1).use { db ->
            db.execSQL(
                "INSERT INTO games (id, catalogGame, sortOrder, archived, remindersEnabled, createdAt) " +
                    "VALUES (1, 'WORDLE', 0, 0, 1, 0)",
            )
            db.execSQL(
                "INSERT INTO entries (id, gameId, puzzleDay, completedAt, rawShareText, success) " +
                    "VALUES (7, 1, 20000, 5, 'Wordle 1,234 4/6', 1)",
            )
        }

        helper.runMigrationsAndValidate(NAME, 2, true).use { db ->
            db.query("SELECT id, gameId, puzzleDay, completedAt, rawShareText FROM entries").use { rows ->
                assertTrue(rows.moveToFirst())
                assertEquals(7L, rows.getLong(0))
                assertEquals(1L, rows.getLong(1))
                assertEquals(20000L, rows.getLong(2))
                assertEquals(5L, rows.getLong(3))
                assertEquals("Wordle 1,234 4/6", rows.getString(4))
                assertFalse(rows.moveToNext())
            }
        }
    }

    private companion object {
        const val NAME = "migration-test.db"
    }
}
