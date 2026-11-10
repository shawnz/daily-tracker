package org.shawnz.dailytracker.ui

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filterNotNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** The dates from [first] to [last], one on each page of a pager. */
internal class DayPages(
    private val first: LocalDate,
    val last: LocalDate,
) {
    val count: Int = ChronoUnit.DAYS.between(first, last).toInt() + 1

    fun dayOf(page: Int): LocalDate = first.plusDays(page.toLong())

    /** The page of [day], or the nearest page when [day] is outside the range. */
    fun pageOf(day: LocalDate): Int =
        ChronoUnit.DAYS
            .between(first, day)
            .coerceIn(0L, count - 1L)
            .toInt()

    /** The pager key of [page]. The date is read back from it with [dayAt]. */
    fun key(page: Int): Any = dayOf(page).toEpochDay()
}

/**
 * The date of [page], read from the key made by [DayPages.key]. Null when [page] isn't laid out.
 *
 * After the range of a [DayPages] changes, an index refers to another date until the next layout.
 * A key always matches the date on its page.
 */
internal fun PagerState.dayAt(page: Int): LocalDate? =
    layoutInfo.visiblePagesInfo
        .firstOrNull { it.index == page }
        ?.let { LocalDate.ofEpochDay(it.key as Long) }

/**
 * The date of the current page of [pagerState], which changes halfway through a scroll. Null until
 * the page is laid out.
 */
@Composable
internal fun rememberCurrentDay(pagerState: PagerState): State<LocalDate?> =
    remember(pagerState) { derivedStateOf { pagerState.dayAt(pagerState.currentPage) } }

/**
 * Scrolls by [pages] from the target page, with an animation. The target page includes the scroll
 * in progress, so the pages of calls made during a scroll add up.
 */
internal suspend fun PagerState.animateScrollByPages(pages: Int) = animateScrollToPage(targetPage + pages)

/**
 * Keeps the settled page of [pagerState] on [selected]. The date of a page that settles is passed
 * to [onSettle]. When [selected] changes to another date while the pager is still, the pager is
 * scrolled to the page of that date.
 */
@Composable
internal fun DayPagerSelection(
    pagerState: PagerState,
    pages: DayPages,
    selected: LocalDate,
    onSettle: (LocalDate) -> Unit,
) {
    val currentOnSettle by rememberUpdatedState(onSettle)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.dayAt(pagerState.settledPage) }
            .filterNotNull()
            .collect { currentOnSettle(it) }
    }
    LaunchedEffect(selected) {
        val shown = pagerState.dayAt(pagerState.settledPage)
        if (!pagerState.isScrollInProgress && shown != null && shown != selected) {
            pagerState.animateScrollToPage(pages.pageOf(selected))
        }
    }
}
