package org.shawnz.dailytracker.ui.games

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.EntryEntity
import org.shawnz.dailytracker.data.Game
import org.shawnz.dailytracker.data.catalog.CatalogGame
import org.shawnz.dailytracker.parse.BandleParser
import org.shawnz.dailytracker.parse.CatfishingParser
import org.shawnz.dailytracker.parse.CluesBySamParser
import org.shawnz.dailytracker.parse.ConnectionsParser
import org.shawnz.dailytracker.parse.CrossclimbParser
import org.shawnz.dailytracker.parse.FoodGuessrParser
import org.shawnz.dailytracker.parse.FourBySixParser
import org.shawnz.dailytracker.parse.FourByThreeParser
import org.shawnz.dailytracker.parse.GameResult
import org.shawnz.dailytracker.parse.KindaHardGolfParser
import org.shawnz.dailytracker.parse.KrillionParser
import org.shawnz.dailytracker.parse.MiniSudokuParser
import org.shawnz.dailytracker.parse.ParsewordParser
import org.shawnz.dailytracker.parse.PatchesParser
import org.shawnz.dailytracker.parse.PinpointParser
import org.shawnz.dailytracker.parse.QueensParser
import org.shawnz.dailytracker.parse.RaddleParser
import org.shawnz.dailytracker.parse.ResultParser
import org.shawnz.dailytracker.parse.RngdleParser
import org.shawnz.dailytracker.parse.SmushParser
import org.shawnz.dailytracker.parse.StrandsParser
import org.shawnz.dailytracker.parse.TangoParser
import org.shawnz.dailytracker.parse.WendParser
import org.shawnz.dailytracker.parse.WordleParser
import org.shawnz.dailytracker.parse.ZipParser

/** The parser and the renderer for one game. */
class GameModule<R : GameResult>(
    private val parser: ResultParser<R>,
    private val renderer: GameRenderer<R>,
) {
    /** Null when the entry has no text, or its text doesn't parse. */
    fun summaryText(
        entry: EntryEntity,
        resources: Resources,
    ): String? = entry.rawShareText?.let(parser::parse)?.let { renderer.summaryText(it, resources) }

    @Composable
    fun Summary(
        entry: EntryEntity,
        modifier: Modifier = Modifier,
    ) {
        val result = rememberParsed(entry)
        if (result == null) {
            DefaultEntryRenderer.Summary(modifier)
        } else {
            renderer.Summary(result, modifier)
        }
    }

    @Composable
    fun Detail(
        entry: EntryEntity,
        modifier: Modifier = Modifier,
    ) {
        val result = rememberParsed(entry)
        if (result == null) {
            DefaultEntryRenderer.Detail(entry, modifier)
        } else {
            renderer.Detail(result, modifier)
        }
    }

    @Composable
    fun Aggregate(
        entries: List<EntryEntity>,
        modifier: Modifier = Modifier,
    ) {
        val results =
            remember(entries) {
                entries.mapNotNull { entry -> entry.rawShareText?.let(parser::parse) }
            }
        if (results.isNotEmpty()) renderer.Aggregate(results, modifier)
    }

    @Composable
    private fun rememberParsed(entry: EntryEntity): R? = remember(entry.rawShareText) { entry.rawShareText?.let(parser::parse) }

    companion object {
        /** The module for [game], or null for a user-made game. */
        fun forGame(game: Game?): GameModule<*>? =
            when (game?.catalogGame ?: return null) {
                CatalogGame.WORDLE -> GameModule(WordleParser, WordleRenderer)
                CatalogGame.CONNECTIONS -> GameModule(ConnectionsParser, ConnectionsRenderer)
                CatalogGame.STRANDS -> GameModule(StrandsParser, StrandsRenderer)
                CatalogGame.KRILLION -> GameModule(KrillionParser, KrillionRenderer)
                CatalogGame.CATFISHING -> GameModule(CatfishingParser, CatfishingRenderer)
                CatalogGame.FOODGUESSR -> GameModule(FoodGuessrParser, FoodGuessrRenderer)
                CatalogGame.BANDLE -> GameModule(BandleParser, BandleRenderer)
                CatalogGame.CLUES_BY_SAM -> GameModule(CluesBySamParser, CluesBySamRenderer)
                CatalogGame.FOUR_BY_THREE -> GameModule(FourByThreeParser, FourByThreeRenderer)
                CatalogGame.FOUR_BY_SIX -> GameModule(FourBySixParser, FourBySixRenderer)
                CatalogGame.SMUSH -> GameModule(SmushParser, SmushRenderer)
                CatalogGame.RADDLE -> GameModule(RaddleParser, RaddleRenderer)
                CatalogGame.RNGDLE -> GameModule(RngdleParser, RngdleRenderer)
                CatalogGame.KINDA_HARD_GOLF -> GameModule(KindaHardGolfParser, KindaHardGolfRenderer)
                CatalogGame.PARSEWORD -> GameModule(ParsewordParser, ParsewordRenderer)
                CatalogGame.QUEENS -> GameModule(QueensParser, LinkedInRenderer)
                CatalogGame.TANGO -> GameModule(TangoParser, LinkedInRenderer)
                CatalogGame.ZIP -> GameModule(ZipParser, LinkedInRenderer)
                CatalogGame.PINPOINT -> GameModule(PinpointParser, PinpointRenderer)
                CatalogGame.CROSSCLIMB -> GameModule(CrossclimbParser, LinkedInRenderer)
                CatalogGame.MINI_SUDOKU -> GameModule(MiniSudokuParser, LinkedInRenderer)
                CatalogGame.WEND -> GameModule(WendParser, LinkedInRenderer)
                CatalogGame.PATCHES -> GameModule(PatchesParser, LinkedInRenderer)
            }
    }
}

/**
 * The words of [EntrySummary], for text that is shared with another app.
 *
 * "Done" is returned for a game with no parser, for an entry that doesn't parse, and for a
 * result with nothing in its summary.
 */
fun entrySummaryText(
    game: Game,
    entry: EntryEntity,
    resources: Resources,
): String =
    GameModule
        .forGame(game)
        ?.summaryText(entry, resources)
        .orEmpty()
        .ifBlank { resources.getString(R.string.result_done) }

@Composable
fun EntrySummary(
    game: Game?,
    entry: EntryEntity,
    modifier: Modifier = Modifier,
) {
    val module = GameModule.forGame(game)
    if (module != null) {
        module.Summary(entry, modifier)
    } else {
        DefaultEntryRenderer.Summary(modifier)
    }
}

@Composable
fun EntryDetail(
    game: Game?,
    entry: EntryEntity,
    modifier: Modifier = Modifier,
) {
    val module = GameModule.forGame(game)
    if (module != null) {
        module.Detail(entry, modifier)
    } else {
        DefaultEntryRenderer.Detail(entry, modifier)
    }
}

/** Draws the aggregate for [game]'s parsed [entries], or nothing when none of them parse. */
@Composable
fun EntryAggregate(
    game: Game?,
    entries: List<EntryEntity>,
    modifier: Modifier = Modifier,
) {
    val module = GameModule.forGame(game) ?: return
    module.Aggregate(entries, modifier)
}
