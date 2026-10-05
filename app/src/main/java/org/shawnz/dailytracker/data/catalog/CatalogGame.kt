package org.shawnz.dailytracker.data.catalog

import androidx.annotation.DrawableRes
import org.shawnz.dailytracker.R
import org.shawnz.dailytracker.data.GameEntity
import org.shawnz.dailytracker.data.GameSource
import org.shawnz.dailytracker.data.RolloverType
import org.shawnz.dailytracker.parse.BandleParser
import org.shawnz.dailytracker.parse.CatfishingParser
import org.shawnz.dailytracker.parse.CluesBySamParser
import org.shawnz.dailytracker.parse.ConnectionsParser
import org.shawnz.dailytracker.parse.CrossclimbParser
import org.shawnz.dailytracker.parse.FoodGuessrParser
import org.shawnz.dailytracker.parse.FourBySixParser
import org.shawnz.dailytracker.parse.FourByThreeParser
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

/**
 * The app's built-in games. A user can also add their own.
 *
 * @property otherUrls Alternate URLs for the game. `*` matches any characters within one path
 *   segment.
 * @property hidden Whether the game is left out of the list of games to add.
 * @property parser Null when no parser exists for this game.
 */
enum class CatalogGame(
    override val title: String,
    val description: String,
    override val url: String,
    val otherUrls: List<String> = emptyList(),
    val publisher: String? = null,
    @param:DrawableRes val logo: Int? = null,
    val hidden: Boolean = false,
    val parser: ResultParser<*>? = null,
    val rolloverType: RolloverType = RolloverType.UNSCHEDULED,
    val rolloverZoneId: String? = null,
    val rolloverMinuteOfDay: Int = 0,
) : GameSource {
    WORDLE(
        title = "Wordle",
        description = "Guess the hidden word in 6 tries.",
        url = "https://www.nytimes.com/games/wordle/index.html",
        otherUrls = listOf("https://www.nytimes.com/games/wordle"),
        publisher = "The New York Times",
        logo = R.drawable.logo_wordle,
        parser = WordleParser,
        rolloverType = RolloverType.LOCAL_MIDNIGHT,
    ),

    CONNECTIONS(
        title = "Connections",
        description = "Group words that share a common thread.",
        url = "https://www.nytimes.com/games/connections",
        publisher = "The New York Times",
        logo = R.drawable.logo_connections,
        parser = ConnectionsParser,
        rolloverType = RolloverType.LOCAL_MIDNIGHT,
    ),

    STRANDS(
        title = "Strands",
        description = "Find hidden words and uncover the day's theme.",
        url = "https://www.nytimes.com/games/strands",
        publisher = "The New York Times",
        logo = R.drawable.logo_strands,
        parser = StrandsParser,
        rolloverType = RolloverType.LOCAL_MIDNIGHT,
    ),

    CLUES_BY_SAM(
        title = "Clues by Sam",
        description = "Figure out who is criminal and who is innocent.",
        url = "https://cluesbysam.com/",
        publisher = "Ad Artis Oy",
        parser = CluesBySamParser,
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "America/New_York",
    ),

    KINDA_HARD_GOLF(
        title = "Kinda Hard Golf",
        description = "A challenging daily golf game.",
        url = "https://kindahardgolf.com/",
        otherUrls = listOf("https://kindahard.golf"),
        publisher = "Mighty Pebble Games",
        parser = KindaHardGolfParser,
        // The game resets at 04:00 UTC, by a fixed offset that ignores daylight saving.
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "UTC-04:00",
    ),

    FOUR_BY_THREE(
        title = "4×3",
        description = "Four categories. Three words each. One word is in all four.",
        url = "https://www.hankgreen.com/fourbythree/",
        otherUrls = listOf("https://4x3.fun"),
        publisher = "Hank Green",
        logo = R.drawable.logo_hankgreen,
        parser = FourByThreeParser,
        rolloverType = RolloverType.LOCAL_MIDNIGHT,
    ),

    SMUSH(
        title = "Smush",
        description =
            "Each letter gets 5 uses. Except the gold one, which is always " +
                "required.",
        // Credited to John Green on the page, although it is on hankgreen.com.
        url = "https://www.hankgreen.com/smush/",
        publisher = "John Green",
        logo = R.drawable.logo_hankgreen,
        parser = SmushParser,
        rolloverType = RolloverType.LOCAL_MIDNIGHT,
    ),

    FOUR_BY_SIX(
        title = "4×6",
        description = "Like colors with Like. Pull from the right. Unscramble the board.",
        url = "https://www.hankgreen.com/4x6/",
        publisher = "Hank Green",
        parser = FourBySixParser,
        // Reached through a streak-gated invite in 4×3.
        hidden = true,
        rolloverType = RolloverType.LOCAL_MIDNIGHT,
    ),

    BANDLE(
        title = "Bandle",
        description = "Guess the song, one instrument at a time.",
        url = "https://bandle.app/",
        parser = BandleParser,
        rolloverType = RolloverType.LOCAL_MIDNIGHT,
    ),

    FOODGUESSR(
        title = "FoodGuessr",
        description = "Explore the world by food!",
        url = "https://www.foodguessr.com/",
        // FoodGuessr has other dailies under /game, so only this daily's path is listed.
        otherUrls = listOf("https://www.foodguessr.com/game/daily"),
        parser = FoodGuessrParser,
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "UTC",
    ),

    RADDLE(
        title = "Raddle",
        description = "Word transformation game.",
        url = "https://raddle.quest/",
        // Every puzzle has its own page, and the share text contains that link.
        otherUrls = listOf("https://raddle.quest/*"),
        publisher = "The Mystery League",
        logo = R.drawable.logo_raddle,
        parser = RaddleParser,
        // The newest puzzle comes from a published manifest, not from the clock. A puzzle is
        // published the evening before its date, Chicago time.
        rolloverType = RolloverType.UNSCHEDULED,
    ),

    CATFISHING(
        title = "Catfishing",
        description =
            "Guess the Wikipedia article from its categories. 10 interesting " +
                "people, places, and things to guess every day.",
        url = "https://catfishing.net/",
        parser = CatfishingParser,
        rolloverType = RolloverType.LOCAL_MIDNIGHT,
    ),

    KRILLION(
        title = "Krillion",
        description =
            "Seven prompts a day. Rare answers sink you deeper. How far down " +
                "can you go?",
        url = "https://krillion.io/",
        parser = KrillionParser,
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "America/New_York",
    ),

    RNGDLE(
        title = "RNGdle",
        description =
            "A daily random number game. Roll your number, collect badges, " +
                "and compete with friends.",
        url = "https://www.rngdle.com/",
        parser = RngdleParser,
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "UTC",
    ),

    PARSEWORD(
        title = "Parseword",
        description = "A tricky wordplay game.",
        url = "https://www.parseword.com/",
        publisher = "Josh Wardle",
        parser = ParsewordParser,
        // The game resets at 08:00 UTC, by a fixed offset that ignores daylight saving.
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "UTC-08:00",
    ),

    // Every LinkedIn game rolls over at midnight Pacific.
    QUEENS(
        title = "Queens",
        description =
            "Use your logic skills to place crowns in just the right regions, " +
                "rows, and columns.",
        url = "https://www.linkedin.com/games/queens/",
        otherUrls = listOf("https://lnkd.in/queens"),
        publisher = "LinkedIn",
        logo = R.drawable.logo_queens,
        parser = QueensParser,
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "America/Los_Angeles",
    ),

    TANGO(
        title = "Tango",
        description =
            "Use your reasoning skills to harmonize the grid with a symbol in " +
                "every cell.",
        url = "https://www.linkedin.com/games/tango/",
        otherUrls = listOf("https://lnkd.in/tango"),
        publisher = "LinkedIn",
        logo = R.drawable.logo_tango,
        parser = TangoParser,
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "America/Los_Angeles",
    ),

    ZIP(
        title = "Zip",
        description =
            "Plot a path through the grid while passing through each number in " +
                "order.",
        url = "https://www.linkedin.com/games/zip/",
        otherUrls = listOf("https://lnkd.in/zip"),
        publisher = "LinkedIn",
        logo = R.drawable.logo_zip,
        parser = ZipParser,
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "America/Los_Angeles",
    ),

    PINPOINT(
        title = "Pinpoint",
        description =
            "Uncover the hidden connection in this thought-provoking word " +
                "pattern game.",
        url = "https://www.linkedin.com/games/pinpoint/",
        otherUrls = listOf("https://lnkd.in/pinpoint"),
        publisher = "LinkedIn",
        logo = R.drawable.logo_pinpoint,
        parser = PinpointParser,
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "America/Los_Angeles",
    ),

    CROSSCLIMB(
        title = "Crossclimb",
        description =
            "Use your word smarts to unlock a trivia ladder and complete the " +
                "hidden clues.",
        url = "https://www.linkedin.com/games/crossclimb/",
        otherUrls = listOf("https://lnkd.in/crossclimb"),
        publisher = "LinkedIn",
        logo = R.drawable.logo_crossclimb,
        parser = CrossclimbParser,
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "America/Los_Angeles",
    ),

    MINI_SUDOKU(
        title = "Mini Sudoku",
        description = "It\u2019s the classic game, made mini.",
        url = "https://www.linkedin.com/games/mini-sudoku/",
        otherUrls = listOf("https://lnkd.in/minisudoku"),
        publisher = "LinkedIn",
        logo = R.drawable.logo_mini_sudoku,
        parser = MiniSudokuParser,
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "America/Los_Angeles",
    ),

    WEND(
        title = "Wend",
        description =
            "Each day, a new puzzle invites you to connect letters in a grid " +
                "and uncover hidden words.",
        url = "https://www.linkedin.com/games/wend/",
        otherUrls = listOf("https://lnkd.in/wend"),
        publisher = "LinkedIn",
        logo = R.drawable.logo_wend,
        parser = WendParser,
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "America/Los_Angeles",
    ),

    PATCHES(
        title = "Patches",
        description =
            "Patches is a spatial logic puzzle where you complete every shape " +
                "in the grid based on a set of clues.",
        url = "https://www.linkedin.com/games/patches/",
        otherUrls = listOf("https://lnkd.in/patches"),
        publisher = "LinkedIn",
        logo = R.drawable.logo_patches,
        parser = PatchesParser,
        rolloverType = RolloverType.FIXED_ZONE,
        rolloverZoneId = "America/Los_Angeles",
    ),

    ;

    /** Every address for this game, [url] first. */
    val urls: List<String> get() = listOf(url) + otherUrls

    fun toEntity(sortOrder: Int) = GameEntity(catalogGame = this, sortOrder = sortOrder)
}
