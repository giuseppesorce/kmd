package komd.shared

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration

/*
 * Markdown → standalone HTML export.
 *
 * Flow:
 *  - the same parser as the preview ([parseMarkdown]) builds the block/inline tree, so the exported
 *    page shows exactly what the preview shows (tables, task lists, inline HTML styles, aligned images);
 *  - while rendering, every *local* image source (relative to the .md, absolute path or file://) is
 *    mapped to `images/<name>` and recorded in [HtmlExport.images]; remote, data: and content://
 *    sources are kept as they are;
 *  - the caller writes [HtmlExport.html] and, only when [HtmlExport.images] is not empty, copies
 *    each image into an `images/` folder next to the .html file.
 *
 * Everything here is pure (no I/O), so it is shared by all platforms and unit tested.
 */

/** Folder, next to the exported .html file, that receives the copied images. */
internal const val EXPORT_IMAGES_DIR = "images"

/** A local image to copy: absolute [sourcePath] → `images/[fileName]`. */
internal data class ExportedImage(val sourcePath: String, val fileName: String)

internal data class HtmlExport(val html: String, val images: List<ExportedImage>)

/**
 * Renders [markdown] as a complete HTML page titled [title]. [documentDir] is the folder of the
 * saved .md (null when unsaved), used to resolve relative image paths.
 */
internal fun buildHtmlExport(markdown: String, title: String, documentDir: String?): HtmlExport {
    val images = ExportImageNames(documentDir)
    val body = HtmlRenderer(images::sourceFor).blocks(parseMarkdown(markdown))
    return HtmlExport(htmlPage(title, body), images.collected)
}

/** Assigns each distinct local image a unique file name inside [EXPORT_IMAGES_DIR]. */
private class ExportImageNames(private val documentDir: String?) {
    private val byPath = LinkedHashMap<String, ExportedImage>()
    // Lower-cased: macOS and Windows filesystems are case-insensitive, `A.png` and `a.png` would collide.
    private val usedNames = mutableSetOf<String>()

    val collected: List<ExportedImage> get() = byPath.values.toList()

    fun sourceFor(source: String): String {
        val path = localImageFilePath(source, documentDir) ?: return source
        val image = byPath.getOrPut(path) { ExportedImage(path, uniqueName(path.substringAfterLast('/'))) }
        return "$EXPORT_IMAGES_DIR/${encodePathSegment(image.fileName)}"
    }

    private fun uniqueName(original: String): String {
        val name = original.ifBlank { "image" }
        val dot = name.lastIndexOf('.').takeIf { it > 0 }
        val stem = dot?.let { name.substring(0, it) } ?: name
        val extension = dot?.let { name.substring(it) } ?: ""
        var candidate = name
        var counter = 2
        while (!usedNames.add(candidate.lowercase())) candidate = "$stem-${counter++}$extension"
        return candidate
    }
}

private class HtmlRenderer(private val imageSource: (String) -> String) {

    fun blocks(blocks: List<MdBlock>): String = buildString { blocks.forEach { block(it) } }

