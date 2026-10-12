package org.shawnz.dailytracker.data

import android.util.Log
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalTime
import javax.inject.Inject

/** The daily reminder, as the user set it. */
data class ReminderSettings(
    val enabled: Boolean = true,
    val time: LocalTime = LocalTime.of(20, 0),
)

interface SettingsRepository {
    val reminder: Flow<ReminderSettings>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setTime(time: LocalTime)
}

private const val TAG = "SettingsRepository"

/**
 * Reads and writes the settings in [store].
 *
 * When [store] cannot be read, the settings have their defaults. When it cannot be written,
 * the change is lost.
 */
class DefaultSettingsRepository
    @Inject
    constructor(
        private val store: DataStore<StoredSettings>,
    ) : SettingsRepository {
        override val reminder: Flow<ReminderSettings> =
            store.data
                .catch { exception ->
                    if (exception is IOException) {
                        Log.e(TAG, "Failed to read the settings", exception)
                        emit(StoredSettings.getDefaultInstance())
                    } else {
                        throw exception
                    }
                }.map { it.toReminderSettings() }

        override suspend fun setEnabled(enabled: Boolean) = update { remindersEnabled = enabled }

        override suspend fun setTime(time: LocalTime) = update { reminderTime = time.toSecondOfDay() }

        private suspend fun update(change: StoredSettingsKt.Dsl.() -> Unit) {
            try {
                store.updateData { it.copy(change) }
            } catch (ioException: IOException) {
                Log.e(TAG, "Failed to write the settings", ioException)
            }
        }
    }

/** A setting that was never written has its default from [ReminderSettings]. */
private fun StoredSettings.toReminderSettings(): ReminderSettings {
    val defaults = ReminderSettings()
    return ReminderSettings(
        enabled = if (hasRemindersEnabled()) remindersEnabled else defaults.enabled,
        time = if (hasReminderTime()) LocalTime.ofSecondOfDay(reminderTime.toLong()) else defaults.time,
    )
}
