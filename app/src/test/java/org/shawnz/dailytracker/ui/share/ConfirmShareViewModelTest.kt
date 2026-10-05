package org.shawnz.dailytracker.ui.share

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.share.ConfirmShareActivity
import org.shawnz.dailytracker.testdoubles.FakeRepository
import org.shawnz.dailytracker.testdoubles.testGame
import org.shawnz.dailytracker.testdoubles.wordleText

@OptIn(ExperimentalCoroutinesApi::class)
class ConfirmShareViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val wordle = testGame(id = 1)
    private val other = testGame(id = 2)
    private val today = wordle.currentPuzzleDay()

    private fun viewModel(
        repo: FakeRepository,
        sharedText: String = wordleText(today),
        matchedGameId: Long? = wordle.id,
    ) = ConfirmShareViewModel(
        repo = repo,
        savedStateHandle =
            SavedStateHandle(
                mapOf(
                    ConfirmShareActivity.EXTRA_SHARED_TEXT to sharedText,
                    ConfirmShareActivity.EXTRA_MATCHED_GAME_ID
                        to (matchedGameId ?: ConfirmShareActivity.NO_GAME),
                ),
            ),
    )

    @Test
    fun `the day comes from the shared text`() =
        runTest(dispatcher) {
            val threeDaysBack = today.minusDays(3)
            val vm =
                viewModel(
                    FakeRepository(games = listOf(wordle)),
                    sharedText = wordleText(threeDaysBack),
                )

            val state = vm.state.first { it.game != null }

            assertEquals(threeDaysBack, state.day)
        }

    // The current puzzle is the likeliest one being shared.
    @Test
    fun `text that names no day falls back to the current puzzle`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(wordle)), sharedText = "nothing here")

            val state = vm.state.first { it.game != null }

            assertEquals(today, state.day)
        }

    @Test
    fun `a day after the current puzzle falls back to the current puzzle`() =
        runTest(dispatcher) {
            val vm =
                viewModel(
                    FakeRepository(games = listOf(wordle)),
                    sharedText = wordleText(today.plusDays(5)),
                )

            val state = vm.state.first { it.game != null }

            assertEquals(today, state.day)
        }

    // The day was worked out for the game that was matched, so it isn't kept for another game.
    @Test
    fun `picking a different game works the day out again`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(wordle, other)))
            vm.pickDay(today.minusDays(5))
            assertEquals(today.minusDays(5), vm.state.first { it.day != null }.day)

            vm.pickGame(other)

            assertEquals(today, vm.state.first { it.game?.id == other.id }.day)
        }

    @Test
    fun `a day already recorded is a clash`() =
        runTest(dispatcher) {
            val existing = EntryEntity(id = 1, gameId = wordle.id, puzzleDay = today)
            val vm = viewModel(FakeRepository(games = listOf(wordle), entries = listOf(existing)))

            val state = vm.state.first { it.game != null }

            assertTrue(state.clash)
            assertEquals(setOf(today), state.playedDays)
        }

    @Test
    fun `an untouched day is no clash`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(wordle)))

            val state = vm.state.first { it.game != null }

            assertFalse(state.clash)
        }

    @Test
    fun `the entries shown are those of the picked game`() =
        runTest(dispatcher) {
            val mine = EntryEntity(id = 1, gameId = other.id, puzzleDay = today)
            val theirs = EntryEntity(id = 2, gameId = wordle.id, puzzleDay = today)
            val repo = FakeRepository(games = listOf(wordle, other), entries = listOf(mine, theirs))
            val vm = viewModel(repo)

            vm.pickGame(other)

            assertEquals(listOf(mine), vm.state.first { it.game?.id == other.id }.entries)
        }

    @Test
    fun `saving writes the result and closes the dialog`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(wordle))
            val vm = viewModel(repo, sharedText = wordleText(today))
            assertFalse(vm.saved.value)

            vm.save(wordle, today).join()

            assertTrue(vm.saved.value)
            val saved = repo.entries.value.single()
            assertEquals(today, saved.puzzleDay)
            assertEquals(wordleText(today), saved.rawShareText)
        }

    // The saved entry is on the chosen day, but it is not shown as a clash.
    @Test
    fun `saving leaves the dialog as it was`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(wordle)))
            backgroundScope.launch { vm.state.collect {} }
            vm.state.first { it.game != null }

            vm.save(wordle, today).join()
            runCurrent()

            assertFalse(vm.state.value.clash)
        }

    @Test
    fun `no game is chosen when the share matched none`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository(games = listOf(wordle)), matchedGameId = null)

            val state = vm.state.first { it.games != null }

            assertEquals(null, state.game)
            assertEquals(null, state.day)
        }
}
