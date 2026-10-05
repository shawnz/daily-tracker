package org.shawnz.dailytracker.ui.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import org.shawnz.dailytracker.data.ReminderSettings
import org.shawnz.dailytracker.data.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) : ViewModel() {
        val reminder: StateFlow<ReminderSettings> = settings.reminder

        fun setEnabled(enabled: Boolean) = settings.setEnabled(enabled)

        fun setTime(
            hour: Int,
            minute: Int,
        ) = settings.setTime(hour, minute)
    }
