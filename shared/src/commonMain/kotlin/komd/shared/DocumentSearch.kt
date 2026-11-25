package komd.shared

/** Shortest query the in-document search accepts. */
internal const val MIN_SEARCH_LENGTH = 2

/**
 * Case-insensitive, non-overlapping occurrences of [query] in [text], as half-open
 * `[start, end)` ranges in source offsets. Empty when the query is shorter than [MIN_SEARCH_LENGTH].
 */
internal fun findMatches(text: String, query: String): List<IntRange> {
    if (query.length < MIN_SEARCH_LENGTH) return emptyList()
    val matches = mutableListOf<IntRange>()
    var from = 0
    while (from <= text.length - query.length) {
        val start = text.indexOf(query, from, ignoreCase = true)
        if (start < 0) break
        matches += start until start + query.length
        from = start + query.length
    }
    return matches
}

/** Index of the first match starting at or after [offset], wrapping to the first one; -1 when there are none. */
internal fun firstMatchFrom(matches: List<IntRange>, offset: Int): Int {
    if (matches.isEmpty()) return -1
    val index = matches.indexOfFirst { it.first >= offset }
    return if (index >= 0) index else 0
}

/** Next (or previous) match index with wrap-around; -1 when there are none. */
internal fun stepMatch(matches: List<IntRange>, current: Int, forward: Boolean = true): Int {
    if (matches.isEmpty()) return -1
    if (current !in matches.indices) return 0
    val step = if (forward) 1 else -1
    return (current + step + matches.size) % matches.size
}
