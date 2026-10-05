package org.shawnz.dailytracker.ui.stats

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.testdoubles.FakeRepository
import org.shawnz.dailytracker.testdoubles.testGame

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    private val steady = testGame(id = 1)
    private val newcomer = testGame(id = 2)
    private val today = steady.currentPuzzleDay()

    private fun entry(
        id: Long,
        gameId: Long,
        daysAgo: Long,
    ) = EntryEntity(
        id = id,
        gameId = gameId,
        puzzleDay = today.minusDays(daysAgo),
    )

    // Four plays over a ten-day window, the last three running up to today.
    private val steadyEntries =
        listOf(entry(1, 1, 9), entry(2, 1, 2), entry(3, 1, 1), entry(4, 1, 0))

    // One play, today, so the window is a single day and the rate is 1.
    private val newcomerEntries = listOf(entry(5, 2, 0))

    private fun viewModel(repo: FakeRepository) = StatsViewModel(repo)

    private fun bothGames() =
        FakeRepository(
            games = listOf(steady, newcomer),
            entries = steadyEntries + newcomerEntries,
        )

    // The rates are 0.4 and 1, so an average would give 0.7. Pooling gives 5 days out of 11.
    @Test
    fun `overall completion pools the days instead of averaging the rates`() =
        runTest(dispatcher) {
            val vm = viewModel(bothGames())

            val state = vm.state.filterNotNull().first()

            assertEquals(5f / 11f, state.overallCompletion, 0.0001f)
        }

    @Test
    fun `the best streak is the longest any one game reaches`() =
        runTest(dispatcher) {
            val vm = viewModel(bothGames())

            val state = vm.state.filterNotNull().first()

            assertEquals(3, state.perGame.single { it.game.id == 1L }.streak)
            assertEquals(1, state.perGame.single { it.game.id == 2L }.streak)
            assertEquals(3, state.bestStreak)
        }

    @Test
    fun `entries are split by the game they belong to`() =
        runTest(dispatcher) {
            val vm = viewModel(bothGames())

            val state = vm.state.filterNotNull().first()

            assertEquals(steadyEntries, state.perGame.single { it.game.id == 1L }.entries)
            assertEquals(newcomerEntries, state.perGame.single { it.game.id == 2L }.entries)
        }

    @Test
    fun `an archived game is left out of the totals`() =
        runTest(dispatcher) {
            val archived = testGame(id = 3).copy(archived = true)
            val repo =
                FakeRepository(
                    games = listOf(steady, archived),
                    entries = steadyEntries + (1L..5L).map { entry(10 + it, 3, it) },
                )
            val vm = viewModel(repo)

            val state = vm.state.filterNotNull().first()

            assertEquals(listOf(1L), state.perGame.map { it.game.id })
            assertEquals(4, state.totalPlays)
        }

    @Test
    fun `tracking no games gives zeroes rather than no state`() =
        runTest(dispatcher) {
            val vm = viewModel(FakeRepository())

            val state = vm.state.filterNotNull().first()

            assertTrue(state.perGame.isEmpty())
            assertEquals(0, state.totalPlays)
            assertEquals(0f, state.overallCompletion)
            assertEquals(0, state.bestStreak)
        }

    @Test
    fun `there is no state before the read finishes`() {
        val vm = viewModel(FakeRepository(games = listOf(steady)))

        assertNull(vm.state.value)
    }
}
