// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PrintHtmlExportTest {

    @Test
    fun localImagesBecomeAbsoluteFileUrisAndNothingIsCopied() {
        val md = "![Logo](assets/my logo.png)\n\n![Remote](https://example.com/a.png)"
        val export = buildHtmlExport(md, "Doc", "/Users/me/docs", ExportMedium.Print)
        assertTrue(export.images.isEmpty())
        assertTrue("src=\"file:///Users/me/docs/assets/my%20logo.png\"" in export.html)
        assertTrue("src=\"https://example.com/a.png\"" in export.html)
    }

    @Test
    fun printStylesheetHasPagesAndNoCssVariables() {
        val html = buildHtmlExport("Some `code`", "Doc", null, ExportMedium.Print).html
        assertTrue("@page" in html)
        assertFalse("var(--" in html)
        assertFalse("prefers-color-scheme" in html)
    }

    @Test
    fun taskCheckboxesAreCssBoxes() {
        val html = buildHtmlExport("- [x] done\n- [ ] todo", "Doc", null, ExportMedium.Print).html
        assertTrue("<li><span class=\"box on\"></span>done</li>" in html)
        assertTrue("<li><span class=\"box\"></span>todo</li>" in html)
        assertFalse("<input" in html)
    }

    @Test
    fun inlineMonospaceUsesPrintFamily() {
        val html = buildHtmlExport("<kbd>Ctrl</kbd>", "Doc", null, ExportMedium.Print).html
        assertFalse("var(--mono)" in html)
    }

    @Test
    fun webExportIsUnchanged() {
        val html = buildHtmlExport("- [x] done", "Doc", null).html
        assertTrue("<input type=\"checkbox\" disabled checked> done" in html)
        assertTrue("var(--fg)" in html)
    }

    @Test
    fun directoryUriEndsWithSlash() {
        assertEquals("file:///Users/me/my%20docs/", directoryFileUri("/Users/me/my docs"))
        assertEquals("file:///C:/Docs/", directoryFileUri("C:\\Docs"))
        assertEquals(null, directoryFileUri("content://x/y"))
    }
}
