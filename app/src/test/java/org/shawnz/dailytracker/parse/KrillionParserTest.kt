package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class KrillionParserTest {
    // A finished day's share text.
    private val played =
        """
        Krillion #61 🦐
        150

        🐟🐟🐟🫧🐟🫧🫧
        """.trimIndent()

    // One of each tier and a miss: 100 + 85 + 60 + 30 + 15 + 10 + 0 = 300.
    private val everyTier = "Krillion #63 🦐\n300\n\n🌟🏮🦑🐟🤡🫧⬛"

    // The tiers are worth 🫧 10, 🤡 15, 🐟 30, 🦑 60, 🏮 85 and 🌟 100, and a miss is worth
    // nothing. Compared by code point, because these emoji do not fit in one Char.
    private fun pointsFromEmoji(emoji: String): Int {
        val worth =
            mapOf(
                0x1FAE7 to 10,
                0x1F921 to 15,
                0x1F41F to 30,
                0x1F991 to 60,
                0x1F3EE to 85,
                0x1F31F to 100,
            )
        return emoji.codePoints().toArray().sumOf { worth[it] ?: 0 }
    }

    @Test
    fun `reads the number, the score and every prompt`() {
        val result = KrillionParser.parse(played)!!
        assertEquals("61", result.puzzleNumber)
        assertEquals(150, result.score)
        assertEquals("🐟🐟🐟🫧🐟🫧🫧", result.emoji)
        assertEquals(7, result.prompts)
    }

    // Four schoolers and three plankton make 150.
    @Test
    fun `the emoji account for the score`() {
        val result = KrillionParser.parse(played)!!
        assertEquals(result.score, pointsFromEmoji(result.emoji))
    }

    @Test
    fun `every tier is kept`() {
        val result = KrillionParser.parse(everyTier)!!
        assertEquals("🌟🏮🦑🐟🤡🫧⬛", result.emoji)
        assertEquals(7, result.prompts)
        assertEquals(result.score, pointsFromEmoji(result.emoji))
    }

    // Hand-edited onto one line.
    @Test
    fun `a share reflowed onto one line reads the same`() {
        val result = KrillionParser.parse("Krillion #61 🦐 150 🐟🐟🐟🫧🐟🫧🫧")!!
        assertEquals("61", result.puzzleNumber)
        assertEquals(150, result.score)
        assertEquals("🐟🐟🐟🫧🐟🫧🫧", result.emoji)
    }

    @Test
    fun `a trailing link is ignored`() {
        val daily = KrillionParser.parse("$played\n\nhttps://krillion.io")!!
        assertEquals(150, daily.score)
        assertEquals("🐟🐟🐟🫧🐟🫧🫧", daily.emoji)

        val archive =
            KrillionParser.parse(
                "Krillion ⟲ #30 🦐\n210\n\n🐟🐟🐟🐟🐟🐟🐟\n\nhttps://krillion.io/archive/30",
            )!!
        assertTrue(archive.archive)
        assertEquals("🐟🐟🐟🐟🐟🐟🐟", archive.emoji)
    }

    // Day one was 2026-07-16.
    @Test
    fun `a day number is the day it was set`() {
        assertEquals(LocalDate.of(2026, 9, 14), KrillionParser.parse(played)!!.day)
        assertEquals(
            LocalDate.of(2026, 7, 16),
            KrillionParser.parse("Krillion #1 🦐\n70\n\n🫧🫧🫧🫧🫧🫧🫧")!!.day,
        )
    }

    // 155 is the lowest Schooler score a day can reach, because every tier is worth a multiple
    // of five.
    @Test
    fun `the score is given the game's own word for it`() {
        assertEquals("Plankton", KrillionParser.parse(played)!!.band)
        assertEquals(
            "Schooler",
            KrillionParser.parse("Krillion #61 🦐\n155\n\n🦑🐟🐟🤡🫧🫧⬛")!!.band,
        )
        assertEquals("Rare", KrillionParser.parse(everyTier)!!.band)
        assertEquals(
            "One in a Krillion",
            KrillionParser.parse("Krillion #61 🦐\n700\n\n🌟🌟🌟🌟🌟🌟🌟")!!.band,
        )
    }

    @Test
    fun `a missed prompt is counted`() {
        val result = KrillionParser.parse("Krillion #62 🦐\n110\n\n🐟🐟⬛🫧🐟⬛🫧")!!
        assertEquals(2, result.misses)
        assertEquals(7, result.prompts)
    }

    @Test
    fun `a day of misses scores nothing`() {
        val result = KrillionParser.parse("Krillion #62 🦐\n0\n\n⬛⬛⬛⬛⬛⬛⬛")!!
        assertEquals(0, result.score)
        assertEquals(7, result.misses)
        assertEquals(7, result.prompts)
        assertEquals("Plankton", result.band)
    }

    @Test
    fun `there is no win or loss`() {
        assertNull(KrillionParser.parse(played)!!.success)
    }

    // An archive day and a run outside the daily have their own sign before the number.
    @Test
    fun `an archive day is marked and still dated`() {
        val result = KrillionParser.parse("Krillion ⟲ #30 🦐\n210\n\n🐟🐟🐟🐟🐟🐟🐟")!!
        assertTrue(result.archive)
        assertEquals(LocalDate.of(2026, 8, 14), result.day)
    }

    @Test
    fun `an unlimited run is numbered but not dated`() {
        val result = KrillionParser.parse("Krillion ∞ #5 🦐\n300\n\n🦑🦑🦑🦑🦑⬛⬛")!!
        assertTrue(result.unlimited)
        assertEquals("5", result.puzzleNumber)
        assertEquals(300, result.score)
        assertEquals("🦑🦑🦑🦑🦑⬛⬛", result.emoji)
        assertNull(result.day)
    }

    @Test
    fun `a themed pack run is numbered but not dated`() {
        val movies =
            KrillionParser.parse(
                "Krillion 🎬 #5 🦐\n145\n\n🏮🐟🫧🫧🫧⬛⬛\n\nhttps://krillion.io/movies?game=5",
            )!!
        assertTrue(movies.unlimited)
        assertFalse(movies.archive)
        assertEquals("5", movies.puzzleNumber)
        assertEquals(145, movies.score)
        assertEquals("🏮🐟🫧🫧🫧⬛⬛", movies.emoji)
        assertNull(movies.day)

        for (badge in listOf("🏆", "🧭", "✒️")) {
            val result = KrillionParser.parse("Krillion $badge #2 🦐\n70\n\n🫧🫧🫧🫧🫧🫧🫧")!!
            assertTrue(result.unlimited, badge)
            assertEquals(70, result.score, badge)
            assertNull(result.day, badge)
        }
    }

    @Test
    fun `matches only krillion text`() {
        assertTrue(KrillionParser.matches(played))
        assertFalse(KrillionParser.matches("Wordle 1,234 4/6"))
    }
}
