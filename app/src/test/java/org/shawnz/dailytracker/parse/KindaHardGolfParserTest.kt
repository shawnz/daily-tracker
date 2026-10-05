package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KindaHardGolfParserTest {
    // Copied from a finished round.
    private val round =
        """
        kindahard.golf #530

        📝 46

        6.⛳ 3
        5.⛳ 5
        4.⛳ 3
        3.⛳ 17
        2.⛳ 10
        1.⛳ 4
        0.🏌️ 4

        https://kindahard.golf
        """.trimIndent()

    @Test
    fun `reads the map number and the total`() {
        val result = KindaHardGolfParser.parse(round)!!
        assertEquals("530", result.puzzleNumber)
        assertEquals(46, result.strokes)
    }

    @Test
    fun `reads every hole in the order printed`() {
        assertEquals(listOf(3, 5, 3, 17, 10, 4, 4), KindaHardGolfParser.parse(round)!!.holes)
    }

    @Test
    fun `the total is the strokes of every hole`() {
        val result = KindaHardGolfParser.parse(round)!!
        assertEquals(result.strokes, result.holes.sum())
    }

    @Test
    fun `a skipped checkpoint is 0 strokes`() {
        val result =
            KindaHardGolfParser.parse(
                "kindahard.golf #13\n\n📝 30\n\n1.⛳ -\n0.🏌️ 30\n\nhttps://kindahard.golf",
            )!!
        assertEquals(listOf(0, 30), result.holes)
    }

    @Test
    fun `a skipped checkpoint keeps the later holes in place`() {
        val result =
            KindaHardGolfParser.parse(
                "kindahard.golf #14\n\n📝 25\n\n3.⛳ 4\n2.⛳ -\n1.⛳ 6\n0.🏌️ 15\n\n" +
                    "https://kindahard.golf",
            )!!
        assertEquals(listOf(4, 0, 6, 15), result.holes)
        assertEquals(result.strokes, result.holes.sum())
    }

    @Test
    fun `a custom map has no number`() {
        val result =
            KindaHardGolfParser.parse(
                "kindahard.golf - Custom Map\n\"Windmill\"\n\n📝 22\n\n0.🏌️ 22\n\n" +
                    "https://kindahard.golf",
            )!!
        assertNull(result.puzzleNumber)
        assertEquals("Windmill", result.mapName)
        assertEquals(22, result.strokes)
    }

    @Test
    fun `a custom map without a name has no map name`() {
        val result =
            KindaHardGolfParser.parse(
                "kindahard.golf - Custom Map\n\n📝 9\n\n1.⛳ 4\n0.🏌️ 5\n\nhttps://kindahard.golf",
            )!!
        assertNull(result.puzzleNumber)
        assertNull(result.mapName)
        assertEquals(listOf(4, 5), result.holes)
    }

    @Test
    fun `a map name can contain quotes`() {
        val result =
            KindaHardGolfParser.parse(
                "kindahard.golf - Custom Map\n\"The \"Big\" One\"\n\n📝 22\n\n0.🏌️ 22\n\n" +
                    "https://kindahard.golf",
            )!!
        assertEquals("The \"Big\" One", result.mapName)
    }

    @Test
    fun `an infuriating round is marked`() {
        val result =
            KindaHardGolfParser.parse(
                "🔥 kindahard.golf #14 🔥\n\n📝 60\n\n2.⛳ 20\n1.⛳ 25\n0.🏌️ 15\n\n" +
                    "https://kindahard.golf",
            )!!
        assertTrue(result.infuriating)
        assertEquals("14", result.puzzleNumber)
        assertEquals(60, result.strokes)
    }

    @Test
    fun `an infuriating custom map is marked and named`() {
        val result =
            KindaHardGolfParser.parse(
                "🔥 kindahard.golf - Custom Map 🔥\n\"Windmill\"\n\n📝 22\n\n0.🏌️ 22\n\n" +
                    "https://kindahard.golf",
            )!!
        assertTrue(result.infuriating)
        assertNull(result.puzzleNumber)
        assertEquals("Windmill", result.mapName)
    }

    @Test
    fun `a daily header without a number has no number`() {
        val result =
            KindaHardGolfParser.parse(
                "kindahard.golf\n\n📝 12\n\n1.⛳ 5\n0.🏌️ 7\n\nhttps://kindahard.golf",
            )!!
        assertNull(result.puzzleNumber)
        assertNull(result.mapName)
        assertEquals(12, result.strokes)
    }

    @Test
    fun `a daily has no map name`() {
        assertNull(KindaHardGolfParser.parse(round)!!.mapName)
    }

    @Test
    fun `a round has no win or loss`() {
        assertNull(KindaHardGolfParser.parse(round)!!.success)
    }

    @Test
    fun `matches only kinda hard golf text`() {
        assertTrue(KindaHardGolfParser.matches(round))
        assertFalse(
            KindaHardGolfParser.matches("Parseword #240\n⚡️ 50s\n💎 Perfect\n🎭 Secret Found\n🥚 Learn Mode"),
        )
    }
}
