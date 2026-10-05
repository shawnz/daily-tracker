package org.shawnz.dailytracker.ui.games

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.parse.WordleResult

// Wordle's board is the whole result, so it is drawn larger than the shared default.
private val TILE_SIZE = 30.dp
private val TILE_GAP = 4.dp

/** Wordle's own two colours, matching the logo drawn at the top of the detail page. */
private val CORRECT = Color(0xFF6AAA64)
private val PRESENT = Color(0xFFD1B036)

object WordleRenderer : GameRenderer<WordleResult> {
    @Composable
    override fun Summary(
        result: WordleResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = result.guesses?.let { "$it/6" } ?: "X/6",
                style = MaterialTheme.typography.bodySmall,
                color = gradeColor(result.guesses),
            )
            if (result.hardMode) HardChip()
        }
    }

    /**
     * The grid, with the score and puzzle number beside it.
     *
     * The grid is only five squares wide, so there is room beside it. Both parts start at the
     * left edge, like the rest of the card.
     */
    @Composable
    override fun Detail(
        result: WordleResult,
        modifier: Modifier,
    ) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.Top,
        ) {
            if (result.grid.isNotEmpty()) {
                val absent = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                TileGrid(result.grid, size = TILE_SIZE, gap = TILE_GAP) {
                    squareColor(it, absent)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text =
                        result.guesses?.let { "$it / 6" }
                            ?: stringResource(R.string.wordle_not_solved),
                    style = MaterialTheme.typography.titleMedium,
                    color = gradeColor(result.guesses),
                )
                if (result.hardMode) HardChip()
                result.puzzleNumber?.let {
                    Text(
                        "#$it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }

    /**
     * Colour shows only whether a letter was correct, present, or absent.
     *
     * The ordinary and high contrast palettes are a player setting, so both map to the same
     * colours here.
     */
    private fun squareColor(
        codePoint: Int,
        absent: Color,
    ): Color =
        when (codePoint) {
            0x1F7E9, 0x1F7E6 -> CORRECT

            // green, and blue in high contrast
            0x1F7E8, 0x1F7E7 -> PRESENT

            // yellow, and orange in high contrast
            else -> absent // white or black, depending on the player's theme
        }

    @Composable
    override fun Aggregate(
        results: List<WordleResult>,
        modifier: Modifier,
    ) {
        val solved = results.mapNotNull { it.guesses }
        if (solved.isEmpty()) return
        val average = solved.average()
        val solveRate = solved.size * 100 / results.size
        Column(modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.wordle_average, "%.2f".format(average)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                stringResource(R.string.wordle_solve_rate, solveRate),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    @Composable
    private fun HardChip(modifier: Modifier = Modifier) {
        Surface(
            modifier = modifier,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            shape = RoundedCornerShape(4.dp),
        ) {
            Text(
                "HARD",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
            )
        }
    }

    @Composable
    private fun gradeColor(guesses: Int?): Color =
        when (guesses) {
            null -> MaterialTheme.colorScheme.error
            1, 2 -> MaterialTheme.colorScheme.primary
            3, 4 -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
}
