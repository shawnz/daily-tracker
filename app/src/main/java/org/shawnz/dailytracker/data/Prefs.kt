package org.shawnz.dailytracker.data

import android.content.Context
import androidx.core.content.edit

class Prefs(
    context: Context,
) {
    private val prefs =
        context.applicationContext
            .getSharedPreferences("daily-tracker", Context.MODE_PRIVATE)

    var remindersEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    var reminderHour: Int
        get() = prefs.getInt(KEY_HOUR, 20)
        set(value) = prefs.edit { putInt(KEY_HOUR, value) }

    var reminderMinute: Int
        get() = prefs.getInt(KEY_MINUTE, 0)
        set(value) = prefs.edit { putInt(KEY_MINUTE, value) }

    private companion object {
        const val KEY_ENABLED = "reminders_enabled"
        const val KEY_HOUR = "reminder_hour"
        const val KEY_MINUTE = "reminder_minute"
    }
}
