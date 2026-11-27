package komd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaskToggleTest {

    private fun taskLines(markdown: String): List<Pair<Int, Boolean?>> =
        parseMarkdown(markdown).flatMap { block ->
            when (block) {
                is MdBlock.Bullets -> block.items.map { it.line to it.checked }
                is MdBlock.Quote -> block.blocks.filterIsInstance<MdBlock.Bullets>().flatMap { b -> b.items.map { it.line to it.checked } }
                else -> emptyList()
            }
        }

    @Test
    fun togglesUncheckedAndChecked() {
        val md = "# Todo\n\n- [ ] one\n- [x] two\n- [X] three"
        assertEquals("# Todo\n\n- [x] one\n- [x] two\n- [X] three", toggleTaskAt(md, 2))
        assertEquals("# Todo\n\n- [ ] one\n- [ ] two\n- [X] three", toggleTaskAt(md, 3))
        assertEquals("# Todo\n\n- [ ] one\n- [x] two\n- [ ] three", toggleTaskAt(md, 4))
    }

    @Test
    fun parserLinesMatchSourceLines() {
        val md = "Intro\n\n* [ ] a\n  continuation\n+ [x] b\n\n> quote\n> - [ ] nested\n\n- plain"
        assertEquals(listOf(2 to false, 4 to true, 7 to false, 9 to null), taskLines(md))
        assertEquals("Intro\n\n* [ ] a\n  continuation\n+ [x] b\n\n> quote\n> - [x] nested\n\n- plain", toggleTaskAt(md, 7))
    }

    @Test
    fun keepsCrlfLineEndings() {
        val md = "- [ ] a\r\n- [ ] b\r\n"
        assertEquals("- [ ] a\r\n- [x] b\r\n", toggleTaskAt(md, 1))
    }

    @Test
    fun indentedAndRoundTrip() {
        val md = "  - [ ] indented"
        val once = toggleTaskAt(md, 0)!!
        assertEquals("  - [x] indented", once)
        assertEquals(md, toggleTaskAt(once, 0))
    }

    @Test
    fun nonTaskLinesReturnNull() {
        val md = "- plain\ntext [ ] here\n```\n- [ ] in code\n```"
        assertNull(toggleTaskAt(md, 0))
        assertNull(toggleTaskAt(md, 1))
        assertNull(toggleTaskAt(md, 99))
        assertNull(toggleTaskAt(md, -1))
    }
}
