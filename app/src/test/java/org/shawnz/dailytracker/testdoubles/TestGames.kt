package org.shawnz.dailytracker.testdoubles

import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.GameSource
import org.shawnz.dailytracker.data.catalog.CatalogGame

/**
 * A tracked game based on a real catalog entry, so a test names the game instead of restating
 * its rollover rule.
 */
fun testGame(
    id: Long = 1,
    source: GameSource = CatalogGame.WORDLE,
) = Game(
    id = id,
    source = source,
    sortOrder = 0,
    archived = false,
    remindersEnabled = true,
)
