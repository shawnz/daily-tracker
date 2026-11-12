package org.shawnz.dailytracker.ui.share

import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.catalog.CatalogGame

/** A game a shared result can be saved to. */
sealed interface ShareTarget {
    val title: String

    /** The catalog entry for the game. Null for a user-made game. */
    val catalogGame: CatalogGame?

    /** A game in the database, active or archived. */
    data class Tracked(
        val game: Game,
    ) : ShareTarget {
        override val title: String get() = game.title
        override val catalogGame: CatalogGame? get() = game.catalogGame
    }

    /** A catalog game that is not tracked. It is added when a result is saved to it. */
    data class New(
        override val catalogGame: CatalogGame,
    ) : ShareTarget {
        override val title: String get() = catalogGame.title
    }
}
