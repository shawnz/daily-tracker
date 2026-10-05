package org.shawnz.dailytracker.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [GameEntity::class, EntryEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao

    abstract fun entryDao(): EntryDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room
                    .databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "daily-tracker.db",
                    ).addCallback(GameSourceRule)
                    .build()
                    .also { instance = it }
            }
    }
}

/**
 * Validates that a game has only a catalog entry, or a title and URL.
 *
 * Uses temporary triggers because Room does not support CHECK constraints.
 */
@VisibleForTesting
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
