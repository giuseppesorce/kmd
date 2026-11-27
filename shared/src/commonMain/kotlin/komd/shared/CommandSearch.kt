package komd.shared

/**
 * Fuzzy match used by the command palette and the tab filter.
 *
 * Returns null when [query] is not a subsequence of [text] (case- and accent-insensitive),
 * otherwise a score where higher is better: contiguous runs, matches at word starts and a
 * match at the very beginning are rewarded, so "sal" ranks "Salva" above "Esporta HTML…".
 */
internal fun commandMatchScore(text: String, query: String): Int? {
    val needle = foldForSearch(query.trim())
    if (needle.isEmpty()) return 0
    val haystack = foldForSearch(text)
    var score = 0
    var from = 0
    var previous = -2
    for (char in needle) {
        if (char == ' ') continue
        val index = haystack.indexOf(char, from)
        if (index < 0) return null
        score += 1
        if (index == previous + 1) score += 4
        if (index == 0) score += 6
        else if (!haystack[index - 1].isLetterOrDigit()) score += 3
        score -= (index - from).coerceAtMost(6)
        previous = index
        from = index + 1
    }
    return score
}

/** Items of [items] matching [query], best first; ties keep the original order. */
internal fun <T> rankByQuery(items: List<T>, query: String, text: (T) -> String): List<T> {
    if (query.isBlank()) return items
    return items.mapIndexedNotNull { index, item -> commandMatchScore(text(item), query)?.let { Triple(item, it, index) } }
        .sortedWith(compareByDescending<Triple<T, Int, Int>> { it.second }.thenBy { it.third })
        .map { it.first }
}

private fun foldForSearch(value: String): String = buildString(value.length) {
    value.lowercase().forEach { char ->
        append(
            when (char) {
                'à', 'á', 'â', 'ä', 'ã' -> 'a'
                'è', 'é', 'ê', 'ë' -> 'e'
                'ì', 'í', 'î', 'ï' -> 'i'
                'ò', 'ó', 'ô', 'ö', 'õ' -> 'o'
                'ù', 'ú', 'û', 'ü' -> 'u'
                'ç' -> 'c'
                'ñ' -> 'n'
                'ß' -> 's'
                else -> char
            }
        )
    }
}
