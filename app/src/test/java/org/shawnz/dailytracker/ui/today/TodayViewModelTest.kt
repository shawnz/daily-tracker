package org.shawnz.dailytracker.ui.today

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
import org.shawnz.dailytracker.data.PuzzleStatus
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.FakeRepository
import org.shawnz.dailytracker.testdoubles.testGame
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

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
            puzzleDay = wordle.possiblePuzzleDays(entries = emptyList()).start,
        )

    @Test
    fun `a game with no entry for today is not done`() =
        runTest(dispatcher) {
            val vm = TodayViewModel(FakeRepository(games = listOf(wordle)))

            val state = vm.state.first { it != null }!!

            assertTrue(state.rows.single().status is PuzzleStatus.Unplayed)
            assertEquals(0, state.doneCount)
        }

    @Test
    fun `a game with an entry for today is done`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(wordle), entries = listOf(entryToday()))
            val vm = TodayViewModel(repo)

            val state = vm.state.first { it != null }!!

            assertTrue(state.rows.single().status is PuzzleStatus.Done)
            assertEquals(1, state.doneCount)
        }

    @Test
    fun `an entry for today saved on an earlier date is done`() =
        runTest(dispatcher) {
            val saved = entryToday().copy(completedAt = Instant.now().minus(Duration.ofDays(2)))
            val vm = TodayViewModel(FakeRepository(listOf(wordle), listOf(saved)))

            val state = vm.state.first { it != null }!!

            assertEquals(PuzzleStatus.Done(saved), state.rows.single().status)
            assertEquals(1, state.doneCount)
        }

    // The entry has to be for this game's own puzzle day, not just any day it has played.
    @Test
    fun `yesterday's entry does not make today done`() =
        runTest(dispatcher) {
            val yesterday =
                entryToday().copy(
                    puzzleDay = wordle.possiblePuzzleDays(entries = emptyList()).start.minusDays(1),
                )
            val vm = TodayViewModel(FakeRepository(listOf(wordle), listOf(yesterday)))

            val state = vm.state.first { it != null }!!

            assertTrue(state.rows.single().status is PuzzleStatus.Unplayed)
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

    @Test
    fun `the page opens on the device's date`() =
        runTest(dispatcher) {
            val vm = TodayViewModel(FakeRepository(games = listOf(wordle)))

            val state = vm.state.first { it != null }!!

            assertEquals(LocalDate.now(), state.day)
        }

    @Test
    fun `with no results, the page can't go back`() =
        runTest(dispatcher) {
            val vm = TodayViewModel(FakeRepository(games = listOf(wordle)))

            assertFalse(vm.state.first { it != null }!!.canGoBack)
        }

    // Its puzzle changes at local midnight, so no later puzzle day is possible.
    @Test
    fun `the page can't go past the latest possible puzzle day`() =
        runTest(dispatcher) {
            val vm = TodayViewModel(FakeRepository(games = listOf(wordle)))
            backgroundScope.launch { vm.state.collect {} }
            vm.state.first { it != null }

            vm.stepDay(1)
            runCurrent()

            assertEquals(LocalDate.now(), vm.state.value!!.day)
            assertFalse(vm.state.value!!.canGoForward)
        }

    @Test
    fun `the page goes back as far as the earliest result and shows it`() =
        runTest(dispatcher) {
            val earliest = entryToday().copy(puzzleDay = entryToday().puzzleDay.minusDays(2))
            val vm = TodayViewModel(FakeRepository(listOf(wordle), listOf(earliest)))
            backgroundScope.launch { vm.state.collect {} }
            vm.state.first { it != null }

            repeat(3) {
                vm.stepDay(-1)
                runCurrent()
            }

            val state = vm.state.value!!
            assertEquals(earliest.puzzleDay, state.day)
            assertFalse(state.canGoBack)
            assertEquals(PuzzleStatus.Done(earliest), state.rows.single().status)
        }

    @Test
    fun `a past puzzle with no result is missed`() =
        runTest(dispatcher) {
            val earliest = entryToday().copy(puzzleDay = entryToday().puzzleDay.minusDays(2))
            val vm = TodayViewModel(FakeRepository(listOf(wordle), listOf(earliest)))
            backgroundScope.launch { vm.state.collect {} }
            vm.state.first { it != null }

            vm.stepDay(-1)
            runCurrent()

            val state = vm.state.value!!
            assertEquals(PuzzleStatus.Missed(currentPuzzleUnplayed = true), state.rows.single().status)
        }

    // Null means not loaded yet, so the empty state isn't shown while loading.
    @Test
    fun `the state is null until the games have been read`() =
        runTest(dispatcher) {
            val vm = TodayViewModel(FakeRepository())
            assertEquals(null, vm.state.value)
        }
}
