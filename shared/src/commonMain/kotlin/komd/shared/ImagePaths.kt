package komd.shared

/*
 * Image paths relative to the Markdown file.
 *
 * Flow:
 *  - insert: the picked image becomes a path relative to the folder of the saved .md
 *    ([imageSourceForInsert]); an unsaved document keeps an absolute file:// URI.
 *  - "Save as" (also the first save): every local image source is re-expressed relative to the
 *    new folder ([rebaseImageSources]), so absolute URIs inserted before saving become relative and
 *    relative ones keep pointing at the same file when the document moves.
 *  - preview: relative sources are resolved against the document's folder ([resolveImageModel]).
 *
 * Only real filesystem paths are handled (POSIX `/…` and Windows `C:\…`): content:// URIs
 * (Android SAF), http(s) and data: sources are left untouched.
 */

/** An image farther than this many `../` hops from the document keeps its absolute file:// URI. */
internal const val MAX_PARENT_HOPS = 3

/** Absolute path split into a root (`""` on POSIX, `"C:"` on Windows) and normalised segments. */
private data class FsPath(val root: String, val segments: List<String>) {
    fun render(): String = root + "/" + segments.joinToString("/")
    fun resolve(relative: String): FsPath = FsPath(root, normalizeSegments(segments, relative.replace('\\', '/').split('/')))
}

private fun normalizeSegments(base: List<String>, parts: List<String>): List<String> {
    val out = base.toMutableList()
    parts.forEach { part ->
        when (part) {
            "", "." -> Unit
            ".." -> out.removeLastOrNull()
            else -> out += part
        }
    }
    return out
}

private fun parseAbsolutePath(path: String): FsPath? {
    val p = path.replace('\\', '/')
    return when {
        p.startsWith("/") -> FsPath("", normalizeSegments(emptyList(), p.split('/')))
        p.length >= 3 && p[0].isLetter() && p[1] == ':' && p[2] == '/' ->
            FsPath(p.substring(0, 2).uppercase(), normalizeSegments(emptyList(), p.substring(2).split('/')))
        else -> null
    }
}

/** Folder containing [filePath], or null when it is not a filesystem path (e.g. an Android content:// URI). */
internal fun parentDirectory(filePath: String): String? {
    val path = parseAbsolutePath(filePath) ?: return null
    if (path.segments.isEmpty()) return null
    return path.copy(segments = path.segments.dropLast(1)).render()
}

/**
 * Relative, percent-encoded path from [documentDir] to [imagePath], or null when the image is on
 * another drive or more than [MAX_PARENT_HOPS] folders up.
 */
internal fun relativeImagePath(documentDir: String, imagePath: String): String? {
    val dir = parseAbsolutePath(documentDir) ?: return null
    val image = parseAbsolutePath(imagePath) ?: return null
    if (dir.root != image.root || image.segments.isEmpty()) return null
    var common = 0
    while (common < dir.segments.size && common < image.segments.size - 1 && dir.segments[common] == image.segments[common]) common++
    val hops = dir.segments.size - common
    if (hops > MAX_PARENT_HOPS) return null
    return (List(hops) { ".." } + image.segments.drop(common).map(::percentEncodeSegment)).joinToString("/")
}

/** Source to write in the `<img>` tag for a freshly picked image. */
internal fun imageSourceForInsert(imagePath: String, documentDir: String?): String {
    val absolute = parseAbsolutePath(imagePath) ?: return imagePath
    return documentDir?.let { relativeImagePath(it, imagePath) } ?: absoluteFileUri(absolute)
}

/**
 * Model for the image loader: remote and file:// URIs as they are, absolute paths and paths
 * relative to [documentDir] as file:// URIs. Null when it cannot be resolved.
 */
internal fun resolveImageModel(source: String, documentDir: String?): String? = when {
    source.startsWith("https://") || source.startsWith("http://") || source.startsWith("file://") -> source
    else -> localImagePath(source, documentDir)?.let(::absoluteFileUri)
}

/**
 * Absolute filesystem path (`/a/b.png`, `C:/a/b.png`) of a local image source — relative to
 * [documentDir], absolute, or file:// — or null for remote, data:, content:// and unresolvable sources.
 */
internal fun localImageFilePath(source: String, documentDir: String?): String? =
    localImagePath(source, documentDir)?.render()

/**
 * Rewrites the local image sources of [markdown] (both `<img src>` and `![](…)`) for a document
 * that moves from [fromDir] (null: never saved) to [toDir]: relative when close enough, otherwise
 * absolute file:// URIs. Fenced code blocks are left alone.
 */
