package komd.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentOutlineTest {
    @Test
    fun collectsAtxHeadingsWithLevelLineAndOffset() {
        val md = "# Titolo\n\ntesto\n## Sezione ##\n### Sotto"
        assertEquals(
            listOf(
                OutlineEntry(1, "Titolo", 0, 0),
                OutlineEntry(2, "Sezione", 3, 16),
                OutlineEntry(3, "Sotto", 4, 30),
            ),
            documentOutline(md)
        )
    }

    @Test
    fun skipsFencedCodeAndNonHeadings() {
        val md = "#hashtag\n```bash\n# commento\n```\n~~~\n## dentro\n~~~\n####### sette\n#   \n## Fuori"
        assertEquals(listOf(OutlineEntry(2, "Fuori", 9, md.lastIndexOf("## Fuori"))), documentOutline(md))
    }

    @Test
    fun handlesCrLf() {
        assertEquals(listOf(OutlineEntry(1, "A", 0, 0), OutlineEntry(2, "B", 1, 5)), documentOutline("# A\r\n## B\r\n"))
    }

    @Test
    fun caretPositionIsOneBased() {
        val text = "ab\ncde\n"
        assertEquals(1 to 1, caretPosition(text, 0))
        assertEquals(1 to 3, caretPosition(text, 2))
        assertEquals(2 to 1, caretPosition(text, 3))
        assertEquals(2 to 3, caretPosition(text, 5))
        assertEquals(3 to 1, caretPosition(text, 99))
    }
}

class HeadingLevelAtTest {
    @Test
    fun detectsHeadingOfCaretLine() {
        val text = "# Uno\ntesto\n### Tre\n#niente"
        assertEquals(1, headingLevelAt(text, 0))
        assertEquals(1, headingLevelAt(text, 5))
        assertEquals(0, headingLevelAt(text, 8))
        assertEquals(3, headingLevelAt(text, text.indexOf("Tre")))
        assertEquals(0, headingLevelAt(text, text.length))
    }
}
