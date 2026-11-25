package komd.shared

/**
 * A file the user worked on (opened or saved), shown under "Welcome" in the sidebar.
 *
 * @param path filesystem path (or content URI on Android) used as identity and shown in the "missing file" dialog.
 * @param bookmark platform bookmark (FileKit `BookmarkData`, Base64): needed to reopen sandboxed files on
 *   iOS/macOS-sandbox and content URIs on Android across launches. Null where the path alone is enough.
 */
internal data class RecentFile(val path: String, val name: String, val bookmark: String? = null)

internal const val MAX_RECENT_FILES = 5

/** Most-recent-first list with [entry] on top, without duplicates (same path), capped at [MAX_RECENT_FILES]. */
internal fun List<RecentFile>.withRecent(entry: RecentFile): List<RecentFile> =
    (listOf(entry) + filterNot { it.path == entry.path }).take(MAX_RECENT_FILES)

internal fun List<RecentFile>.withoutRecent(path: String): List<RecentFile> = filterNot { it.path == path }

/**
 * Line-based format, one file per line: `path \t name \t bookmark`. Tabs, newlines and `%`
 * are percent-escaped so any path survives; unreadable lines are skipped instead of failing the whole list.
 */
internal fun encodeRecentFiles(files: List<RecentFile>): String =
    files.joinToString("\n") { file ->
        listOf(file.path, file.name, file.bookmark.orEmpty()).joinToString("\t") { escapeField(it) }
    }

internal fun decodeRecentFiles(text: String?): List<RecentFile> {
    if (text.isNullOrBlank()) return emptyList()
    return text.lines().mapNotNull { line ->
        val fields = line.split('\t')
        if (fields.size < 2) return@mapNotNull null
        val path = unescapeField(fields[0])
        if (path.isBlank()) return@mapNotNull null
        RecentFile(
            path = path,
            name = unescapeField(fields[1]).ifBlank { path.substringAfterLast('/').substringAfterLast('\\') },
            bookmark = fields.getOrNull(2)?.let(::unescapeField)?.ifEmpty { null }
        )
    }.distinctBy { it.path }.take(MAX_RECENT_FILES)
}

private fun escapeField(value: String): String = buildString {
    value.forEach { c ->
        when (c) {
            '%' -> append("%25")
            '\t' -> append("%09")
            '\n' -> append("%0A")
            '\r' -> append("%0D")
            else -> append(c)
        }
    }
}

private fun unescapeField(value: String): String = buildString {
    var i = 0
    while (i < value.length) {
        val code = if (value[i] == '%' && i + 3 <= value.length) value.substring(i + 1, i + 3) else null
        val decoded = when (code) {
            "25" -> '%'
            "09" -> '\t'
            "0A" -> '\n'
            "0D" -> '\r'
            else -> null
        }
        if (decoded != null) {
            append(decoded)
            i += 3
        } else {
            append(value[i])
            i++
        }
    }
}

/** Platform key-value storage for the recent files list (Preferences / SharedPreferences / NSUserDefaults). */
internal expect object RecentFilesStorage {
    fun load(): String?
    fun save(value: String)
}
