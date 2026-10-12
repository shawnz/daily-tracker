package org.shawnz.dailytracker.testdoubles

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.shawnz.dailytracker.data.ReminderSettings
import org.shawnz.dailytracker.data.SettingsRepository
import java.time.LocalTime

/** A [SettingsRepository] held in memory. */
class FakeSettingsRepository(
    settings: ReminderSettings = ReminderSettings(),
) : SettingsRepository {
    private val state = MutableStateFlow(settings)

    override val reminder: Flow<ReminderSettings> = state

    override suspend fun setEnabled(enabled: Boolean) {
        state.value = state.value.copy(enabled = enabled)
    }

    override suspend fun setTime(time: LocalTime) {
        state.value = state.value.copy(time = time)
    }
}
