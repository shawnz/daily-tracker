package org.shawnz.dailytracker.ui.add

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
object Add : NavKey

@Serializable
object AddCustom : NavKey

fun MutableList<NavKey>.navigateToAdd() {
    add(Add)
}

fun MutableList<NavKey>.navigateToAddCustom() {
    add(AddCustom)
}

fun EntryProviderScope<NavKey>.addGameEntry(
    onBack: () -> Unit,
    onCreateCustom: () -> Unit,
    onAdded: () -> Unit,
) {
    entry<Add> {
        AddGameScreen(onBack = onBack, onCreateCustom = onCreateCustom, onAdded = onAdded)
    }
}

fun EntryProviderScope<NavKey>.customGameEntry(
    onBack: () -> Unit,
    onAdded: () -> Unit,
) {
    entry<AddCustom> {
        CustomGameScreen(onBack = onBack, onAdded = onAdded)
    }
}
