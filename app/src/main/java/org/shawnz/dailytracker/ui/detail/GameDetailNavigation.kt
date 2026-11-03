package org.shawnz.dailytracker.ui.detail

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import org.shawnz.dailytracker.ui.LocalDateSerializer
import java.time.LocalDate

/** @property day The day the page opens on. */
@Serializable
data class GameDetail(
    val gameId: Long,
    @Serializable(with = LocalDateSerializer::class) val day: LocalDate,
) : NavKey

fun MutableList<NavKey>.navigateToGameDetail(
    gameId: Long,
    day: LocalDate,
) {
    add(GameDetail(gameId, day))
}

fun EntryProviderScope<NavKey>.gameDetailEntry(onBack: () -> Unit) {
    entry<GameDetail> { key ->
        GameDetailScreen(gameId = key.gameId, day = key.day, onBack = onBack)
    }
}
