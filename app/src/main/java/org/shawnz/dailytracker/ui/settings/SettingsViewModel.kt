package org.shawnz.dailytracker.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.shawnz.dailytracker.data.ReminderMode
import org.shawnz.dailytracker.data.ReminderSettings
import org.shawnz.dailytracker.data.SettingsRepository
import org.shawnz.dailytracker.ui.STOP_TIMEOUT_MILLIS
import java.time.Duration
import java.time.LocalTime
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val settings: SettingsRepository,
    ) : ViewModel() {
        /** Null until the settings are read. */
        val reminder: StateFlow<ReminderSettings?> =
            settings.reminder
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        fun setEnabled(enabled: Boolean) {
            viewModelScope.launch { settings.setEnabled(enabled) }
        }

        fun setMode(mode: ReminderMode) {
            viewModelScope.launch { settings.setMode(mode) }
        }

        /** Sets the time of the fixed reminder, and selects that mode. */
        fun pickTime(time: LocalTime) {
            viewModelScope.launch {
                settings.setTime(time)
                settings.setMode(ReminderMode.FIXED_TIME)
            }
        }

        /** Sets the lead time of the reminders before a reset, and selects that mode. */
        fun pickLeadTime(leadTime: Duration) {
            viewModelScope.launch {
                settings.setLeadTime(leadTime)
                settings.setMode(ReminderMode.BEFORE_RESET)
            }
        }
    }
