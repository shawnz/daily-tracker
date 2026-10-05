package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class RngdleParserTest {
    // Copied from a finished roll.
    private val rolled =
        """
        RNGdle 🎲 982702

        ⬜ COMMON • Bottom 8%

        ⬜ 🎰 Lucky Seven (Divisible)
        ⬜ 👻 Ghost
        ⬜ 💨 Oxygen (8)
        +8 more

        2,860 EP
        https://rngdle.com
        """.trimIndent()

    // Copied from a finished roll. A badge has its own rarity, which is not the roll's.
    private val rare =
        """
        RNGdle 🎲 276297

        🟦 RARE • Top 11%

        🟦 🎼 Metronome
        ⬜ 🎰 Lucky Seven (Divisible)
        ⬜ 👯‍♀️ Two Pair
        +10 more

        20,817 EP
        https://rngdle.com
        """.trimIndent()

    @Test
    fun `a badge of a different rarity to the roll is still read`() {
        val result = RngdleParser.parse(rare)!!
        assertEquals("RARE", result.rarity)
        assertEquals("Top 11%", result.standing)
        assertEquals(listOf("Metronome", "Lucky Seven (Divisible)", "Two Pair"), result.badges)
        assertEquals(13, result.badgesEarned)
        assertEquals(20817, result.points)
    }

    // 👯‍♀️ is one badge emoji built from several code points joined together.
    @Test
    fun `a badge emoji made of joined code points does not split the label`() {
        assertEquals("Two Pair", RngdleParser.parse(rare)!!.badges.last())
    }

    @Test
    fun `reads the roll, the rarity and where it placed`() {
        val result = RngdleParser.parse(rolled)!!
        assertEquals("982702", result.roll)
        assertEquals("COMMON", result.rarity)
        assertEquals("Bottom 8%", result.standing)
    }

    // Three badges are listed, and the rest are given as a number.
    @Test
    fun `reads the badges listed and how many were left out`() {
        val result = RngdleParser.parse(rolled)!!
        assertEquals(
            listOf("Lucky Seven (Divisible)", "Ghost", "Oxygen (8)"),
            result.badges,
        )
        assertEquals(8, result.moreBadges)
        assertEquals(11, result.badgesEarned)
    }

    @Test
    fun `reads the experience without its separators`() {
        assertEquals(2860, RngdleParser.parse(rolled)!!.points)
    }

    // The separators are Swiss, French and Russian.
    @ParameterizedTest
    @ValueSource(strings = ["20'817", "20\u202F817", "20\u00A0817"])
    fun `reads the experience with the separators of other regions`(points: String) {
        assertEquals(20817, RngdleParser.parse(rare.replace("20,817", points))!!.points)
    }

    @Test
    fun `a roll is neither won nor lost`() {
        assertNull(RngdleParser.parse(rolled)!!.success)
    }

    // A roll between the top half and the bottom tenth doesn't have a standing.
    @Test
    fun `a middling roll has no standing`() {
        val result =
            RngdleParser.parse(
                "RNGdle 🎲 100374\n\n⬜ COMMON\n\n🟩 🕳️ Deep Void\n⬜ 💧 Hydrogen (1)\n" +
                    "⬜ 🔋 Lithium (3)\n+9 more\n\n5,096 EP\nhttps://rngdle.com",
            )!!
        assertEquals("COMMON", result.rarity)
        assertNull(result.standing)
        assertEquals(listOf("Deep Void", "Hydrogen (1)", "Lithium (3)"), result.badges)
        assertEquals(5096, result.points)
    }

    @Test
    fun `an uncommon roll is read`() {
        val result =
            RngdleParser.parse(
                "RNGdle 🎲 100154\n\n🟩 UNCOMMON • Top 26%\n\n🟩 🕳️ Deep Void\n🟩 🪶 Feather\n" +
                    "🟩 🪞 Pocket Mirror\n+10 more\n\n9,528 EP\nhttps://rngdle.com",
            )!!
        assertEquals("UNCOMMON", result.rarity)
        assertEquals("Top 26%", result.standing)
    }

    @Test
    fun `the rarest tiers are read too`() {
        val mythic =
            RngdleParser.parse(
                "RNGdle 🎲 100204\n\n🟥 MYTHIC • Top 1%\n\n🟪 🔊 Crescendo\n🟩 📉 Low Ball\n" +
                    "🟩 🕳️ Deep Void\n+14 more\n\n223,692 EP\nhttps://rngdle.com",
            )!!
        assertEquals("MYTHIC", mythic.rarity)
        assertEquals("Top 1%", mythic.standing)
        val trash =
            RngdleParser.parse(
                "RNGdle 🎲 103463\n\n🟫 TRASH • Bottom 1%\n\n⬜ 👻 Ghost\n⬜ 💧 Hydrogen (1)\n" +
                    "⬜ ✏️ Carbon (6)\n+6 more\n\n1,997 EP\nhttps://rngdle.com",
            )!!
        assertEquals("TRASH", trash.rarity)
        assertEquals("Bottom 1%", trash.standing)
    }

    // The share text has "Top 0%" for a roll in the top 0.5%.
    @Test
    fun `a roll at the very top is read with its red badges`() {
        val result =
            RngdleParser.parse(
                "RNGdle 🎲 7\n\n🟥 MYTHIC • Top 0%\n\n🟥 7️⃣ Seven\n🟥 7️⃣ Power of Seven\n" +
                    "🟥 ☝️ Single Digit\n+13 more\n\n113,596,176 EP\nhttps://rngdle.com",
            )!!
        assertEquals("Top 0%", result.standing)
        assertEquals(listOf("Seven", "Power of Seven", "Single Digit"), result.badges)
        assertEquals(113596176, result.points)
    }

    // The share text has "Bottom 0%" for a roll in the bottom 0.5%.
    @Test
    fun `a roll at the very bottom is read`() {
        val result =
            RngdleParser.parse(
                "RNGdle 🎲 103506\n\n🟫 TRASH • Bottom 0%\n\n⬜ 💧 Hydrogen (1)\n⬜ ✏️ Carbon (6)\n" +
                    "⬜ 🔋 Lithium (3)\n+6 more\n\n1,970 EP\nhttps://rngdle.com",
            )!!
        assertEquals("TRASH", result.rarity)
        assertEquals("Bottom 0%", result.standing)
    }

    @Test
    fun `a roll at the edge of the bottom tenth is read`() {
        val result =
            RngdleParser.parse(
                "RNGdle 🎲 103084\n\n⬜ COMMON • Bottom 10%\n\n⬜ 🏞️ Hills\n⬜ 🐫 Dunes\n" +
                    "⬜ 🦘 Hopscotch\n+9 more\n\n3,015 EP\nhttps://rngdle.com",
            )!!
        assertEquals("COMMON", result.rarity)
        assertEquals("Bottom 10%", result.standing)
    }

    @Test
    fun `an anomaly roll is read`() {
        val result =
            RngdleParser.parse(
                "RNGdle 🎲 100012\n\n🟧 ANOMALY • Top 2%\n\n🟦 🌑 Deep Void (3)\n🟦 🐢 Turtle\n" +
                    "🟩 📉 Low Ball\n+22 more\n\n96,987 EP\nhttps://rngdle.com",
            )!!
        assertEquals("ANOMALY", result.rarity)
        assertEquals("Top 2%", result.standing)
        assertEquals(listOf("Deep Void (3)", "Turtle", "Low Ball"), result.badges)
        assertEquals(96987, result.points)
    }

    @Test
    fun `an orange badge is read`() {
        val result =
            RngdleParser.parse(
                "RNGdle 🎲 100001\n\n🟥 MYTHIC • Top 0%\n\n🟧 🤖 Binary Soul\n" +
                    "🟪 🌌 Deep Void (4)\n🟪 🙃 Strobogrammatic\n+28 more\n\n" +
                    "2,906,970 EP\nhttps://rngdle.com",
            )!!
        assertEquals(listOf("Binary Soul", "Deep Void (4)", "Strobogrammatic"), result.badges)
        assertEquals(31, result.badgesEarned)
    }

    // A roll can have a poem, in quotes above the experience.
    @Test
    fun `a poem is read and is not taken for a badge`() {
        val result =
            RngdleParser.parse(
                "RNGdle 🎲 69557\n\n🟦 RARE • Top 20%\n\n🟩 🤑 High Roller\n🟩 😏 Nice\n" +
                    "🟩 💎 Prime Number\n+13 more\n\n\"without chance, endless void\"\n\n12,637 EP\nhttps://rngdle.com",
            )!!
        assertEquals("without chance, endless void", result.poem)
        assertEquals(listOf("High Roller", "Nice", "Prime Number"), result.badges)
        assertEquals(12637, result.points)
    }

    @Test
    fun `matches only rngdle text`() {
        assertTrue(RngdleParser.matches(rolled))
        assertFalse(RngdleParser.matches("Wordle 1,234 4/6"))
    }
}
