package org.shawnz.dailytracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields

/**
 * Selects a day, one month at a time, with [playedDays] highlighted. Days after [last] are shown
 * but can't be selected.
 */
@Composable
fun DayPickerDialog(
    selected: LocalDate,
    last: LocalDate,
    playedDays: Set<LocalDate>,
    onPick: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onPick(last) },
                enabled = selected != last,
            ) { Text(stringResource(R.string.detail_latest)) }
        },
        text = { DayPickerContent(selected, last, playedDays, onPick) },
    )
}

/** The contents of [DayPickerDialog], for showing inside another container. */
@Composable
fun DayPickerContent(
    selected: LocalDate,
    last: LocalDate,
    playedDays: Set<LocalDate>,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var month by remember(selected) { mutableStateOf(YearMonth.from(selected)) }
    val lastMonth = YearMonth.from(last)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(
                    painterResource(R.drawable.ic_chevron_left),
                    stringResource(R.string.detail_month_previous),
                )
            }
            Text(
                month.format(MONTH_FORMAT),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = { month = month.plusMonths(1) },
                enabled = month < lastMonth,
            ) {
                Icon(
                    painterResource(R.drawable.ic_chevron_right),
                    stringResource(R.string.detail_month_next),
                )
            }
        }
        MonthGrid(
            month = month,
            selected = selected,
            last = last,
            playedDays = playedDays,
            onPick = onPick,
        )
    }
}

private val MONTH_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")

@Composable
private fun MonthGrid(
    month: YearMonth,
    selected: LocalDate,
    last: LocalDate,
    playedDays: Set<LocalDate>,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalLocale.current.platformLocale
    val weekStart = WeekFields.of(locale).firstDayOfWeek
    val weekDays = (0..6).map { weekStart.plus(it.toLong()) }

    // Blank cells before the first of the month, to align each date with its weekday.
    val lead = (month.atDay(1).dayOfWeek.value - weekStart.value + 7) % 7
    val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row {
            weekDays.forEach { day ->
                Text(
                    day.getDisplayName(TextStyle.NARROW, locale),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row {
                week.forEach { day ->
                    if (day == null) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        DayCell(
                            day = day,
                            selected = day == selected,
                            enabled = !day.isAfter(last),
                            played = day in playedDays,
                            onPick = onPick,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                repeat(7 - week.size) {
                    Box(Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: LocalDate,
    selected: Boolean,
    enabled: Boolean,
    played: Boolean,
    onPick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val content =
        when {
            selected -> MaterialTheme.colorScheme.onPrimary
            enabled -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
        }

    Box(
        modifier =
            modifier
                .aspectRatio(1f)
                .padding(2.dp)
                .background(
                    if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    CircleShape,
                ).then(
                    if (played && !selected) {
                        Modifier.border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    },
                ).clickable(enabled = enabled) { onPick(day) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (played) FontWeight.Bold else FontWeight.Normal,
            color = content,
        )
    }
}
