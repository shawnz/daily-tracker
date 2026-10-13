package org.shawnz.dailytracker.data

import android.util.Log
import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.Duration
import java.time.LocalTime
import javax.inject.Inject

/**
 * How the reminder time is set.
 *
 * [FIXED_TIME] is one time of day for every game. [BEFORE_RESET] is a set time before each
 * game's puzzle day changes, so games with different rollovers have different reminder times.
 */
enum class ReminderMode { FIXED_TIME, BEFORE_RESET }

/**
 * The reminders, as the user set them.
 *
 * @property time The reminder time with [ReminderMode.FIXED_TIME].
 * @property leadTime With [ReminderMode.BEFORE_RESET], how long a game's reminder time is
 *   before its puzzle day changes.
 */
data class ReminderSettings(
    val enabled: Boolean = true,
    val mode: ReminderMode = ReminderMode.FIXED_TIME,
    val time: LocalTime = LocalTime.of(20, 0),
    val leadTime: Duration = Duration.ofHours(4),
)

interface SettingsRepository {
    val reminder: Flow<ReminderSettings>

    suspend fun setEnabled(enabled: Boolean)

    suspend fun setMode(mode: ReminderMode)

    suspend fun setTime(time: LocalTime)

    suspend fun setLeadTime(leadTime: Duration)
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

        override suspend fun setMode(mode: ReminderMode) =
            update {
                reminderMode =
                    when (mode) {
                        ReminderMode.FIXED_TIME -> ReminderModeProto.REMINDER_MODE_FIXED_TIME
                        ReminderMode.BEFORE_RESET -> ReminderModeProto.REMINDER_MODE_BEFORE_RESET
                    }
            }

        override suspend fun setTime(time: LocalTime) = update { reminderTime = time.toSecondOfDay() }

        override suspend fun setLeadTime(leadTime: Duration) = update { reminderLeadTimeSeconds = leadTime.seconds.toInt() }

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
        mode =
            when (reminderMode) {
                null,
                ReminderModeProto.REMINDER_MODE_UNSPECIFIED,
                ReminderModeProto.UNRECOGNIZED,
                -> defaults.mode

                ReminderModeProto.REMINDER_MODE_FIXED_TIME -> ReminderMode.FIXED_TIME

                ReminderModeProto.REMINDER_MODE_BEFORE_RESET -> ReminderMode.BEFORE_RESET
            },
        time = if (hasReminderTime()) LocalTime.ofSecondOfDay(reminderTime.toLong()) else defaults.time,
        leadTime =
            if (hasReminderLeadTimeSeconds()) {
                Duration.ofSeconds(reminderLeadTimeSeconds.toLong())
            } else {
                defaults.leadTime
            },
    )
}
