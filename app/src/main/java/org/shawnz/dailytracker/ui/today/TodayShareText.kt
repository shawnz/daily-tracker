package org.shawnz.dailytracker.ui.today

import android.content.res.Resources
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.ui.games.entrySummaryText
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val HEADING_DAY: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")

/**
 * The text that is shared from the Today screen.
 *
 * A heading with the app's name and [day], then a line for each game in [rows] that is done,
 * with the game's title and its one-line result.
 */
internal fun todayShareText(
    rows: List<GameRowState>,
    day: LocalDate,
    resources: Resources,
): String {
    val heading =
        resources.getString(
            R.string.today_share_heading,
            resources.getString(R.string.app_name),
            day.format(HEADING_DAY),
        )
    val lines =
        rows.mapNotNull { row ->
            row.entry?.let { entry ->
                resources.getString(
                    R.string.today_share_line,
                    row.game.title,
                    entrySummaryText(row.game, entry, resources),
                )
            }
        }
    return (listOf(heading, "") + lines).joinToString("\n")
}
