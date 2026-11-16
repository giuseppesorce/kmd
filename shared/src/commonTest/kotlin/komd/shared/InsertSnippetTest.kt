package komd.shared

import androidx.compose.ui.text.TextRange
import kotlin.test.Test
import kotlin.test.assertEquals

class InsertSnippetTest {
    private val img = "<img src=\"a.png\" alt=\"x\" />"

    @Test
    fun imageGoesOnItsOwnLineAtTheCaretNotAtTheEnd() {
        val text = "line one\nline two\nline three"
        val caret = "line one".length
        val result = insertSnippet(text, TextRange(caret), img)
        assertEquals("line one\n$img\nline two\nline three", result.text)
        assertEquals("line one\n$img".length, result.selection.start)
    }

    @Test
    fun blockAtStartOfLineDoesNotAddLeadingNewline() {
        val result = insertSnippet("a\nb", TextRange(2), img)
        assertEquals("a\n$img\nb", result.text)
    }

    @Test
    fun inlineSnippetReplacesSelection() {
        val result = insertSnippet("hello world", TextRange(6, 11), "**text**")
        assertEquals("hello **text**", result.text)
        assertEquals(TextRange(14), result.selection)
    }

    @Test
    fun outOfRangeSelectionIsClamped() {
        val result = insertSnippet("abc", TextRange(99), "x")
        assertEquals("abcx", result.text)
    }
}

class ScaledScrollTest {
    @Test
    fun mapsProportionally() = assertEquals(500, scaledScroll(fromValue = 100, fromMax = 200, toMax = 1000))

    @Test
    fun emptyRangesMapToTop() {
        assertEquals(0, scaledScroll(50, 0, 1000))
        assertEquals(0, scaledScroll(50, 100, 0))
    }

    @Test
    fun clampsToTargetRange() = assertEquals(1000, scaledScroll(300, 200, 1000))
}
