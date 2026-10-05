package org.shawnz.dailytracker.data.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource

/** Each share text is matched by exactly one catalog game's parser, which can parse it. */
class CatalogParsersTest {
    private val parsers = CatalogGame.entries.mapNotNull { it.parser }

    @ParameterizedTest(name = "{0} matches one parser")
    @MethodSource("shareTexts")
    fun `each share text matches exactly one parser`(
        name: String,
        text: String,
    ) {
        val matching = parsers.filter { it.matches(text) }
        assertEquals(1, matching.size, "$name matched ${matching.size} parsers")
    }

    @ParameterizedTest(name = "{0} parses")
    @MethodSource("shareTexts")
    fun `the parser that matches a text can parse it`(
        name: String,
        text: String,
    ) {
        val parser = parsers.single { it.matches(text) }
        assertTrue(parser.parse(text) != null, "$name matched but did not parse")
    }

    companion object {
        @JvmStatic
        fun shareTexts() =
            listOf(
                arguments(
                    "Wordle",
                    "Wordle 1,234 4/6\n\n⬛⬛⬛🟨⬛\n⬛🟨⬛⬛⬛\n⬛🟩🟩⬛🟨\n🟩🟩🟩🟩🟩",
                ),
                arguments(
                    "Connections",
                    "Connections\nPuzzle #1191\n🟦🟦🟦🟦\n🟩🟩🟩🟩\n🟨🟨🟨🟨\n🟪🟪🟪🟪",
                ),
                arguments("Strands", "Strands #925\n“OH MY GOSH!!”\n🔵💡🔵🟡\n🔵🔵🔵🔵"),
                arguments(
                    "4x3",
                    "September 14, 2026\n156 points • No mistakes\n" +
                        "🌟🟦🟦\n🌟🟨🟨\n🌟🟩🟩\n🌟🟪🟪\nhttps://4x3.fun",
                ),
                arguments(
                    "4x6",
                    "4×6 · Mon Sep 14\n🟥🟥🟥🟥\n🟨🟨🟨🟨\n🟧🟧🟧🟧\n🟩🟩🟩🟩\n🟦🟦🟦🟦\n🟪🟪🟪🟪\n" +
                        "14/16 moves 💯 · 276 pts\nhankgreen.com/4x6",
                ),
                arguments(
                    "Smush",
                    "hankgreen.com/smush · Sep 14\n298 pts · ★★ pangram first\n" +
                        "🟩🟨🥞\n🥞⭐🥞\n🥞🥞🟨",
                ),
                arguments(
                    "Kinda Hard Golf",
                    "kindahard.golf #530\n\n📝 46\n\n6.⛳ 3\n5.⛳ 5\n4.⛳ 3\n3.⛳ 17\n2.⛳ 10\n1.⛳ 4\n" +
                        "0.🏌️ 4\n\nhttps://kindahard.golf",
                ),
                arguments("Parseword", "Parseword #240\n⚡️ 50s\n💎 Perfect\n🎭 Secret Found\n🥚 Learn Mode"),
                arguments(
                    "Bandle",
                    "Bandle #1191 3/6\n🟥🟥🟩⬜⬜⬜\nFound: 45/50 (90%)\n" +
                        "Current Streak: 5 (max 12)\n#Bandle \nhttps://bandle.app",
                ),
                arguments(
                    "Bandle, older, with #Wordle",
                    "Bandle #733 1/6\n🟩⬜⬜⬜⬜⬜\nFound: 7/11 (63.6%)\nCurrent Streak: 1 (max 2)\n" +
                        "#Bandle #Heardle #Wordle \n\nhttps://bandle.app/",
                ),
                arguments(
                    "RNGdle",
                    "RNGdle 🎲 982702\n\n⬜ COMMON • Bottom 8%\n\n⬜ 🎰 Lucky Seven (Divisible)\n" +
                        "⬜ 👻 Ghost\n⬜ 💨 Oxygen (8)\n+8 more\n\n2,860 EP\nhttps://rngdle.com",
                ),
                arguments("Krillion", "Krillion #61 🦐\n150\n\n🐟🐟🐟🫧🐟🫧🫧"),
                arguments("Queens", "Queens #867 | 1:00 👑\nlnkd.in/queens."),
                arguments("Zip", "Zip #546 | 0:21 🏁\nlnkd.in/zip."),
                arguments("Catfishing", "catfishing.net\n#813 - 1/10\n🐟🐟🐟🐟🐟\n🐈🐟🐟🐟🐟"),
                arguments(
                    "FoodGuessr",
                    "I got 8,791 on the FoodGuessr Daily!\n\n🌕🌖🌑🌑🌑 1,718 (Round 1)\n" +
                        "🌕🌕🌕🌕🌕 5,000 (Round 2) 💯\n🌕🌕🌘🌑🌑 2,073 (Round 3)\n\n" +
                        "Monday, Sep 14, 2026\nPlay here: https://www.foodguessr.com/",
                ),
                arguments(
                    "Raddle",
                    "COSTA → RICA [💯]\nRaddle #567 • Sep 14, 2026\n\n🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🙌",
                ),
                arguments(
                    "Clues by Sam",
                    "I solved the daily #CluesBySam, Sep 25th 2026 (Hard), in 03:08\n" +
                        "🟩🟩🟩🟩\n🟩🟩🟩🟩\n🟩🟩🟩🟩\n🟩🟩🟩🟩\n🟩🟩🟩🟩\nhttps://cluesbysam.com",
                ),
                arguments(
                    "Clues by Sam heading",
                    "#CluesBySam - Sep 25th 2026 (Hard)\n03:08\n" +
                        "🟩🟩🟩🟩\n🟩🟩🟩🟩\n🟩🟩🟩🟩\n🟩🟩🟩🟩\n🟩🟩🟩🟩",
                ),
            )
    }
}
