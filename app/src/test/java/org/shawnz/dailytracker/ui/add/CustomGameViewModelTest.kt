package org.shawnz.dailytracker.ui.add

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
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.testdoubles.FakeRepository
import org.shawnz.dailytracker.testdoubles.testGame

@OptIn(ExperimentalCoroutinesApi::class)
class CustomGameViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    // Archived games are included, so a link already tracked is recognized rather than added
    // a second time.
    @Test
    fun `the games it matches against include archived ones`() =
        runTest(dispatcher) {
            val repo =
                FakeRepository(
                    games = listOf(testGame(id = 1), testGame(id = 2).copy(archived = true)),
                )
            val vm = CustomGameViewModel(repo)

            val games = vm.games.first { it != null }!!

            assertEquals(listOf(1L, 2L), games.map { it.id })
        }

    @Test
    fun `adding a game of the user's own closes the screen`() =
        runTest(dispatcher) {
            val vm = CustomGameViewModel(FakeRepository())
            assertFalse(vm.added.value)

            vm.addCustom("Hued", "https://hued.example").join()

            assertTrue(vm.added.value)
        }

    // The link is matched against these games, so the restored game is still listed as archived.
    @Test
    fun `restoring leaves the games as they were`() =
        runTest(dispatcher) {
            val archived = testGame().copy(archived = true)
            val vm = CustomGameViewModel(FakeRepository(games = listOf(archived)))
            backgroundScope.launch { vm.games.collect {} }
            vm.games.first { it != null }

            vm.restore(archived).join()
            runCurrent()

            assertEquals(listOf(archived), vm.games.value)
        }

    @Test
    fun `adding a catalog game closes the screen`() =
        runTest(dispatcher) {
            val vm = CustomGameViewModel(FakeRepository())

            vm.addFromCatalog(CatalogGame.WORDLE).join()

            assertTrue(vm.added.value)
        }

    // A link belonging to an archived game puts that game back rather than making another.
    @Test
    fun `restoring an archived game closes the screen`() =
        runTest(dispatcher) {
            val archived = testGame().copy(archived = true)
            val repo = FakeRepository(games = listOf(archived))
            val vm = CustomGameViewModel(repo)

            vm.restore(archived).join()

            assertFalse(
                repo.games.value
                    .single()
                    .archived,
            )
            assertTrue(vm.added.value)
        }
}
