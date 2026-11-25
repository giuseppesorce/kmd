package komd.shared

/*
 * Toggle of a GFM task list item from the preview.
 *
 * The preview parser stores, for every task item, the index of its source line (see
 * [BulletItem.line]); a click on the checkbox calls [toggleTaskAt] with that index and the
 * editor text is replaced with the result. Only the single character inside the brackets
 * changes (' ' <-> 'x'), so the text keeps its length and the editor selection stays valid.
 */

/** Optional blockquote markers, indentation, bullet, then `[` + state + `]`. */
private val taskLinePattern = Regex("""^((?:[ \t]*>[ \t]?)*[ \t]*[-*+][ \t]+\[)([ xX])]""")

/**
 * Returns [markdown] with the task checkbox on line [line] (0-based, `\n`, `\r\n` or `\r`
 * separated, same numbering as the preview parser) flipped, or null when that line is not a task item.
 */
internal fun toggleTaskAt(markdown: String, line: Int): String? {
    if (line < 0) return null
    var start = 0
    var current = 0
    while (current < line) {
        val next = nextLineStart(markdown, start) ?: return null
        start = next
        current++
    }
    var end = start
    while (end < markdown.length && markdown[end] != '\n' && markdown[end] != '\r') end++
    val match = taskLinePattern.find(markdown.substring(start, end)) ?: return null
    val stateIndex = start + match.groups[1]!!.value.length
    val flipped = if (markdown[stateIndex] == ' ') 'x' else ' '
    return markdown.substring(0, stateIndex) + flipped + markdown.substring(stateIndex + 1)
}

private fun nextLineStart(text: String, from: Int): Int? {
    var i = from
    while (i < text.length) {
        when (text[i]) {
            '\n' -> return i + 1
            '\r' -> return if (i + 1 < text.length && text[i + 1] == '\n') i + 2 else i + 1
        }
        i++
    }
    return null
}
