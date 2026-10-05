package org.shawnz.dailytracker.ui.today

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object Today : NavKey

/** Today is always the first entry. */
fun MutableList<NavKey>.popToToday() {
    while (size > 1) removeAt(size - 1)
}

fun EntryProviderScope<NavKey>.todayEntry(
    onOpenGame: (Long) -> Unit,
    onAdd: () -> Unit,
    onStats: () -> Unit,
    onSettings: () -> Unit,
) {
    entry<Today> {
        TodayScreen(
            onOpenGame = onOpenGame,
            onAdd = onAdd,
            onStats = onStats,
            onSettings = onSettings,
        )
    }
}
