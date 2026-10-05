package org.shawnz.dailytracker.ui.detail

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data class GameDetail(
    val gameId: Long,
) : NavKey

fun MutableList<NavKey>.navigateToGameDetail(gameId: Long) {
    add(GameDetail(gameId))
}

fun EntryProviderScope<NavKey>.gameDetailEntry(onBack: () -> Unit) {
    entry<GameDetail> { key ->
        GameDetailScreen(gameId = key.gameId, onBack = onBack)
    }
}
