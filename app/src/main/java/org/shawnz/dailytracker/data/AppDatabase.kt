package org.shawnz.dailytracker.data

import androidx.room3.AutoMigration
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.DeleteColumn
import androidx.room3.RoomDatabase
import androidx.room3.migration.AutoMigrationSpec
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

@Database(
    entities = [GameEntity::class, EntryEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = AppDatabase.DropEntrySuccess::class),
        AutoMigration(from = 2, to = 3),
    ],
)
@ColumnTypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    abstract fun entryDao(): EntryDao

    @DeleteColumn(tableName = "entries", columnName = "success")
    class DropEntrySuccess : AutoMigrationSpec
}

/**
 * Returns the database built from [builder], with the SQLite driver and the rules shared by every
 * copy of the database. Only the location of the database comes from [builder].
 */
internal fun buildAppDatabase(builder: RoomDatabase.Builder<AppDatabase>): AppDatabase =
    builder
        .setDriver(BundledSQLiteDriver())
        .addCallback(GameSourceRule)
        .addCallback(EntryZoneRule)
        .build()

/**
 * Validates that a game has only a catalog entry, or a title and URL.
 *
 * Uses triggers because Room does not support CHECK constraints.
 */
private object GameSourceRule : RoomDatabase.Callback() {
    override suspend fun onOpen(connection: SQLiteConnection) {
        connection.replaceTriggers(
            mapOf(
                "games_source_on_insert" to trigger("INSERT"),
                "games_source_on_update" to trigger("UPDATE"),
            ),
        )
    }

    private fun trigger(event: String) =
        """
        BEFORE $event ON games
        WHEN NOT (
            (NEW.catalogGame IS NOT NULL AND NEW.title IS NULL AND NEW.url IS NULL)
            OR (NEW.catalogGame IS NULL AND NEW.title IS NOT NULL AND NEW.url IS NOT NULL)
        )
        BEGIN
            SELECT RAISE(ABORT, 'a game is a catalog game, or has a title and url, and not both');
        END
        """.trimIndent()
}

/**
 * Validates that a new entry has a [EntryEntity.completedZone], and that a zone is not removed by
 * an edit. An entry saved before database version 3 can still be edited without a zone.
 *
 * Uses triggers because Room does not support CHECK constraints.
 */
private object EntryZoneRule : RoomDatabase.Callback() {
    override suspend fun onOpen(connection: SQLiteConnection) {
        connection.replaceTriggers(
            mapOf(
                "entries_zone_on_insert" to
                    """
                    BEFORE INSERT ON entries
                    WHEN NEW.completedZone IS NULL
                    BEGIN
                        SELECT RAISE(ABORT, 'a new entry has a zone');
                    END
                    """.trimIndent(),
                "entries_zone_on_update" to
                    """
                    BEFORE UPDATE ON entries
                    WHEN NEW.completedZone IS NULL AND OLD.completedZone IS NOT NULL
                    BEGIN
                        SELECT RAISE(ABORT, 'a zone cannot be removed from an entry');
                    END
                    """.trimIndent(),
            ),
        )
    }
}

/**
 * Drops and creates each trigger in [triggers], a map from trigger name to the definition that
 * follows the name in `CREATE TRIGGER`.
 *
 * The triggers are stored in the database, so they apply on every connection to it. Each one is
 * dropped first, so a changed definition takes effect. The changes are made in one
 * transaction, so a write from another process waits until every trigger is in place.
 */
private fun SQLiteConnection.replaceTriggers(triggers: Map<String, String>) {
    execSQL("BEGIN")
    try {
        triggers.forEach { (name, definition) ->
            execSQL("DROP TRIGGER IF EXISTS $name")
            execSQL("CREATE TRIGGER $name $definition")
        }
        execSQL("COMMIT")
    } catch (error: Throwable) {
        execSQL("ROLLBACK")
        throw error
    }
}
