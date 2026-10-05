package org.shawnz.dailytracker.ui.today

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.FakeRepository
import org.shawnz.dailytracker.testdoubles.testGame

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val wordle = testGame(id = 1, source = CatalogGame.WORDLE)

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private fun entryToday() =
        EntryEntity(
            id = 1,
            gameId = wordle.id,
            puzzleDay = wordle.currentPuzzleDay(),
        )

    @Test
    fun `a game with no entry for today is not done`() =
        runTest(dispatcher) {
            val vm = TodayViewModel(FakeRepository(games = listOf(wordle)))

            val state = vm.state.first { it != null }!!

            assertFalse(state.rows.single().done)
            assertEquals(0, state.doneCount)
        }

    @Test
    fun `a game with an entry for today is done`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(wordle), entries = listOf(entryToday()))
            val vm = TodayViewModel(repo)

            val state = vm.state.first { it != null }!!

            assertTrue(state.rows.single().done)
            assertEquals(1, state.doneCount)
        }

    // The entry has to be for this game's own puzzle day, not just any day it has played.
    @Test
    fun `yesterday's entry does not make today done`() =
        runTest(dispatcher) {
            val yesterday =
                entryToday().copy(
                    puzzleDay = wordle.currentPuzzleDay().minusDays(1),
                )
            val vm = TodayViewModel(FakeRepository(listOf(wordle), listOf(yesterday)))

            val state = vm.state.first { it != null }!!

            assertFalse(state.rows.single().done)
        }

    @Test
    fun `an archived game is not on the list`() =
        runTest(dispatcher) {
            val repo =
                FakeRepository(games = listOf(wordle, testGame(id = 2).copy(archived = true)))
            val vm = TodayViewModel(repo)

            val state = vm.state.first { it != null }!!

            assertEquals(listOf(1L), state.rows.map { it.game.id })
        }

    // Null means not loaded yet, so the empty state isn't shown while loading.
    @Test
    fun `the state is null until the games have been read`() =
        runTest(dispatcher) {
            val vm = TodayViewModel(FakeRepository())
            assertEquals(null, vm.state.value)
        }
}
