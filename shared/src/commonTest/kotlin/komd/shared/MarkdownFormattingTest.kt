// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

class HeadingTest {
    @Test
    fun addsHeadingToCurrentLineNotAtTheEnd() {
        val text = "intro\ntitle\noutro"
        val result = toggleHeading(text, TextRange(8), 2)
        assertEquals("intro\n## title\noutro", result.text)
        assertEquals(TextRange(11), result.selection) // caret stays on the same character
    }

    @Test
    fun sameLevelRemovesIt() = assertEquals("title", toggleHeading("## title", TextRange(5), 2).text)

    @Test
    fun otherLevelReplacesIt() = assertEquals("# title", toggleHeading("### title", TextRange(9), 1).text)

    @Test
    fun emptyLineGetsPrefixAndCaretAfterIt() {
        val result = toggleHeading("a\n\nb", TextRange(2), 3)
        assertEquals("a\n### \nb", result.text)
        assertEquals(TextRange(6), result.selection)
    }

    @Test
    fun multiLineSelectionSkipsBlankLines() {
        val result = toggleHeading("one\n\ntwo", TextRange(0, 8), 1)
        assertEquals("# one\n\n# two", result.text)
        assertEquals(TextRange(0, 12), result.selection)
    }

    @Test
    fun selectionEndingAtColumnZeroDoesNotTouchNextLine() =
        assertEquals("# one\ntwo", toggleHeading("one\ntwo", TextRange(0, 4), 1).text)
}

class LineStyleTest {
    @Test
    fun bulletTogglesOnAndOff() {
        val on = toggleLineStyle("a\nb", TextRange(0, 3), LineStyle.Bullet)
        assertEquals("- a\n- b", on.text)
        assertEquals("a\nb", toggleLineStyle(on.text, on.selection, LineStyle.Bullet).text)
    }

    @Test
    fun numberedCountsAndReplacesBullets() =
        assertEquals("1. a\n2. b", toggleLineStyle("- a\n* b", TextRange(0, 7), LineStyle.Numbered).text)

    @Test
    fun taskKeepsIndentation() =
        assertEquals("  - [ ] sub", toggleLineStyle("  - sub", TextRange(3), LineStyle.Task).text)

    @Test
    fun taskOffRemovesWholeMarker() =
        assertEquals("done", toggleLineStyle("- [x] done", TextRange(0), LineStyle.Task).text)

    @Test
    fun quoteTogglesAtColumnZero() {
        assertEquals("> a\n> b", toggleLineStyle("a\nb", TextRange(0, 3), LineStyle.Quote).text)
        assertEquals("a\nb", toggleLineStyle("> a\n> b", TextRange(0, 7), LineStyle.Quote).text)
    }

    @Test
    fun thematicBreakIsNotABullet() =
        assertEquals("- ---", toggleLineStyle("---", TextRange(0), LineStyle.Bullet).text)
}

class InlineStyleTest {
    @Test
    fun wrapsSelection() {
        val result = toggleInline("say hello now", TextRange(4, 9), InlineStyle.Bold, "x")
        assertEquals("say **hello** now", result.text)
        assertEquals(TextRange(6, 11), result.selection)
    }

    @Test
    fun unwrapsWhenMarkersAreOutsideSelection() {
        val result = toggleInline("say **hello** now", TextRange(6, 11), InlineStyle.Bold, "x")
        assertEquals("say hello now", result.text)
        assertEquals(TextRange(4, 9), result.selection)
    }

    @Test
    fun unwrapsWhenMarkersAreInsideSelection() =
        assertEquals("say hello", toggleInline("say *hello*", TextRange(4, 11), InlineStyle.Italic, "x").text)

    @Test
    fun italicDoesNotUnwrapBold() =
        assertEquals("***hi***", toggleInline("**hi**", TextRange(2, 4), InlineStyle.Italic, "x").text)

    @Test
    fun emptySelectionInsertsSelectedPlaceholder() {
        val result = toggleInline("a ", TextRange(2), InlineStyle.Strikethrough, "text")
        assertEquals("a ~~text~~", result.text)
        assertEquals(TextRange(4, 8), result.selection)
    }

    @Test
    fun emptyPairIsRemovedOnSecondPress() =
        assertEquals("a  b", toggleInline("a **** b", TextRange(4), InlineStyle.Bold, "x").text)

    @Test
    fun trailingSpaceStaysOutsideMarkers() =
        assertEquals("**word** next", toggleInline("word next", TextRange(0, 5), InlineStyle.Bold, "x").text)

    @Test
    fun multiLineCodeBecomesFence() =
        assertEquals("```\na\nb\n```\n", toggleInline("a\nb", TextRange(0, 3), InlineStyle.Code, "x").text)

    // A whole table row selected (long cell soft-wrapped in the editor): the selection is wrapped,
    // not replaced by a placeholder, and the leading/trailing pipes stay outside the markers.
    private val row = "| --- |\n| Backoffice COR (approvazione progetti) e un registro lato server. |\n<br>"
    private val cell = "Backoffice COR (approvazione progetti) e un registro lato server."
    private val rowStart = row.indexOf("| Backoffice")
    private val rowEnd = row.indexOf(" |\n<br>") + 2

    @Test
    fun wholeTableRowKeepsPipesOutsideEveryInlineStyle() {
        listOf(InlineStyle.Bold, InlineStyle.Italic, InlineStyle.Strikethrough, InlineStyle.Code).forEach { style ->
            val m = style.marker
            val result = toggleInline(row, TextRange(rowStart, rowEnd), style, "testo")
            assertEquals(row.replace(cell, m + cell + m), result.text, style.name)
            val s = rowStart + 2 + m.length
            assertEquals(TextRange(s, s + cell.length), result.selection, style.name)
        }
    }

    @Test
    fun cellWithoutTrailingPipeIsWrapped() {
        // As in the bug report: "| text" selected up to the end of the line, no closing pipe.
        val text = "| $cell\n<br>"
        val result = toggleInline(text, TextRange(0, text.indexOf('\n')), InlineStyle.Bold, "testo")
        assertEquals("| **$cell**\n<br>", result.text)
    }

    @Test
    fun selectedTableCellIsUnwrappedOnSecondPress() {
        val bold = "| **$cell** |"
        assertEquals("| $cell |", toggleInline(bold, TextRange(0, bold.length), InlineStyle.Bold, "testo").text)
    }

    @Test
    fun onlyPipesSelectedFallsBackToPlaceholder() =
        assertEquals("**testo**| |", toggleInline("| |", TextRange(0, 3), InlineStyle.Bold, "testo").text)
}

class LinkTest {
    @Test
    fun wrapsSelectionAndSelectsUrl() {
        val result = insertLinkAround("see docs", TextRange(4, 8), "Link")
        assertEquals("see [docs](https://)", result.text)
        assertEquals("https://", result.text.substring(result.selection.min, result.selection.max))
    }

    @Test
    fun noSelectionSelectsLabel() {
        val result = insertLinkAround("", TextRange(0), "Link")
        assertEquals("[Link](https://)", result.text)
        assertEquals(TextRange(1, 5), result.selection)
    }
}
