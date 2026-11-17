package komd.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A formatting shortcut: maps (text, selection) to the edited text, see MarkdownFormatting.kt. */
internal enum class FormatAction(val glyph: String) {
    H1("H1"), H2("H2"), H3("H3"),
    Bold("B"), Italic("I"), Strikethrough("S"), InlineCode("</>"),
    BulletList("•"), NumberedList("1."), TaskList("☐"), Quote("❞"),
    Link("↗");

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

/** Sidebar section: heading and inline chips on two rows, then one row per block style. */
@Composable
internal fun SidebarFormatSection(onFormat: (FormatAction) -> Unit) {
    val colors = LocalKomdPalette.current
    val ui = LocalUiStrings.current
    Text(ui.formatting, Modifier.padding(top = 17.dp, bottom = 8.dp), color = colors.secondary, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FormatAction.headings.forEach { FormatChip(it, onFormat) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FormatAction.inline.forEach { FormatChip(it, onFormat) }
        }
    }
    Box(Modifier.height(6.dp))
    FormatAction.blocks.forEach { action ->
        Text(
            "${action.glyph.padEnd(4)}${action.label(ui)}",
            Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
                .clickable(role = Role.Button) { onFormat(action) }
                .padding(horizontal = 10.dp, vertical = 9.dp),
            color = colors.foreground,
            fontSize = 13.sp
        )
    }
}

/** Compact chips for the phone bottom bar. */
internal val mobileFormatActions = FormatAction.headings + listOf(FormatAction.Bold, FormatAction.Italic, FormatAction.BulletList, FormatAction.TaskList)

@Composable
private fun RowScope.FormatChip(action: FormatAction, onFormat: (FormatAction) -> Unit) {
    FormatChipContent(action, onFormat, Modifier.weight(1f))
}

@Composable
internal fun FormatChipContent(action: FormatAction, onFormat: (FormatAction) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalKomdPalette.current
    val ui = LocalUiStrings.current
    Box(
        modifier.clip(RoundedCornerShape(7.dp)).background(colors.button)
            .clickable(role = Role.Button) { onFormat(action) }
            .semantics { contentDescription = action.label(ui) }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            action.glyph,
            color = colors.foreground,
            fontSize = 12.sp,
            maxLines = 1,
            fontWeight = if (action == FormatAction.Bold || action in FormatAction.headings) FontWeight.Bold else FontWeight.Medium,
            fontStyle = if (action == FormatAction.Italic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (action == FormatAction.Strikethrough) TextDecoration.LineThrough else null,
            fontFamily = if (action == FormatAction.InlineCode) FontFamily.Monospace else null
        )
    }
}
