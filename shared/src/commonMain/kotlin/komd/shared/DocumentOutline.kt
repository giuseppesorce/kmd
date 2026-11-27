package komd.shared

/** A heading of the document, for the "Structure" panel. [offset] is where the heading line starts. */
internal data class OutlineEntry(val level: Int, val title: String, val line: Int, val offset: Int)

private val atxHeading = Regex("""^ {0,3}(#{1,6})[ \t]+(.*?)[ \t]*#*[ \t]*$""")
private val fence = Regex("""^ {0,3}(```|~~~)""")

/**
 * ATX headings (`#` … `######`) of [markdown], in order. Lines inside fenced code blocks are
 * skipped, so a `# comment` in a shell snippet is not mistaken for a heading.
 */
internal fun documentOutline(markdown: String): List<OutlineEntry> {
    val entries = mutableListOf<OutlineEntry>()
    var openFence: String? = null
    var offset = 0
    markdown.split('\n').forEachIndexed { index, rawLine ->
        val line = rawLine.removeSuffix("\r")
        val fenceMatch = fence.find(line)
        when {
            fenceMatch != null -> {
                val marker = fenceMatch.groupValues[1]
                openFence = when (openFence) {
                    null -> marker
                    marker -> null
                    else -> openFence
                }
            }
            openFence == null -> atxHeading.matchEntire(line)?.let { match ->
                val title = match.groupValues[2].trim()
                if (title.isNotEmpty()) entries += OutlineEntry(match.groupValues[1].length, title, index, offset)
            }
        }
        offset += rawLine.length + 1
    }
    return entries
}

/** 1-based line and column of [offset] in [text], for the status bar. */
internal fun caretPosition(text: String, offset: Int): Pair<Int, Int> {
    val safe = offset.coerceIn(0, text.length)
    var line = 1
    var lineStart = 0
    for (i in 0 until safe) {
        if (text[i] == '\n') {
            line++
            lineStart = i + 1
        }
    }
    return line to (safe - lineStart + 1)
}
