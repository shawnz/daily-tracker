package org.shawnz.dailytracker.ui.stats

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object Stats : NavKey

fun MutableList<NavKey>.navigateToStats() {
    add(Stats)
}

fun EntryProviderScope<NavKey>.statsEntry(onBack: () -> Unit) {
    entry<Stats> {
        StatsScreen(onBack = onBack)
    }
}
