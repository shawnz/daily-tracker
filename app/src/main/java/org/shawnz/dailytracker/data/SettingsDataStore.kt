package org.shawnz.dailytracker.data

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Serializer
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import androidx.datastore.migrations.SharedPreferencesMigration
import androidx.datastore.migrations.SharedPreferencesView
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.CoroutineScope
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalTime

// The settings were in this shared preferences file up to version 1.3.
private const val OLD_FILE = "daily-tracker"
private const val OLD_ENABLED = "reminders_enabled"
private const val OLD_HOUR = "reminder_hour"
private const val OLD_MINUTE = "reminder_minute"

/**
 * Opens the settings file of [context]. The file is read and written in [scope], and is open
 * until [scope] is cancelled.
 *
 * On the first read, the settings in the shared preferences file are moved to the settings
 * file.
 */
fun settingsDataStore(
    context: Context,
    scope: CoroutineScope,
): DataStore<StoredSettings> =
    DataStoreFactory.create(
        serializer = StoredSettingsSerializer,
        corruptionHandler = ReplaceFileCorruptionHandler { StoredSettings.getDefaultInstance() },
        migrations = listOf(SharedPreferencesMigration(context, OLD_FILE, migrate = ::moveOldSettings)),
        scope = scope,
    ) { context.dataStoreFile("settings.pb") }

private object StoredSettingsSerializer : Serializer<StoredSettings> {
    override val defaultValue: StoredSettings = StoredSettings.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): StoredSettings =
        try {
            StoredSettings.parseFrom(input)
        } catch (e: InvalidProtocolBufferException) {
            throw CorruptionException("The settings file cannot be parsed.", e)
        }

    override suspend fun writeTo(
        t: StoredSettings,
        output: OutputStream,
    ) = t.writeTo(output)
}

private fun moveOldSettings(
    old: SharedPreferencesView,
    current: StoredSettings,
): StoredSettings =
    current.copy {
        if (old.contains(OLD_ENABLED)) remindersEnabled = old.getBoolean(OLD_ENABLED, true)
        if (old.contains(OLD_HOUR)) {
            val time = LocalTime.of(old.getInt(OLD_HOUR, 0), old.getInt(OLD_MINUTE, 0))
            reminderTime = time.toSecondOfDay()
        }
    }
