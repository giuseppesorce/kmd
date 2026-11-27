// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import androidx.compose.ui.text.TextRange

/*
 * Pure text transforms behind the formatting shortcuts (H1-H3, bold, italic, lists...).
 *
 * Every function takes the current text plus selection and returns the new text plus selection,
 * so the UI only has to apply an [InsertResult]. Two families:
 *  - line transforms (headings, lists, quote): act on every line touched by the selection and
 *    toggle, i.e. applying the same format twice gives back the original text;
 *  - inline transforms (bold, italic, strike, code, link): wrap the selection, or insert a
 *    placeholder and select it when nothing is selected.
 */

internal enum class InlineStyle(val marker: String) {
    Bold("**"), Italic("*"), Strikethrough("~~"), Code("`")
}

internal enum class LineStyle { Bullet, Numbered, Task, Quote }

/** Heading level [level] (1-6) on the touched lines; same level again removes it, another level replaces it. */
internal fun toggleHeading(text: String, selection: TextRange, level: Int): InsertResult {
    require(level in 1..6)
    val target = "#".repeat(level) + " "
    return editLines(text, selection) { lines ->
        val content = lines.filter { it.isNotBlank() }.ifEmpty { lines }
        val remove = content.all { headingPrefix.find(it)?.groupValues?.get(1)?.length == level }
        lines.map { line ->
            val existing = headingPrefix.find(line)?.value?.length ?: 0
            when {
                remove -> LineEdit(0, existing, "")
                line.isBlank() && lines.size > 1 -> LineEdit.None
                else -> LineEdit(0, existing, target)
            }
        }
    }
}

/** Bullet / numbered / task list or blockquote on the touched lines, toggled. List styles replace each other. */
internal fun toggleLineStyle(text: String, selection: TextRange, style: LineStyle): InsertResult = editLines(text, selection) { lines ->
    val skipBlank = lines.size > 1
    val content = lines.filter { !skipBlank || it.isNotBlank() }
    if (style == LineStyle.Quote) {
        val remove = content.all { quotePrefix.containsMatchIn(it) }
        lines.map { line ->
            when {
                skipBlank && line.isBlank() -> LineEdit.None
                remove -> LineEdit(0, quotePrefix.find(line)!!.value.length, "")
                else -> LineEdit(0, 0, "> ")
            }
        }
    } else {
        val remove = content.all { listMarker(it)?.style == style }
        var number = 0
        lines.map { line ->
            if (skipBlank && line.isBlank()) return@map LineEdit.None
            val indent = line.length - line.trimStart().length
            val existing = listMarker(line)?.length ?: 0
            val marker = when {
                remove -> ""
                style == LineStyle.Bullet -> "- "
                style == LineStyle.Task -> "- [ ] "
                else -> "${++number}. "
            }
            LineEdit(indent, existing, marker)
        }
    }
}

/**
 * Wraps the selection in [style]'s marker, or unwraps it when it is already wrapped (markers
 * inside or just outside the selection). With no selection inserts `marker placeholder marker`
 * and selects the placeholder so typing replaces it. Inline code over several lines becomes a fence.
 */
internal fun toggleInline(text: String, selection: TextRange, style: InlineStyle, placeholder: String): InsertResult {
    var start = selection.min.coerceIn(0, text.length)
    var end = selection.max.coerceIn(start, text.length)
    val marker = style.marker

    if (start == end) {
        // Caret between an empty pair (e.g. "**|**"): remove it, so the button undoes itself.
        if (text.startsWith(marker, end) && text.substring(0, start).endsWith(marker) &&
            wrapped(trailingStars(text.substring(0, start)), leadingStars(text.substring(end)), style)
        ) {
            val s = start - marker.length
            return InsertResult(text.removeRange(s, end + marker.length), TextRange(s))
        }
        val inserted = marker + placeholder + marker
        val s = start + marker.length
        return InsertResult(text.replaceRange(start, end, inserted), TextRange(s, s + placeholder.length))
    }

    // "**word **" is not bold in Markdown: keep surrounding whitespace outside the markers.
    while (start < end && text[start].isWhitespace()) start++
    while (end > start && text[end - 1].isWhitespace()) end--
    if (start == end) return toggleInline(text, TextRange(selection.min), style, placeholder)

    val selected = text.substring(start, end)
    if (style == InlineStyle.Code && '\n' in selected) {
        return insertSnippet(text, TextRange(start, end), "\n```\n$selected\n```\n")
    }
    val before = text.substring(0, start)
    val after = text.substring(end)
    if (selected.length >= 2 * marker.length && selected.startsWith(marker) && selected.endsWith(marker) &&
        wrapped(leadingStars(selected), trailingStars(selected), style)
    ) {
        val inner = selected.substring(marker.length, selected.length - marker.length)
        return InsertResult(before + inner + after, TextRange(start, start + inner.length))
    }
    if (before.endsWith(marker) && after.startsWith(marker) &&
        wrapped(trailingStars(before), leadingStars(after), style)) {
        val s = start - marker.length
        return InsertResult(before.dropLast(marker.length) + selected + after.drop(marker.length), TextRange(s, s + selected.length))
    }
    val s = start + marker.length
    return InsertResult(before + marker + selected + marker + after, TextRange(s, s + selected.length))
}

