package org.shawnz.dailytracker.ui.settings

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object Settings : NavKey

fun MutableList<NavKey>.navigateToSettings() {
    add(Settings)
}

fun EntryProviderScope<NavKey>.settingsEntry(onBack: () -> Unit) {
    entry<Settings> {
        SettingsScreen(onBack = onBack)
    }
}
