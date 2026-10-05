package org.shawnz.dailytracker.parse

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate

class RaddleParserTest {
    // Copied from a finished ladder.
    private val solved =
        """
        COSTA → RICA [💯]
        Raddle #567 • Sep 14, 2026
        https://raddle.quest/567

        🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🙌
        """.trimIndent()

    @Test
    fun `reads the words, the number and the day`() {
        val result = RaddleParser.parse(solved)!!
        assertEquals("COSTA", result.from)
        assertEquals("RICA", result.to)
        assertEquals("567", result.puzzleNumber)
        assertEquals(LocalDate.of(2026, 9, 14), result.day)
    }

    // At a hundred, 💯 is shown in place of the number.
    @Test
    fun `a ladder without help scores a hundred and is a success`() {
        val result = RaddleParser.parse(solved)!!
        assertEquals(100, result.percentage)
        assertTrue(result.success!!)
    }

    // 🟢 no help, 💡 one reveal, 👁️ more than one.
    @Test
    fun `counts the rungs by how much help each took`() {
        val result = RaddleParser.parse(solved)!!
        assertEquals(11, result.clean)
        assertEquals(0, result.hinted)
        assertEquals(0, result.revealed)
        assertEquals(11, result.rungs)
    }

    @Test
    fun `a ladder that took help is counted rung by rung`() {
        val result =
            RaddleParser.parse(
                "NORTH → WESTERN [80%]\nRaddle #560 • Sep 07, 2026\n\n🟢🟢💡🟢👁️🟢🤝🟢💡🟢🟢🟢",
            )!!
        assertEquals(8, result.clean)
        assertEquals(2, result.hinted)
        assertEquals(1, result.revealed)
        assertEquals(11, result.rungs)
    }

    @Test
    fun `a ladder that took help has its percentage and is not a success`() {
        val result =
            RaddleParser.parse(
                "NORTH → WESTERN [80%]\nRaddle #560 • Sep 07, 2026\n\n🟢🟢💡🟢👁️🟢🤝🟢💡🟢🟢🟢",
            )!!
        assertEquals(80, result.percentage)
        assertFalse(result.success!!)
    }

    // Sharing through the system sheet drops the link, so nothing may depend on it.
    @Test
    fun `a share with no link still parses`() {
        val result =
            RaddleParser.parse(
                "COSTA → RICA [💯]\nRaddle #567 • Sep 14, 2026\n\n🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🙌",
            )!!
        assertEquals("567", result.puzzleNumber)
        assertEquals(LocalDate.of(2026, 9, 14), result.day)
        assertEquals(11, result.rungs)
    }

    // The day is written with two digits, and the year is always included.
    @Test
    fun `a single figure day is read`() {
        assertEquals(
            LocalDate.of(2026, 9, 7),
            RaddleParser
                .parse("NORTH → WESTERN [💯]\nRaddle #560 • Sep 07, 2026\n\n🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🙌")!!
                .day,
        )
    }

    // A ladder finished at the top has 🙌 before the first rung.
    @Test
    fun `a ladder finished at the top is counted`() {
        val result =
            RaddleParser.parse(
                "COSTA → RICA [💯]\nRaddle #567 • Sep 14, 2026\nhttps://raddle.quest/567\n\n🙌🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢",
            )!!
        assertEquals(11, result.rungs)
        assertEquals(100, result.percentage)
    }

    // A restored ladder can lack the mark for where it was finished.
    @Test
    fun `a ladder without a finishing mark is counted`() {
        val result =
            RaddleParser.parse(
                "COSTA → RICA [💯]\nRaddle #567 • Sep 14, 2026\n\n🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢",
            )!!
        assertEquals(11, result.rungs)
    }

    @Test
    fun `words with spaces and a low percentage are read`() {
        val result =
            RaddleParser.parse(
                "ACTION STAR → ACTOR [16%]\nRaddle #403 • Apr 03, 2026\n\n🟢👁️👁️👁️👁️🤝👁️👁️👁️👁️👁️👁️🟢",
            )!!
        assertEquals("ACTION STAR", result.from)
        assertEquals("ACTOR", result.to)
        assertEquals(16, result.percentage)
        assertEquals(10, result.revealed)
        assertEquals(12, result.rungs)
        assertEquals(LocalDate.of(2026, 4, 3), result.day)
    }

    // The end word is hidden for the last step, so help can be used on it too.
    @Test
    fun `a hinted end word is counted`() {
        val result =
            RaddleParser.parse(
                "COSTA → RICA [94%]\nRaddle #567 • Sep 14, 2026\n\n🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢💡🙌",
            )!!
        assertEquals(10, result.clean)
        assertEquals(1, result.hinted)
        assertEquals(11, result.rungs)
        assertEquals(94, result.percentage)
    }

    // Hand-edited: a link pasted after the ladder.
    @Test
    fun `a link after the ladder is ignored`() {
        val result =
            RaddleParser.parse(
                "COSTA → RICA [💯]\nRaddle #567 • Sep 14, 2026\n\n🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🟢🙌 https://raddle.quest/567",
            )!!
        assertEquals("567", result.puzzleNumber)
        assertEquals(11, result.rungs)
    }

    @Test
    fun `matches only raddle text`() {
        assertTrue(RaddleParser.matches(solved))
        assertFalse(
            RaddleParser.matches("Wordle 1,234 4/6\n\n⬛⬛⬛🟨⬛\n⬛🟨⬛⬛⬛\n⬛🟩🟩⬛🟨\n🟩🟩🟩🟩🟩"),
        )
        assertNull(RaddleParser.parse("Krillion #61 🦐\n150\n\n🐟🐟🐟🫧🐟🫧🫧"))
    }
}
