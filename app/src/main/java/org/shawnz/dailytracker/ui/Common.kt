package org.shawnz.dailytracker.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.browser.CustomTabLauncher
import org.shawnz.dailytracker.share.ReceiveShareActivity
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** The gutter between screen content and the edge of the screen. */
val PageMargin = 24.dp

const val STOP_TIMEOUT_MILLIS = 5_000L

private val DAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM")
private val DAY_WITH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM yyyy")

/** This date in short form, such as "Thu 24 Sep", with the year added if it doesn't match the current year. */
fun LocalDate.dayLabel(): String = format(if (year == LocalDate.now().year) DAY_FORMAT else DAY_WITH_YEAR)

/** Formats [time] for the device's locale and the device's 12-hour or 24-hour clock setting. */
@Composable
fun timeText(time: LocalTime): String {
    val locale = LocalConfiguration.current.locales[0]
    val skeleton = if (DateFormat.is24HourFormat(LocalContext.current)) "Hm" else "hm"
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return time.format(DateTimeFormatter.ofPattern(pattern, locale))
}

/**
 * Sends [text] to another app.
 *
 * Daily Tracker is itself an `ACTION_SEND` target, so it is excluded from the chooser.
 */
fun shareText(
    context: Context,
    text: String,
) {
    val send =
        Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
    val chooser =
        Intent.createChooser(send, null).apply {
            putExtra(
                Intent.EXTRA_EXCLUDE_COMPONENTS,
                arrayOf(ComponentName(context, ReceiveShareActivity::class.java)),
            )
        }
    runCatching { context.startActivity(chooser) }
}

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
