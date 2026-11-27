// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class HtmlExportTest {

    @Test
    fun documentWithoutImagesHasNothingToCopy() {
        val export = buildHtmlExport("# Title\n\nSome **bold** and *italic* text.", "Notes", "/Users/me/docs")
        assertTrue(export.images.isEmpty())
        assertTrue("<title>Notes</title>" in export.html)
        assertTrue("<h1>Title</h1>" in export.html)
        assertTrue("<p>Some <strong>bold</strong> and <em>italic</em> text.</p>" in export.html)
    }

    @Test
    fun relativeImagesAreCollectedAndRewritten() {
        val md = "![Logo](assets/logo.png)\n\nText ![inline](../shared/pic.jpg) here."
        val export = buildHtmlExport(md, "Doc", "/Users/me/docs")
        assertEquals(
            listOf(
                ExportedImage("/Users/me/docs/assets/logo.png", "logo.png"),
                ExportedImage("/Users/me/shared/pic.jpg", "pic.jpg"),
            ),
            export.images
        )
        assertTrue("src=\"images/logo.png\"" in export.html)
        assertTrue("src=\"images/pic.jpg\"" in export.html)
    }

    @Test
    fun htmlImgWithAlignmentAndAbsoluteFileUri() {
        val md = """<img src="file:///Users/me/Pictures/My%20Photo.png" alt="Photo" style="display:block;max-width:100%;margin-left:auto;margin-right:auto;" />"""
        val export = buildHtmlExport(md, "Doc", "/Users/me/docs")
        assertEquals(listOf(ExportedImage("/Users/me/Pictures/My Photo.png", "My Photo.png")), export.images)
        assertTrue("<p class=\"image\" style=\"text-align:center\"><img src=\"images/My%20Photo.png\" alt=\"Photo\"></p>" in export.html)
    }

    @Test
    fun remoteImagesAreLeftAlone() {
        val export = buildHtmlExport("![Remote](https://example.com/a.png)", "Doc", "/Users/me/docs")
        assertTrue(export.images.isEmpty())
        assertTrue("src=\"https://example.com/a.png\"" in export.html)
    }

    @Test
    fun sameImageIsCopiedOnceAndNameClashesGetASuffix() {
        val md = "![a](one/pic.png)\n\n![b](two/PIC.png)\n\n![c](one/pic.png)"
        val export = buildHtmlExport(md, "Doc", "/d")
        assertEquals(listOf("pic.png", "PIC-2.png"), export.images.map { it.fileName })
        assertEquals(2, Regex("""src="images/pic\.png"""").findAll(export.html).count())
        assertTrue("src=\"images/PIC-2.png\"" in export.html)
    }

    @Test
    fun imagesInsideCodeAreNotExported() {
        val export = buildHtmlExport("```\n![x](a.png)\n```\n\nUse `![x](b.png)`", "Doc", "/d")
        assertTrue(export.images.isEmpty())
        assertTrue("<pre><code>![x](a.png)</code></pre>" in export.html)
    }

    @Test
    fun textIsEscaped() {
        val export = buildHtmlExport("a < b && \"c\"", "<T>", null)
        assertTrue("<title>&lt;T&gt;</title>" in export.html)
        assertTrue("<p>a &lt; b &amp;&amp; \"c\"</p>" in export.html)
        assertFalse("<T>" in export.html)
    }

    @Test
    fun tablesTasksAndInlineHtml() {
        val md = """
            | A | B |
            | :-: | --: |
            | 1 | 2 |

            - [x] done
            - [ ] todo

            Press <kbd>⌘</kbd> and <mark>look</mark>.
        """.trimIndent()
        val html = buildHtmlExport(md, "Doc", null).html
        assertTrue("<th style=\"text-align:center\">A</th><th style=\"text-align:right\">B</th>" in html)
        assertTrue("<td style=\"text-align:center\">1</td>" in html)
        assertTrue("<ul class=\"tasks\">" in html)
        assertTrue("<input type=\"checkbox\" disabled checked> done" in html)
        assertTrue("<input type=\"checkbox\" disabled> todo" in html)
        assertTrue("font-family:var(--mono)" in html)
        assertTrue("background:#fff3a0" in html)
    }

    @Test
    fun unsavedDocumentKeepsUnresolvableRelativeSources() {
        val export = buildHtmlExport("![x](pic.png)", "Doc", null)
        assertTrue(export.images.isEmpty())
        assertTrue("src=\"pic.png\"" in export.html)
    }
}
