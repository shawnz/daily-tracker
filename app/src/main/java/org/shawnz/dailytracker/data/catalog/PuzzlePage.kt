package org.shawnz.dailytracker.data.catalog

import java.time.LocalDate

/** How the address of one day's puzzle is formed. */
sealed interface PuzzlePage {
    /** The address is formed from the date. */
    class ByDate(
        val url: (LocalDate) -> String,
    ) : PuzzlePage

    /** The address is formed from the day's puzzle number, given by [CatalogGame.numbering]. */
    class ByNumber(
        val url: (Int) -> String,
    ) : PuzzlePage
}
