package org.shawnz.dailytracker.ui

import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.shawnz.dailytracker.ui.add.Add
import org.shawnz.dailytracker.ui.add.AddCustom
import org.shawnz.dailytracker.ui.detail.GameDetail
import org.shawnz.dailytracker.ui.settings.Settings
import org.shawnz.dailytracker.ui.stats.Stats
import org.shawnz.dailytracker.ui.today.Today

/**
 * Each route is serialized by reflection when the back stack is saved, so one missing
 * `@Serializable` is a crash on restore.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NavBackStackTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `every route survives process death`() {
        val restoration = StateRestorationTester(compose)
        lateinit var backStack: NavBackStack<NavKey>
        restoration.setContent {
            backStack = rememberNavBackStack(Today)
        }
        val routes = listOf(Add, AddCustom, Stats, Settings, GameDetail(gameId = 7))
        compose.runOnIdle { backStack.addAll(routes) }

        restoration.emulateSavedInstanceStateRestore()

        compose.runOnIdle { assertEquals(listOf(Today) + routes, backStack.toList()) }
    }
}