    private fun StringBuilder.block(block: MdBlock) {
        when (block) {
            is MdBlock.Heading -> append("<h${block.level}>").append(inlines(block.inlines)).append("</h${block.level}>\n")
            is MdBlock.Paragraph -> append("<p>").append(inlines(block.inlines)).append("</p>\n")
            is MdBlock.Quote -> append("<blockquote>\n").append(blocks(block.blocks)).append("</blockquote>\n")
            is MdBlock.Bullets -> {
                val tasks = block.items.any { it.checked != null }
                append(if (tasks) "<ul class=\"tasks\">\n" else "<ul>\n")
                block.items.forEach { item ->
                    append("<li>")
                    if (item.checked != null) {
                        append("<input type=\"checkbox\" disabled").append(if (item.checked) " checked" else "").append("> ")
                    }
                    append(inlines(item.inlines)).append("</li>\n")
                }
                append("</ul>\n")
            }
            is MdBlock.Numbers -> {
                val start = block.items.firstOrNull()?.first ?: 1
                append(if (start != 1) "<ol start=\"$start\">\n" else "<ol>\n")
                block.items.forEachIndexed { index, (number, item) ->
                    // Markdown keeps the written numbers only for the first item; honour explicit jumps anyway.
                    val value = if (number != start + index) " value=\"$number\"" else ""
                    append("<li$value>").append(inlines(item)).append("</li>\n")
                }
                append("</ol>\n")
            }
            is MdBlock.Code -> append("<pre><code>").append(escapeText(block.text)).append("</code></pre>\n")
            is MdBlock.Table -> table(block)
            is MdBlock.Image -> {
                val image = block.image
                val align = when (image.alignment) {
                    ImageAlignment.Left -> "left"
                    ImageAlignment.Center -> "center"
                    ImageAlignment.Right -> "right"
                }
                val width = image.width?.let { " width=\"$it\"" } ?: ""
                append("<p class=\"image\" style=\"text-align:$align\"><img src=\"")
                    .append(escapeAttribute(imageSource(image.source))).append("\" alt=\"")
                    .append(escapeAttribute(image.alt)).append('"').append(width).append("></p>\n")
            }
            MdBlock.Rule -> append("<hr>\n")
        }
    }

    private fun StringBuilder.table(table: MdBlock.Table) {
        fun alignAttr(column: Int): String = when (table.aligns.getOrNull(column)) {
            MdAlign.Center -> " style=\"text-align:center\""
            MdAlign.Right -> " style=\"text-align:right\""
            else -> ""
        }
        append("<table>\n<thead>\n<tr>")
        table.header.forEachIndexed { column, cell -> append("<th${alignAttr(column)}>").append(inlines(cell)).append("</th>") }
        append("</tr>\n</thead>\n")
        if (table.rows.isNotEmpty()) {
            append("<tbody>\n")
            table.rows.forEach { row ->
                append("<tr>")
                // Same shape as the header, like GFM: missing cells are empty, extra cells dropped.
                table.header.indices.forEach { column ->
                    append("<td${alignAttr(column)}>").append(row.getOrNull(column)?.let(::inlines).orEmpty()).append("</td>")
                }
                append("</tr>\n")
            }
            append("</tbody>\n")
        }
        append("</table>\n")
    }

    private fun inlines(items: List<MdInline>): String = buildString {
        items.forEach { item ->
            when (item) {
                is MdInline.Text -> append(escapeText(item.value).replace("\n", "<br>\n"))
                is MdInline.Code -> append("<code>").append(escapeText(item.value)).append("</code>")
                is MdInline.Bold -> append("<strong>").append(inlines(item.children)).append("</strong>")
                is MdInline.Italic -> append("<em>").append(inlines(item.children)).append("</em>")
                is MdInline.Strike -> append("<del>").append(inlines(item.children)).append("</del>")
                is MdInline.Link -> append("<a href=\"").append(escapeAttribute(item.url)).append("\">")
                    .append(inlines(item.children)).append("</a>")
                is MdInline.Image -> append("<img src=\"").append(escapeAttribute(imageSource(item.source)))
                    .append("\" alt=\"").append(escapeAttribute(item.alt)).append("\">")
                is MdInline.Styled -> {
                    val css = cssOf(item.look)
                    if (css.isEmpty()) append(inlines(item.children))
                    else append("<span style=\"").append(escapeAttribute(css)).append("\">").append(inlines(item.children)).append("</span>")
                }
            }
        }
    }
}

