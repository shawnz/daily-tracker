package org.shawnz.dailytracker.data

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [GameEntity::class, EntryEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = AppDatabase.DropEntrySuccess::class),
        AutoMigration(from = 2, to = 3),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    abstract fun entryDao(): EntryDao

    @DeleteColumn(tableName = "entries", columnName = "success")
    class DropEntrySuccess : AutoMigrationSpec
}

/**
 * Validates that a game has only a catalog entry, or a title and URL.
 *
 * Uses temporary triggers because Room does not support CHECK constraints.
 */
internal object GameSourceRule : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.execSQL(trigger("games_source_on_insert", "INSERT"))
        db.execSQL(trigger("games_source_on_update", "UPDATE"))
    }

    private fun trigger(
        name: String,
        event: String,
    ) = """
        CREATE TEMP TRIGGER IF NOT EXISTS $name BEFORE $event ON games
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
 * Uses temporary triggers because Room does not support CHECK constraints.
 */
internal object EntryZoneRule : RoomDatabase.Callback() {
    override fun onOpen(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TEMP TRIGGER IF NOT EXISTS entries_zone_on_insert BEFORE INSERT ON entries
            WHEN NEW.completedZone IS NULL
            BEGIN
                SELECT RAISE(ABORT, 'a new entry has a zone');
            END
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TEMP TRIGGER IF NOT EXISTS entries_zone_on_update BEFORE UPDATE ON entries
            WHEN NEW.completedZone IS NULL AND OLD.completedZone IS NOT NULL
            BEGIN
                SELECT RAISE(ABORT, 'a zone cannot be removed from an entry');
            END
            """.trimIndent(),
        )
    }
}
