// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange

/** A formatting shortcut: maps (text, selection) to the edited text, see MarkdownFormatting.kt. */
internal enum class FormatAction(val glyph: String) {
    H1("H1"), H2("H2"), H3("H3"),
    Bold("B"), Italic("I"), Strikethrough("S"), InlineCode("</>"),
    BulletList("•"), NumberedList("1."), TaskList("☐"), Quote("❞"),
    Link("↗");

    /** Vector icon for toolbars and the palette; null for actions drawn as a styled glyph (H1, B, I…). */
    val icon: ImageVector?
        get() = when (this) {
            InlineCode -> KomdIcons.Code
            BulletList -> KomdIcons.BulletList
            NumberedList -> KomdIcons.NumberedList
            TaskList -> KomdIcons.Task
            Quote -> KomdIcons.Quote
            Link -> KomdIcons.Link
            else -> null
        }

    fun apply(text: String, selection: TextRange, ui: AppStrings): InsertResult = when (this) {
        H1 -> toggleHeading(text, selection, 1)
        H2 -> toggleHeading(text, selection, 2)
        H3 -> toggleHeading(text, selection, 3)
        Bold -> toggleInline(text, selection, InlineStyle.Bold, ui.formatPlaceholder)
        Italic -> toggleInline(text, selection, InlineStyle.Italic, ui.formatPlaceholder)
        Strikethrough -> toggleInline(text, selection, InlineStyle.Strikethrough, ui.formatPlaceholder)
        InlineCode -> toggleInline(text, selection, InlineStyle.Code, ui.code)
        BulletList -> toggleLineStyle(text, selection, LineStyle.Bullet)
        NumberedList -> toggleLineStyle(text, selection, LineStyle.Numbered)
        TaskList -> toggleLineStyle(text, selection, LineStyle.Task)
        Quote -> toggleLineStyle(text, selection, LineStyle.Quote)
        Link -> insertLinkAround(text, selection, ui.link)
    }

    fun label(ui: AppStrings): String = when (this) {
        H1 -> "${ui.heading} 1"
        H2 -> "${ui.heading} 2"
        H3 -> "${ui.heading} 3"
        Bold -> ui.bold
        Italic -> ui.italic
        Strikethrough -> ui.strikethrough
        InlineCode -> ui.code
        BulletList -> ui.list
        NumberedList -> ui.numberedList
        TaskList -> ui.taskList
        Quote -> ui.quote
        Link -> ui.link
    }

    companion object {
        val headings = listOf(H1, H2, H3)
        val inline = listOf(Bold, Italic, Strikethrough, InlineCode)
        val blocks = listOf(BulletList, NumberedList, TaskList, Quote)
    }
}
