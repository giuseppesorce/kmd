// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import androidx.compose.ui.text.TextRange

internal enum class ImageAlignment { Left, Center, Right }

internal data class PreviewImage(
    val alt: String,
    val source: String,
    val alignment: ImageAlignment,
    /** Explicit display width in dp, from `<img width="…">` or `style="width:…px"`. */
    val width: Int? = null,
)

internal data class InsertResult(val text: String, val selection: TextRange)

/**
 * Inserts [snippet] at the caret, replacing any selected text. Block-level snippets (those that
 * start with a newline, and `<img>` tags) are padded so they always
 * sit on their own line, without doubling blank lines that are already there.
 */
internal fun insertSnippet(text: String, selection: TextRange, snippet: String): InsertResult {
    val start = selection.min.coerceIn(0, text.length)
    val end = selection.max.coerceIn(start, text.length)
    val block = snippet.startsWith("\n") || snippet.startsWith("<img")
    val body = if (block) snippet.trim('\n') else snippet
    val before = text.substring(0, start)
    val after = text.substring(end)
    val prefix = if (block && before.isNotEmpty() && !before.endsWith("\n")) "\n" else ""
    val suffix = if (block && !after.startsWith("\n")) "\n" else ""
    val inserted = prefix + body + suffix
    val caret = start + prefix.length + body.length + suffix.length
    return InsertResult(before + inserted + after, TextRange(caret))
}

/** Markup inserted by the "BR" toolbar button: an HTML line break, the only way to add blank space in Markdown. */
internal const val LINE_BREAK_HTML = "<br>"

/** Markup inserted by the horizontal-line button (CommonMark thematic break). */
internal const val HORIZONTAL_RULE_MARKDOWN = "---"

/**
 * Inserts [LINE_BREAK_HTML] at the caret as a paragraph of its own, so it renders as one empty line in
 * preview, HTML and PDF instead of a trailing break inside the previous paragraph, which browsers ignore.
 * Repeated presses stack, one empty line each.
 */
internal fun insertLineBreak(text: String, selection: TextRange): InsertResult =
    insertStandaloneBlock(text, selection, LINE_BREAK_HTML)

/**
 * Inserts [HORIZONTAL_RULE_MARKDOWN] as a block of its own. The blank line before matters: `---` right
 * under a text line would turn that line into a setext heading instead of drawing a line.
 */
internal fun insertHorizontalRule(text: String, selection: TextRange): InsertResult =
    insertStandaloneBlock(text, selection, HORIZONTAL_RULE_MARKDOWN)

/** Puts [markup] at the caret with a blank line before and after (none added where one exists); replaces a selection. */
private fun insertStandaloneBlock(text: String, selection: TextRange, markup: String): InsertResult {
    val start = selection.min.coerceIn(0, text.length)
    val end = selection.max.coerceIn(start, text.length)
    val before = text.substring(0, start)
    val after = text.substring(end)
    val prefix = when {
        before.isEmpty() || before.endsWith("\n\n") -> ""
        before.endsWith("\n") -> "\n"
        else -> "\n\n"
    }
    val suffix = when {
        after.startsWith("\n\n") -> ""
        after.startsWith("\n") -> "\n"
        else -> "\n\n"
    }
    // Caret past the blank line that follows the block: ready to type, or to press the button again.
    val caret = start + prefix.length + markup.length + 2
    return InsertResult(before + prefix + markup + suffix + after, TextRange(caret))
}

internal fun alignedImageHtml(alt: String, source: String, alignment: ImageAlignment): String {
    val imageSource = if (source.startsWith("/")) fileUri(source) else source
    val margins = when (alignment) {
        ImageAlignment.Left -> "margin-left:0;margin-right:auto;"
        ImageAlignment.Center -> "margin-left:auto;margin-right:auto;"
        ImageAlignment.Right -> "margin-left:auto;margin-right:0;"
    }
    return "<img src=\"${escapeHtmlAttribute(imageSource)}\" alt=\"${escapeHtmlAttribute(alt)}\" style=\"display:block;max-width:100%;$margins\" />"
}

internal fun fileUri(path: String): String {
    val safe = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789/-._~!$&'()*+,;=:@"
    val hex = "0123456789ABCDEF"
    val encodedPath = buildString {
        path.encodeToByteArray().forEach { byte ->
            val value = byte.toInt() and 0xFF
            val character = value.toChar()
            if (value < 128 && character in safe) append(character)
            else {
                append('%')
                append(hex[value shr 4])
                append(hex[value and 0x0F])
            }
        }
    }
    return "file://$encodedPath"
}

private fun escapeHtmlAttribute(value: String): String = value
    .replace("&", "&amp;")
    .replace("\"", "&quot;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")

internal fun markdownLink(label: String, target: String): String {
    val safeLabel = label.replace("\\", "\\\\").replace("[", "\\[").replace("]", "\\]")
    val safeTarget = target.replace("\n", "").replace(" ", "%20").replace(">", "%3E").replace("<", "%3C")
    return "[$safeLabel](<$safeTarget>)"
}

private val markdownImagePattern = Regex("""^!\[(.*?)\]\((?:<([^>]+)>|([^\s)]+))\)$""")
private val htmlImagePattern = Regex("""<img\b([^>]*)/?>""", RegexOption.IGNORE_CASE)
private val htmlAttributePattern = Regex("""([\w:-]+)\s*=\s*["']([^"']*)["']""")

internal fun parsePreviewImage(line: String): PreviewImage? {
    val trimmed = line.trim()
    val htmlMatch = htmlImagePattern.matchEntire(trimmed)
    if (htmlMatch != null) {
        val attributes = htmlAttributePattern.findAll(htmlMatch.groupValues[1])
            .associate { it.groupValues[1].lowercase() to it.groupValues[2] }
        val source = attributes["src"] ?: return null
        val style = attributes["style"].orEmpty().lowercase().replace(" ", "")
        val alignment = when {
            "margin-left:auto" in style && "margin-right:auto" in style -> ImageAlignment.Center
            "margin-left:auto" in style -> ImageAlignment.Right
            else -> ImageAlignment.Left
        }
        val width = attributes["width"]?.removeSuffix("px")?.trim()?.toIntOrNull()
            ?: Regex("""(?:^|;)width:(\d+)px""").find(style)?.groupValues?.get(1)?.toIntOrNull()
        return PreviewImage(attributes["alt"].orEmpty(), unescapeHtmlAttribute(source), alignment, width?.takeIf { it > 0 })
    }

    val markdownMatch = markdownImagePattern.matchEntire(trimmed) ?: return null
    val source = markdownMatch.groups[2]?.value ?: markdownMatch.groups[3]?.value ?: return null
    return PreviewImage(markdownMatch.groups[1]?.value.orEmpty(), source, ImageAlignment.Left)
}

private fun unescapeHtmlAttribute(value: String): String = value
    .replace("&quot;", "\"")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&amp;", "&")

