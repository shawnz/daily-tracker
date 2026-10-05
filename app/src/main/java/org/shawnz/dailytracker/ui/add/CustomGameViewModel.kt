package org.shawnz.dailytracker.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.Repository
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.ui.STOP_TIMEOUT_MILLIS
import javax.inject.Inject

@HiltViewModel
class CustomGameViewModel
    @Inject
    constructor(
        private val repo: Repository,
    ) : ViewModel() {
        /**
         * True once the form has been submitted. [games] is not updated after that, so the link
         * is not matched against the game being added.
         */
        private var frozen = false

        /** Archived games included, so a link is matched against everything already added. */
        val games: StateFlow<List<Game>?> =
            repo
                .observeAllGames()
                .takeWhile { !frozen }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), null)

        private val _added = MutableStateFlow(false)

        /** True once a game has been added, so the screen should close. */
        val added: StateFlow<Boolean> = _added.asStateFlow()

        fun addCustom(
            title: String,
            url: String,
        ) = viewModelScope.launch {
            if (frozen) return@launch
            frozen = true
            repo.addCustom(title, url)
            _added.value = true
        }

        fun addFromCatalog(game: CatalogGame) =
            viewModelScope.launch {
                if (frozen) return@launch
                frozen = true
                repo.addFromCatalog(game)
                _added.value = true
            }

        fun restore(game: Game) =
            viewModelScope.launch {
                if (frozen) return@launch
                frozen = true
                repo.setArchived(game, false)
                _added.value = true
            }
    }
