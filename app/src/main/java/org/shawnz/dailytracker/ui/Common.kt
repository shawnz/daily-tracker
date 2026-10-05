package org.shawnz.dailytracker.ui

import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.browser.CustomTabLauncher
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** The gutter between screen content and the edge of the screen. */
val PageMargin = 24.dp

const val STOP_TIMEOUT_MILLIS = 5_000L

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
private val DAY_WITH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

/** This date in short form, such as "Thu 24 Sep", with the year added if it doesn't match the current year. */
fun LocalDate.dayLabel(): String = format(if (year == LocalDate.now().year) DAY_FORMAT else DAY_WITH_YEAR)

/**
 * The host of [url] without a leading `www.`, such as `example.com`. Null when [url] doesn't have
 * a host.
 */
fun hostLabel(url: String): String? =
    CustomTabLauncher
        .normalize(url)
        .host
        ?.removePrefix("www.")
        ?.takeIf { it.isNotBlank() }