/** `[selection](url)` with the url selected; with no selection `[label](url)` with the label selected. */
internal fun insertLinkAround(text: String, selection: TextRange, label: String, url: String = "https://"): InsertResult {
    val start = selection.min.coerceIn(0, text.length)
    val end = selection.max.coerceIn(start, text.length)
    val selected = text.substring(start, end)
    return if (selected.isEmpty() || '\n' in selected) {
        val s = start + 1
        InsertResult(text.replaceRange(start, end, "[$label]($url)"), TextRange(s, s + label.length))
    } else {
        val s = start + selected.length + 3
        InsertResult(text.replaceRange(start, end, "[$selected]($url)"), TextRange(s, s + url.length))
    }
}

// ---- internals ----------------------------------------------------------------------------------

private val headingPrefix = Regex("""^(#{1,6})(?:[ \t]+|$)""")
private val quotePrefix = Regex("""^[ \t]*> ?""")
private val taskMarker = Regex("""^[-*+][ \t]+\[[ xX]][ \t]+""")
private val bulletMarker = Regex("""^[-*+][ \t]+""")
private val numberedMarker = Regex("""^\d+[.)][ \t]+""")

private data class ListMarker(val style: LineStyle, val length: Int)

private fun listMarker(line: String): ListMarker? {
    val body = line.trimStart()
    taskMarker.find(body)?.let { return ListMarker(LineStyle.Task, it.value.length) }
    // "---" / "* * *" are thematic breaks, not bullets: the bullet pattern needs a space after one marker.
    bulletMarker.find(body)?.let { return ListMarker(LineStyle.Bullet, it.value.length) }
    numberedMarker.find(body)?.let { return ListMarker(LineStyle.Numbered, it.value.length) }
    return null
}

/**
 * `*` vs `**` ambiguity: italic needs an odd run of `*` on both sides (`*x*`, `***x***`),
 * bold needs a run of at least two. Other markers are unambiguous.
 */
private fun wrapped(runBefore: Int, runAfter: Int, style: InlineStyle): Boolean {
    if (style != InlineStyle.Italic && style != InlineStyle.Bold) return true
    return if (style == InlineStyle.Italic) runBefore % 2 == 1 && runAfter % 2 == 1 else runBefore >= 2 && runAfter >= 2
}

private fun leadingStars(s: String) = s.takeWhile { it == '*' }.length
private fun trailingStars(s: String) = s.takeLastWhile { it == '*' }.length

/** Replace [removeLength] chars at column [offset] with [insert]. */
private data class LineEdit(val offset: Int, val removeLength: Int, val insert: String) {
    /** [stickLeft]: a position right at [offset] stays before the inserted text (start of a ranged selection). */
    fun mapColumn(column: Int, stickLeft: Boolean = false): Int = when {
        column < offset || (stickLeft && column == offset) -> column
        column < offset + removeLength || column == offset -> offset + insert.length
        else -> column - removeLength + insert.length
    }

    companion object {
        val None = LineEdit(0, 0, "")
    }
}

/**
 * Applies one [LineEdit] per line touched by [selection] and remaps the selection, so the caret
 * stays on the same character. A selection ending at column 0 does not include that last line.
 */
private fun editLines(text: String, selection: TextRange, edits: (List<String>) -> List<LineEdit>): InsertResult {
    val start = selection.min.coerceIn(0, text.length)
    val rawEnd = selection.max.coerceIn(start, text.length)
    val end = if (rawEnd > start && text[rawEnd - 1] == '\n') rawEnd - 1 else rawEnd
    val blockStart = text.lastIndexOf('\n', start - 1) + 1
    val blockEnd = text.indexOf('\n', end).let { if (it == -1) text.length else it }
    val lines = text.substring(blockStart, blockEnd).split('\n')
    val lineEdits = edits(lines)

    val out = StringBuilder(text.length + 16).append(text, 0, blockStart)
    val oldStarts = IntArray(lines.size)
    val newStarts = IntArray(lines.size)
    var oldPos = blockStart
    lines.forEachIndexed { i, line ->
        if (i > 0) out.append('\n')
        oldStarts[i] = oldPos
        newStarts[i] = out.length
        val e = lineEdits[i]
        val offset = e.offset.coerceIn(0, line.length)
        val remove = e.removeLength.coerceIn(0, line.length - offset)
        out.append(line, 0, offset).append(e.insert).append(line, offset + remove, line.length)
        oldPos += line.length + 1
    }
    out.append(text, blockEnd, text.length)

    fun map(pos: Int, stickLeft: Boolean = false): Int {
        if (pos < blockStart) return pos
        if (pos > blockEnd) return pos + (out.length - text.length)
        val i = oldStarts.indexOfLast { it <= pos }
        return newStarts[i] + lineEdits[i].mapColumn(pos - oldStarts[i], stickLeft)
    }
    val newSelection = if (selection.collapsed) TextRange(map(start)) else TextRange(map(selection.min, stickLeft = true), map(selection.max))
    return InsertResult(out.toString(), newSelection)
}