/** Inline CSS equivalent of the preview's [InlineLook] (from `<mark>`, `<kbd>`, `<span style>`…). */
private fun cssOf(look: InlineLook): String = buildList {
    look.color?.let { add("color:${cssColor(it)}") }
    look.background?.let { add("background:${cssColor(it)}") }
    look.fontWeight?.let { add("font-weight:${it.weight}") }
    if (look.fontStyle == FontStyle.Italic) add("font-style:italic")
    when (look.decoration) {
        TextDecoration.Underline -> add("text-decoration:underline")
        TextDecoration.LineThrough -> add("text-decoration:line-through")
        else -> Unit
    }
    when (look.baseline) {
        InlineBaseline.Sup -> add("vertical-align:super")
        InlineBaseline.Sub -> add("vertical-align:sub")
        InlineBaseline.None -> Unit
    }
    if (look.monospace) add("font-family:var(--mono)")
    if (look.scale != 1f) add("font-size:${(look.scale * 100).toInt()}%")
    if (look.pad) add("padding:0 .3em;border-radius:4px")
}.joinToString(";")

private fun cssColor(color: Color): String {
    val argb = color.toArgb()
    val alpha = (argb ushr 24) and 0xFF
    val rgb = argb and 0xFFFFFF
    val hex = rgb.toString(16).padStart(6, '0')
    return if (alpha == 0xFF) "#$hex" else "#$hex" + alpha.toString(16).padStart(2, '0')
}

private fun escapeText(value: String): String = value
    .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

private fun escapeAttribute(value: String): String = escapeText(value).replace("\"", "&quot;")

private const val PATH_SAFE = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~!$&'()*+,;=@"

/** Percent-encodes a file name for use in a relative URL (spaces, accents, `#`, `?`…). */
private fun encodePathSegment(segment: String): String = buildString {
    segment.encodeToByteArray().forEach { byte ->
        val value = byte.toInt() and 0xFF
        if (value < 128 && value.toChar() in PATH_SAFE) append(value.toChar())
        else append('%').append("0123456789ABCDEF"[value shr 4]).append("0123456789ABCDEF"[value and 0x0F])
    }
}

private fun htmlPage(title: String, body: String): String = """<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="generator" content="KoMD">
<title>${escapeText(title)}</title>
<style>
:root { --fg:#1f2328; --muted:#59636e; --border:#d1d9e0; --code-bg:#f6f8fa; --link:#0969da; --bg:#ffffff;
  --mono: ui-monospace, SFMono-Regular, "SF Mono", Menlo, Consolas, monospace; color-scheme: light dark; }
@media (prefers-color-scheme: dark) {
  :root { --fg:#e6edf3; --muted:#9198a1; --border:#3d444d; --code-bg:#151b23; --link:#4493f8; --bg:#0d1117; }
}
body { margin:0; background:var(--bg); color:var(--fg);
  font: 16px/1.6 -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif; }
main { max-width: 820px; margin: 0 auto; padding: 40px 24px 64px; }
h1, h2, h3, h4, h5, h6 { line-height:1.25; margin:1.5em 0 .6em; }
h1 { font-size:2em; margin-top:0; } h2 { font-size:1.5em; border-bottom:1px solid var(--border); padding-bottom:.3em; }
h3 { font-size:1.25em; } h4, h5, h6 { font-size:1em; }
p, ul, ol, blockquote, pre, table { margin:0 0 1em; }
a { color:var(--link); }
img { max-width:100%; height:auto; }
p.image img { display:inline-block; }
blockquote { margin-left:0; padding:0 1em; color:var(--muted); border-left:.25em solid var(--border); }
code { font-family:var(--mono); font-size:.9em; background:var(--code-bg); padding:.15em .35em; border-radius:4px; }
pre { background:var(--code-bg); padding:14px 16px; border-radius:8px; overflow:auto; }
pre code { background:none; padding:0; font-size:.875em; }
table { border-collapse:collapse; display:block; overflow:auto; }
th, td { border:1px solid var(--border); padding:6px 13px; }
th { font-weight:600; background:var(--code-bg); }
hr { border:0; border-top:1px solid var(--border); margin:1.5em 0; }
ul.tasks { list-style:none; padding-left:.4em; }
ul.tasks input { margin:0 .4em 0 0; vertical-align:middle; }
</style>
</head>
<body>
<main>
$body</main>
</body>
</html>
"""
