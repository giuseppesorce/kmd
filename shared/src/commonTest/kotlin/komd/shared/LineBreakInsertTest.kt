// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LineBreakInsertTest {
    @Test
    fun midLineBecomesItsOwnParagraph() {
        val result = insertLineBreak("first second", TextRange(6))
        assertEquals("first \n\n<br>\n\nsecond", result.text)
        assertEquals("first \n\n<br>\n\n".length, result.selection.start)
    }

    @Test
    fun betweenParagraphsAddsNoExtraBlankLines() {
        val text = "a\n\nb"
        val result = insertLineBreak(text, TextRange(3))
        assertEquals("a\n\n<br>\n\nb", result.text)
        assertEquals(TextRange("a\n\n<br>\n\n".length), result.selection)
    }

    @Test
    fun atEndOfLineReusesTheExistingNewline() {
        val result = insertLineBreak("a\nb", TextRange(1))
        assertEquals("a\n\n<br>\n\nb", result.text)
    }

    @Test
    fun emptyDocumentAndEndOfDocument() {
        assertEquals("<br>\n\n", insertLineBreak("", TextRange(0)).text)
        val end = insertLineBreak("text", TextRange(4))
        assertEquals("text\n\n<br>\n\n", end.text)
        assertEquals(end.text.length, end.selection.start)
    }

    @Test
    fun repeatedPressesStackOneParagraphEach() {
        val once = insertLineBreak("a\n\nb", TextRange(3))
        val twice = insertLineBreak(once.text, once.selection)
        assertEquals("a\n\n<br>\n\n<br>\n\nb", twice.text)
    }

    @Test
    fun selectionIsReplaced() {
        assertEquals("a\n\n<br>\n\nc", insertLineBreak("a\nX\nc", TextRange(2, 3)).text)
    }

    @Test
    fun renderedAsAnEmptyParagraphInHtmlExport() {
        val html = buildHtmlExport("a\n\n<br>\n\nb", "t", null).html
        assertTrue("<p>a</p>" in html)
        assertTrue(Regex("<p><br>\\s*</p>").containsMatchIn(html), html)
    }

    @Test
    fun previewCountsBreakOnlyParagraphs() {
        val blocks = parseMarkdown("a\n\n<br>\n\n<br><br>\n\nb <br>")
        val counts = blocks.map { (it as MdBlock.Paragraph).inlines }.map(::lineBreakOnlyCount)
        assertEquals(listOf(0, 1, 2, 0), counts)
    }

    @Test
    fun horizontalRuleGetsABlankLineBeforeSoItIsNotASetextHeading() {
        val result = insertHorizontalRule("Title\nnext", TextRange(5))
        assertEquals("Title\n\n---\n\nnext", result.text)
        val blocks = parseMarkdown(result.text)
        assertEquals(MdBlock.Rule, blocks[1])
        assertTrue(blocks[0] is MdBlock.Paragraph)
    }

    @Test
    fun htmlHrOnItsOwnLineIsARule() {
        assertEquals(listOf(MdBlock.Rule), parseMarkdown("<hr>"))
        assertEquals(listOf(MdBlock.Rule), parseMarkdown("<hr />"))
        assertTrue("<hr>" in buildHtmlExport("a\n\n---\n\nb", "t", null).html)
    }
}
