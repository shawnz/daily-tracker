package org.shawnz.dailytracker.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.shawnz.dailytracker.work.Reminders
import javax.inject.Inject
import javax.inject.Singleton

/** The daily reminder, as the user set it. */
data class ReminderSettings(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int,
)

interface SettingsRepository {
    val reminder: StateFlow<ReminderSettings>

    fun setEnabled(enabled: Boolean)

    fun setTime(
        hour: Int,
        minute: Int,
    )
}

/**
 * Backed by [Prefs], which are read once and then kept in a flow. Changing a setting also
 * reschedules the reminder.
 */
@Singleton
class DefaultSettingsRepository
    @Inject
    constructor(
        private val prefs: Prefs,
        @ApplicationContext private val context: Context,
    ) : SettingsRepository {
        private val state =
            MutableStateFlow(
                ReminderSettings(prefs.remindersEnabled, prefs.reminderHour, prefs.reminderMinute),
            )

        override val reminder: StateFlow<ReminderSettings> = state.asStateFlow()

        override fun setEnabled(enabled: Boolean) {
            prefs.remindersEnabled = enabled
            state.value = state.value.copy(enabled = enabled)
            Reminders.reschedule(context, state.value)
        }

        override fun setTime(
            hour: Int,
            minute: Int,
        ) {
            prefs.reminderHour = hour
            prefs.reminderMinute = minute
            state.value = state.value.copy(hour = hour, minute = minute)
            Reminders.reschedule(context, state.value)
        }
    }
