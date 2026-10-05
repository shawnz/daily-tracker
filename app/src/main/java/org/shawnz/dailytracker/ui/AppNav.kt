package org.shawnz.dailytracker.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.shawnz.dailytracker.ui.add.addGameEntry
import org.shawnz.dailytracker.ui.add.customGameEntry
import org.shawnz.dailytracker.ui.add.navigateToAdd
import org.shawnz.dailytracker.ui.add.navigateToAddCustom
import org.shawnz.dailytracker.ui.detail.gameDetailEntry
import org.shawnz.dailytracker.ui.detail.navigateToGameDetail
import org.shawnz.dailytracker.ui.settings.navigateToSettings
import org.shawnz.dailytracker.ui.settings.settingsEntry
import org.shawnz.dailytracker.ui.stats.navigateToStats
import org.shawnz.dailytracker.ui.stats.statsEntry
import org.shawnz.dailytracker.ui.today.Today
import org.shawnz.dailytracker.ui.today.popToToday
import org.shawnz.dailytracker.ui.today.todayEntry

@Composable
fun AppNav() {
    val backStack = rememberNavBackStack(Today)

    NavDisplay(
        backStack = backStack,
        entryDecorators =
            listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
        entryProvider =
            entryProvider {
                todayEntry(
                    onOpenGame = { backStack.navigateToGameDetail(it) },
                    onAdd = { backStack.navigateToAdd() },
                    onStats = { backStack.navigateToStats() },
                    onSettings = { backStack.navigateToSettings() },
                )
                addGameEntry(
                    onBack = { backStack.removeLastOrNull() },
                    onCreateCustom = { backStack.navigateToAddCustom() },
                    onAdded = { backStack.removeLastOrNull() },
                )
                customGameEntry(
                    onBack = { backStack.removeLastOrNull() },
                    onAdded = { backStack.popToToday() },
                )
                statsEntry(onBack = { backStack.removeLastOrNull() })
                settingsEntry(onBack = { backStack.removeLastOrNull() })
                gameDetailEntry(onBack = { backStack.removeLastOrNull() })
            },
    )
}
