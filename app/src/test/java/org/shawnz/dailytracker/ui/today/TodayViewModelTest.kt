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
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
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

    private suspend fun TodayViewModel.loadedFrame() = frame.first { it != null }!!

    private suspend fun TodayViewModel.loadedState(day: LocalDate) = stateOn(day).first { it != null }!!

    @Test
    fun `a game with no entry for today is not done`() =
        runTest(dispatcher) {
            val vm = TodayViewModel(FakeRepository(games = listOf(wordle)))

            val state = vm.loadedState(LocalDate.now())

            assertTrue(state.rows.single().status is PuzzleStatus.Unplayed)
            assertEquals(0, state.doneCount)
        }

    @Test
    fun `a game with an entry for today is done`() =
        runTest(dispatcher) {
            val repo = FakeRepository(games = listOf(wordle), entries = listOf(entryToday()))
            val vm = TodayViewModel(repo)

            val state = vm.loadedState(LocalDate.now())

            assertTrue(state.rows.single().status is PuzzleStatus.Done)
            assertEquals(1, state.doneCount)
        }

    @Test
    fun `an entry for today saved on an earlier date is done`() =
        runTest(dispatcher) {
            val saved = entryToday().copy(completedAt = Instant.now().minus(Duration.ofDays(2)))
            val vm = TodayViewModel(FakeRepository(listOf(wordle), listOf(saved)))

            val state = vm.loadedState(LocalDate.now())

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

            val state = vm.loadedState(LocalDate.now())

            assertTrue(state.rows.single().status is PuzzleStatus.Unplayed)
        }

    @Test
    fun `an archived game is not on the list`() =
        runTest(dispatcher) {
            val repo =
                FakeRepository(games = listOf(wordle, testGame(id = 2).copy(archived = true)))
            val vm = TodayViewModel(repo)

            val state = vm.loadedState(LocalDate.now())

            assertEquals(listOf(1L), state.rows.map { it.game.id })
        }

    @Test
    fun `the page opens on the device's date`() =
        runTest(dispatcher) {
            val vm = TodayViewModel(FakeRepository(games = listOf(wordle)))

            assertEquals(LocalDate.now(), vm.loadedFrame().day)
        }

    @Test
    fun `with no results, the device's date is the earliest date`() =
        runTest(dispatcher) {
            val vm = TodayViewModel(FakeRepository(games = listOf(wordle)))

            assertEquals(LocalDate.now(), vm.loadedFrame().earliestDay)
        }

    // Its puzzle changes at local midnight, so no later puzzle day is possible.
    @Test
    fun `a date after the latest possible puzzle day can't be selected`() =
        runTest(dispatcher) {
            val vm = TodayViewModel(FakeRepository(games = listOf(wordle)))
            backgroundScope.launch { vm.frame.collect {} }
            vm.loadedFrame()

            vm.selectDay(LocalDate.now().plusDays(1))
            runCurrent()

            val frame = vm.frame.value!!
            assertEquals(LocalDate.now(), frame.day)
            assertEquals(LocalDate.now(), frame.latestDay)
        }

    @Test
    fun `the earliest result is the earliest date`() =
        runTest(dispatcher) {
            val earliest = entryToday().copy(puzzleDay = entryToday().puzzleDay.minusDays(2))
            val vm = TodayViewModel(FakeRepository(listOf(wordle), listOf(earliest)))
            backgroundScope.launch { vm.frame.collect {} }
            vm.loadedFrame()

            vm.selectDay(earliest.puzzleDay.minusDays(1))
            runCurrent()

            val frame = vm.frame.value!!
            assertEquals(earliest.puzzleDay, frame.earliestDay)
            assertEquals(earliest.puzzleDay, frame.day)
        }

    @Test
    fun `a past puzzle with no result is missed`() =
        runTest(dispatcher) {
            val earliest = entryToday().copy(puzzleDay = entryToday().puzzleDay.minusDays(2))
            val vm = TodayViewModel(FakeRepository(listOf(wordle), listOf(earliest)))

            val state = vm.loadedState(entryToday().puzzleDay.minusDays(1))

            assertEquals(PuzzleStatus.Missed(currentPuzzleUnplayed = true), state.rows.single().status)
        }

    @Test
    fun `the page for another date has that date's results`() =
        runTest(dispatcher) {
            val earlier = entryToday().copy(puzzleDay = entryToday().puzzleDay.minusDays(2))
            val vm = TodayViewModel(FakeRepository(listOf(wordle), listOf(earlier)))

            val state = vm.loadedState(earlier.puzzleDay)

            assertEquals(earlier.puzzleDay, state.day)
            assertEquals(PuzzleStatus.Done(earlier), state.rows.single().status)
            assertEquals(LocalDate.now(), vm.loadedFrame().day)
        }

    @Test
    fun `a date's flow is kept while it is among the recently used dates`() {
        val vm = TodayViewModel(FakeRepository(games = listOf(wordle)))
        val day = LocalDate.now()
        val flow = vm.stateOn(day)

        repeat(DAY_STATES_KEPT - 1) { vm.stateOn(day.minusDays(it + 1L)) }

        assertSame(flow, vm.stateOn(day))
    }

    @Test
    fun `a date's flow is removed when more than the kept number of other dates are used`() {
        val vm = TodayViewModel(FakeRepository(games = listOf(wordle)))
        val day = LocalDate.now()
        val flow = vm.stateOn(day)

        repeat(DAY_STATES_KEPT) { vm.stateOn(day.minusDays(it + 1L)) }

        assertNotSame(flow, vm.stateOn(day))
    }

    // Null means not loaded yet, so the empty state isn't shown while loading.
    @Test
    fun `the frame is null until the games have been read`() =
        runTest(dispatcher) {
            val vm = TodayViewModel(FakeRepository())
            assertEquals(null, vm.frame.value)
        }
}