internal fun rebaseImageSources(markdown: String, fromDir: String?, toDir: String): String {
    if (parseAbsolutePath(toDir) == null) return markdown
    val rebase: (String) -> String = { source ->
        val local = localImagePath(source, fromDir)
        when {
            local == null -> source
            else -> relativeImagePath(toDir, local.render())
                ?: if (isRelativeSource(source)) absoluteFileUri(local) else source
        }
    }
    var fence: String? = null
    return markdown.split('\n').joinToString("\n") { line ->
        val trimmed = line.trimStart()
        val marker = fencePattern.find(trimmed)?.value
        when {
            fence != null -> {
                if (marker != null && marker[0] == fence!![0] && marker.length >= fence!!.length && trimmed.trimEnd() == marker) fence = null
                line
            }
            marker != null -> {
                fence = marker
                line
            }
            else -> rewriteImageSources(line, rebase)
        }
    }
}

private val fencePattern = Regex("""^(`{3,}|~{3,})""")
private val htmlImageSrcPattern = Regex("""(<img\b[^>]*?\bsrc\s*=\s*)(["'])(.*?)\2""", RegexOption.IGNORE_CASE)
private val markdownImageTargetPattern = Regex("""(!\[[^\]\n]*]\()(<[^>\n]*>|[^\s)]+)""")

private fun rewriteImageSources(line: String, rebase: (String) -> String): String {
    val html = htmlImageSrcPattern.replace(line) { match ->
        val (prefix, quote, value) = match.destructured
        val rebased = rebase(unescapeHtml(value))
        if (rebased == unescapeHtml(value)) match.value else prefix + quote + escapeHtml(rebased) + quote
    }
    return markdownImageTargetPattern.replace(html) { match ->
        val (prefix, target) = match.destructured
        val bracketed = target.startsWith("<")
        val source = if (bracketed) target.substring(1, target.length - 1) else target
        val rebased = rebase(source)
        if (rebased == source) match.value else prefix + (if (bracketed) "<$rebased>" else rebased)
    }
}

/** Absolute path of a local image source, or null for remote / non-file / unresolvable sources. */
private fun localImagePath(source: String, documentDir: String?): FsPath? {
    if (source.startsWith("file://")) {
        val path = percentDecode(source.removePrefix("file://").removePrefix("localhost"))
        // file:///C:/x → /C:/x
        return parseAbsolutePath(if (path.length >= 3 && path[0] == '/' && path[2] == ':') path.substring(1) else path)
    }
    parseAbsolutePath(source)?.let { return it }
    if (!isRelativeSource(source)) return null
    val base = documentDir?.let(::parseAbsolutePath) ?: return null
    return base.resolve(percentDecode(source.substringBefore('#').substringBefore('?')))
}

private val schemePattern = Regex("""^[A-Za-z][A-Za-z0-9+.-]*:""")

private fun isRelativeSource(source: String): Boolean =
    source.isNotBlank() && !source.startsWith("/") && !source.startsWith("\\") && !source.startsWith("#") &&
        parseAbsolutePath(source) == null && !schemePattern.containsMatchIn(source)

/** file:// URI of the folder [directory] with a trailing slash (a base URI for relative links), or null for non-filesystem paths. */
internal fun directoryFileUri(directory: String): String? =
    parseAbsolutePath(directory)?.let(::absoluteFileUri)?.let { if (it.endsWith("/")) it else "$it/" }

private fun absoluteFileUri(path: FsPath): String =
    // fileUri keeps '/' and ':', so Windows paths become file:///C:/…
    fileUri(if (path.root.isEmpty()) path.render() else "/" + path.render())

private const val UNRESERVED = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~!$&'()*+,;=@"
private const val HEX = "0123456789ABCDEF"

private fun percentEncodeSegment(segment: String): String = buildString {
    segment.encodeToByteArray().forEach { byte ->
        val value = byte.toInt() and 0xFF
        if (value < 128 && value.toChar() in UNRESERVED) append(value.toChar())
        else append('%').append(HEX[value shr 4]).append(HEX[value and 0x0F])
    }
}

internal fun percentDecode(value: String): String {
    if ('%' !in value) return value
    val bytes = ArrayList<Byte>(value.length)
    var i = 0
    while (i < value.length) {
        val c = value[i]
        val hi = value.getOrNull(i + 1)?.digitToIntOrNull(16)
        val lo = value.getOrNull(i + 2)?.digitToIntOrNull(16)
        if (c == '%' && hi != null && lo != null) {
            bytes += ((hi shl 4) or lo).toByte()
            i += 3
        } else {
            c.toString().encodeToByteArray().forEach { bytes += it }
            i++
        }
    }
    return bytes.toByteArray().decodeToString()
}

private fun unescapeHtml(value: String): String = value
    .replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")

private fun escapeHtml(value: String): String = value
    .replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;")
