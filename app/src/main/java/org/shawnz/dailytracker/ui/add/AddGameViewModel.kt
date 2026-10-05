package org.shawnz.dailytracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.ui.STOP_TIMEOUT_MILLIS
import javax.inject.Inject

@HiltViewModel
class AddGameViewModel
    @Inject
    constructor(
        private val repo: Repository,
    ) : ViewModel() {
        /**
         * True once a game has been picked. [available] and [archived] are not updated after
         * that, so the picked game stays listed.
         */
        private var frozen = false

        /**
         * The catalog games that aren't tracked, hidden games included.
         *
         * Null until the tracked games have loaded, so a tracked game isn't listed while they
         * load.
         */
        val available: StateFlow<List<CatalogGame>?> =
            repo
                .observeTrackedCatalogGames()
                .map { tracked -> CatalogGame.entries.filterNot { it in tracked } }
                .takeWhile { !frozen }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        /**
         * The archived games. Both catalog and user-made games are listed, since a user-made game
         * has no catalog entry.
         */
        val archived: StateFlow<List<Game>?> =
            repo
                .observeArchivedGames()
                .takeWhile { !frozen }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        private val _added = MutableStateFlow(false)

        /** True once a game has been added, so the screen should close. */
        val added: StateFlow<Boolean> = _added.asStateFlow()

        fun add(game: CatalogGame) =
            viewModelScope.launch {
                if (frozen) return@launch
                frozen = true
                repo.addFromCatalog(game)
                _added.value = true
            }

        /** Puts an archived game back, with its entries and list position unchanged. */
        fun restore(game: Game) =
            viewModelScope.launch {
                if (frozen) return@launch
                frozen = true
                repo.setArchived(game, false)
                _added.value = true
            }
    }
