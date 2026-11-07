package komd.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import coil3.compose.AsyncImage

private sealed interface MdInline {
    data class Text(val value: String) : MdInline
    data class Code(val value: String) : MdInline
    data class Bold(val children: List<MdInline>) : MdInline
    data class Italic(val children: List<MdInline>) : MdInline
    data class Strike(val children: List<MdInline>) : MdInline
    data class Link(val children: List<MdInline>, val url: String) : MdInline
    data class Image(val alt: String, val source: String) : MdInline
}

private sealed interface MdBlock {
    data class Heading(val level: Int, val inlines: List<MdInline>) : MdBlock
    data class Paragraph(val inlines: List<MdInline>) : MdBlock
    data class Quote(val inlines: List<MdInline>) : MdBlock
    data class Bullets(val items: List<List<MdInline>>) : MdBlock
    data class Numbers(val items: List<Pair<Int, List<MdInline>>>) : MdBlock
    data class Code(val text: String) : MdBlock
    data class Table(val header: List<List<MdInline>>, val aligns: List<MdAlign>, val rows: List<List<List<MdInline>>>) : MdBlock
    data class Image(val image: PreviewImage) : MdBlock
    data object Rule : MdBlock
}

private enum class MdAlign { Left, Center, Right }

private val headingPattern = Regex("""^(#{1,6})[ \t]+(.+)$""")
private val bulletPattern = Regex("""^[-*+][ \t]+(.+)$""")
private val orderedPattern = Regex("""^(\d+)\.[ \t]+(.+)$""")
private val thematicPattern = Regex("""^(?:(?:\*[ \t]*){3,}|(?:-[ \t]*){3,}|(?:_[ \t]*){3,})$""")

@Composable
internal fun MarkdownDocument(markdown: String, imageUnsupported: String) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        blocks.forEach { block -> MarkdownBlock(block, imageUnsupported) }
    }
}

@Composable
private fun MarkdownBlock(block: MdBlock, imageUnsupported: String) {
    val colors = LocalKomdPalette.current
    when (block) {
        is MdBlock.Heading -> {
            val (size, line, top, bottom, weight) = when (block.level) {
                1 -> HeadingStyle(36.sp, 42.sp, 2.dp, 12.dp, FontWeight.Bold)
                2 -> HeadingStyle(26.sp, 32.sp, 28.dp, 10.dp, FontWeight.Bold)
                3 -> HeadingStyle(20.sp, 26.sp, 22.dp, 8.dp, FontWeight.SemiBold)
                else -> HeadingStyle(17.sp, 24.sp, 18.dp, 6.dp, FontWeight.SemiBold)
            }
            RichText(
                block.inlines,
                TextStyle(color = colors.foreground, fontSize = size, lineHeight = line, fontWeight = weight),
                colors.foreground,
                Modifier.fillMaxWidth().padding(top = top, bottom = bottom)
            )
        }
        is MdBlock.Paragraph -> RichText(
            block.inlines,
            bodyStyle(colors),
            colors.foreground,
            Modifier.fillMaxWidth().padding(bottom = 14.dp)
        )
        is MdBlock.Quote -> Row(
            Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(top = 2.dp, bottom = 16.dp)
        ) {
            Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(colors.accent))
            RichText(
                block.inlines,
                bodyStyle(colors).copy(fontStyle = FontStyle.Italic, color = colors.foreground),
                colors.foreground,
                Modifier.weight(1f).padding(start = 14.dp)
            )
        }
        is MdBlock.Bullets -> Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            block.items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.Top) {
                    Text("•", Modifier.width(22.dp), color = colors.foreground, fontSize = 16.sp, lineHeight = 26.sp)
                    RichText(item, bodyStyle(colors), colors.foreground, Modifier.weight(1f))
                }
            }
        }
        is MdBlock.Numbers -> Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            block.items.forEach { (number, item) ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.Top) {
                    Text(
                        "$number.",
                        Modifier.width(28.dp),
                        color = colors.foreground,
                        fontSize = 16.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.Medium
                    )
                    RichText(item, bodyStyle(colors), colors.foreground, Modifier.weight(1f))
                }
            }
        }
        is MdBlock.Code -> Text(
            block.text.ifEmpty { " " },
            Modifier.fillMaxWidth().padding(bottom = 16.dp).clip(RoundedCornerShape(8.dp)).background(colors.toolbar)
                .horizontalScroll(rememberScrollState()).padding(horizontal = 14.dp, vertical = 12.dp),
            color = colors.editorText,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )
        is MdBlock.Table -> MarkdownTable(block)
        is MdBlock.Image -> MarkdownImage(block.image, imageUnsupported)
        MdBlock.Rule -> Box(
            Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 18.dp).height(1.dp).background(colors.divider)
        )
    }
}

private data class HeadingStyle(
    val size: androidx.compose.ui.unit.TextUnit,
    val line: androidx.compose.ui.unit.TextUnit,
    val top: Dp,
    val bottom: Dp,
    val weight: FontWeight,
)

private fun bodyStyle(colors: KomdPalette) = TextStyle(
    color = colors.foreground,
    fontSize = 16.5.sp,
    lineHeight = 27.sp,
    fontWeight = FontWeight.Normal
)

@Composable
private fun MarkdownTable(table: MdBlock.Table) {
    val colors = LocalKomdPalette.current
    val light = colors.background.luminance() > 0.5f
    val headerBg = if (light) Color(0xFFF3F4F6) else Color(0xFF12263C)
    val grid = if (light) Color(0xFFE4E6EA) else colors.divider
    val cols = table.header.size.coerceAtLeast(1)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        val minCol = 152.dp
        val natural = minCol * cols
        val target = if (maxWidth != Dp.Infinity && maxWidth >= natural) maxWidth else natural
        val colWidth = target / cols
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
            Column(Modifier.width(colWidth * cols).border(1.dp, grid)) {
                TableRow(table.header, table.aligns, colWidth, header = true, headerBg, grid, colors)
                table.rows.forEach { row ->
                    Box(Modifier.fillMaxWidth().height(1.dp).background(grid))
                    val padded = List(cols) { index -> row.getOrElse(index) { emptyList() } }
                    TableRow(padded, table.aligns, colWidth, header = false, Color.Transparent, grid, colors)
                }
            }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<List<MdInline>>,
    aligns: List<MdAlign>,
    colWidth: Dp,
    header: Boolean,
    background: Color,
    grid: Color,
    colors: KomdPalette,
) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).background(background)) {
        cells.forEachIndexed { index, cell ->
            if (index > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(grid))
            val align = aligns.getOrElse(index) { MdAlign.Left }
            val textAlign = if (header) TextAlign.Center else when (align) {
                MdAlign.Left -> TextAlign.Start
                MdAlign.Center -> TextAlign.Center
                MdAlign.Right -> TextAlign.End
            }
            Box(
                Modifier.width(colWidth).padding(horizontal = 12.dp, vertical = 12.dp),
                contentAlignment = if (header || align == MdAlign.Center) Alignment.Center else Alignment.CenterStart
            ) {
                RichText(
                    cell,
                    TextStyle(
                        color = colors.foreground,
                        fontSize = 14.5.sp,
                        lineHeight = 21.sp,
                        fontWeight = if (header) FontWeight.Bold else FontWeight.Normal,
                        textAlign = textAlign
                    ),
                    colors.foreground,
                    Modifier.fillMaxWidth(),
                    textAlign
                )
            }
        }
    }
}

@Composable
private fun MarkdownImage(image: PreviewImage, imageUnsupported: String) {
    val colors = LocalKomdPalette.current
    val model = when {
        image.source.startsWith("/") -> fileUri(image.source)
        image.source.startsWith("file://") || image.source.startsWith("https://") || image.source.startsWith("http://") -> image.source
        else -> null
    }
    if (model == null) {
        Text(
            imageUnsupported.replace("{source}", image.source),
            Modifier.padding(bottom = 12.dp),
            color = colors.secondary,
            fontSize = 13.sp
        )
        return
    }
    val alignment = when (image.alignment) {
        ImageAlignment.Left -> Alignment.CenterStart
        ImageAlignment.Center -> Alignment.Center
        ImageAlignment.Right -> Alignment.CenterEnd
    }
    Box(Modifier.fillMaxWidth().padding(bottom = 14.dp), contentAlignment = alignment) {
        AsyncImage(
            model = model,
            contentDescription = image.alt,
            contentScale = ContentScale.Fit,
            modifier = Modifier.widthIn(max = 640.dp).heightIn(max = 420.dp)
        )
    }
}

private sealed interface RichSegment {
    data class TextRun(val inlines: List<MdInline>) : RichSegment
    data class ImageRun(val alt: String, val source: String) : RichSegment
}

@Composable
private fun RichText(
    inlines: List<MdInline>,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    val segments = remember(inlines) { splitByImages(inlines) }
    if (segments.size == 1 && segments.first() is RichSegment.TextRun) {
        InlineText((segments.first() as RichSegment.TextRun).inlines, style, color, modifier, textAlign)
        return
    }
    Column(modifier) {
        segments.forEach { segment ->
            when (segment) {
                is RichSegment.TextRun -> InlineText(segment.inlines, style, color, Modifier.fillMaxWidth(), textAlign)
                is RichSegment.ImageRun -> MarkdownImage(
                    PreviewImage(segment.alt, segment.source, ImageAlignment.Left),
                    segment.source
                )
            }
        }
    }
}

@Composable
private fun InlineText(
    inlines: List<MdInline>,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start,
) {
    val colors = LocalKomdPalette.current
    val annotated = remember(inlines, color, colors.accent, colors.button, style.fontSize) {
        buildAnnotatedString {
            appendInlines(inlines, SpanStyle(color = color), colors, style)
        }
    }
    Text(annotated, modifier, style = style, textAlign = textAlign)
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlines(
    inlines: List<MdInline>,
    extra: SpanStyle,
    colors: KomdPalette,
    style: TextStyle,
) {
    inlines.forEach { item ->
        when (item) {
            is MdInline.Text -> withStyle(extra) { append(item.value) }
            is MdInline.Code -> withStyle(
                extra.merge(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Normal,
                        fontStyle = FontStyle.Normal,
                        background = colors.button,
                        fontSize = style.fontSize * 0.9f
                    )
                )
            ) {
                append("\u2009")
                append(item.value)
                append("\u2009")
            }
            is MdInline.Bold -> appendInlines(item.children, extra.merge(SpanStyle(fontWeight = FontWeight.Bold)), colors, style)
            is MdInline.Italic -> appendInlines(item.children, extra.merge(SpanStyle(fontStyle = FontStyle.Italic)), colors, style)
            is MdInline.Strike -> appendInlines(
                item.children,
                extra.merge(SpanStyle(textDecoration = TextDecoration.LineThrough)),
                colors,
                style
            )
            is MdInline.Link -> withLink(
                LinkAnnotation.Url(
                    item.url,
                    TextLinkStyles(
                        style = extra.merge(
                            SpanStyle(color = colors.accent, textDecoration = TextDecoration.Underline)
                        )
                    )
                )
            ) {
                appendInlines(item.children, SpanStyle(), colors, style)
            }
            is MdInline.Image -> withStyle(extra) { append(item.alt.ifEmpty { item.source }) }
        }
    }
}

private fun splitByImages(inlines: List<MdInline>): List<RichSegment> {
    val segments = mutableListOf<RichSegment>()
    val buffer = mutableListOf<MdInline>()
    fun flush() {
        if (buffer.isNotEmpty()) {
            segments += RichSegment.TextRun(buffer.toList())
            buffer.clear()
        }
    }
    inlines.forEach { item ->
        if (item is MdInline.Image) {
            flush()
            segments += RichSegment.ImageRun(item.alt, item.source)
        } else buffer += item
    }
    flush()
    return segments
}

private fun parseMarkdown(markdown: String): List<MdBlock> {
    val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').lines()
    val blocks = mutableListOf<MdBlock>()
    var index = 0
    while (index < lines.size) {
        val line = lines[index]
        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            index++
            continue
        }
        if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
            val fence = trimmed.take(3)
            val code = mutableListOf<String>()
            index++
            while (index < lines.size && !lines[index].trimStart().startsWith(fence)) {
                code += lines[index]
                index++
            }
            if (index < lines.size) index++
            blocks += MdBlock.Code(code.joinToString("\n"))
            continue
        }
        if (thematicPattern.matches(trimmed)) {
            blocks += MdBlock.Rule
            index++
            continue
        }
        val heading = headingPattern.matchEntire(trimmed)
        if (heading != null) {
            val text = heading.groupValues[2].trim().trimEnd('#').trim()
            blocks += MdBlock.Heading(heading.groupValues[1].length, parseInlines(text))
            index++
            continue
        }
        val image = parsePreviewImage(line)
        if (image != null) {
            blocks += MdBlock.Image(image)
            index++
            continue
        }
        if (trimmed.startsWith(">")) {
            val quote = mutableListOf<String>()
            while (index < lines.size && lines[index].trimStart().startsWith(">")) {
                quote += lines[index].trim().removePrefix(">").trimStart()
                index++
            }
            blocks += MdBlock.Quote(parseInlines(joinLines(quote)))
            continue
        }
        if (index + 1 < lines.size && looksLikeTableRow(trimmed) && isTableSeparator(lines[index + 1].trim())) {
            val header = splitRow(trimmed)
            val aligns = splitRow(lines[index + 1].trim()).map { alignmentOf(it) }
            index += 2
            val rows = mutableListOf<List<List<MdInline>>>()
            while (index < lines.size && lines[index].isNotBlank() && looksLikeTableRow(lines[index].trim())) {
                rows += splitRow(lines[index].trim()).map { parseInlines(it) }
                index++
            }
            blocks += MdBlock.Table(header.map { parseInlines(it) }, aligns, rows)
            continue
        }
        if (bulletPattern.matches(trimmed)) {
            val items = mutableListOf<List<MdInline>>()
            while (index < lines.size && bulletPattern.matches(lines[index].trim())) {
                val body = StringBuilder(bulletPattern.matchEntire(lines[index].trim())!!.groupValues[1])
                index++
                while (index < lines.size && isListContinuation(lines[index])) {
                    body.append('\n').append(lines[index].trim())
                    index++
                }
                items += parseInlines(joinLines(body.lines()))
            }
            blocks += MdBlock.Bullets(items)
            continue
        }
        if (orderedPattern.matches(trimmed)) {
            val items = mutableListOf<Pair<Int, List<MdInline>>>()
            while (index < lines.size && orderedPattern.matches(lines[index].trim())) {
                val match = orderedPattern.matchEntire(lines[index].trim())!!
                val body = StringBuilder(match.groupValues[2])
                index++
                while (index < lines.size && isListContinuation(lines[index])) {
                    body.append('\n').append(lines[index].trim())
                    index++
                }
                items += match.groupValues[1].toInt() to parseInlines(joinLines(body.lines()))
            }
            blocks += MdBlock.Numbers(items)
            continue
        }
        val paragraph = mutableListOf(trimmed)
        index++
        while (index < lines.size && lines[index].isNotBlank() && !isStructural(lines, index)) {
            paragraph += lines[index].trim()
            index++
        }
        blocks += MdBlock.Paragraph(parseInlines(joinLines(paragraph)))
    }
    return blocks
}

private fun joinLines(lines: List<String>): String = buildString {
    lines.forEachIndexed { index, line ->
        if (index > 0) append(if (lines[index - 1].endsWith("  ") || lines[index - 1].endsWith("\\")) "\n" else " ")
        append(line.trimEnd('\\').trimEnd())
    }
}

private fun isListContinuation(line: String): Boolean {
    if (line.isBlank()) return false
    if (!line.startsWith(" ") && !line.startsWith("\t")) return false
    val trimmed = line.trim()
    return !isStructural(listOf(trimmed), 0) || looksLikeTableRow(trimmed).not()
}

private fun isStructural(lines: List<String>, index: Int): Boolean {
    val trimmed = lines[index].trim()
    if (trimmed.isEmpty()) return true
    if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) return true
    if (trimmed.startsWith(">")) return true
    if (thematicPattern.matches(trimmed)) return true
    if (headingPattern.matches(trimmed)) return true
    if (bulletPattern.matches(trimmed)) return true
    if (orderedPattern.matches(trimmed)) return true
    if (parsePreviewImage(lines[index]) != null) return true
    return index + 1 < lines.size && looksLikeTableRow(trimmed) && isTableSeparator(lines[index + 1].trim())
}

private fun looksLikeTableRow(line: String): Boolean = line.contains('|')

private fun isTableSeparator(line: String): Boolean {
    if (!line.contains('-') || !line.contains('|')) return false
    val cells = splitRow(line)
    if (cells.isEmpty()) return false
    return cells.all { it.matches(Regex(""":?-+:?""")) }
}

private fun splitRow(line: String): List<String> {
    var value = line.trim()
    if (value.startsWith("|")) value = value.drop(1)
    if (value.endsWith("|")) value = value.dropLast(1)
    val cells = mutableListOf<String>()
    val cell = StringBuilder()
    var escaped = false
    value.forEach { char ->
        when {
            escaped -> {
                cell.append(char)
                escaped = false
            }
            char == '\\' -> escaped = true
            char == '|' -> {
                cells += cell.toString().trim()
                cell.clear()
            }
            else -> cell.append(char)
        }
    }
    cells += cell.toString().trim()
    return cells
}

private fun alignmentOf(cell: String): MdAlign = when {
    cell.startsWith(":") && cell.endsWith(":") -> MdAlign.Center
    cell.endsWith(":") -> MdAlign.Right
    else -> MdAlign.Left
}

private fun parseInlines(input: String): List<MdInline> {
    val out = mutableListOf<MdInline>()
    val buffer = StringBuilder()
    fun flush() {
        if (buffer.isNotEmpty()) {
            out += MdInline.Text(buffer.toString())
            buffer.clear()
        }
    }
    var index = 0
    while (index < input.length) {
        val char = input[index]
        if (char == '\\' && index + 1 < input.length) {
            buffer.append(input[index + 1])
            index += 2
            continue
        }
        if (char == '`') {
            val end = input.indexOf('`', index + 1)
            if (end > index) {
                flush()
                out += MdInline.Code(input.substring(index + 1, end))
                index = end + 1
                continue
            }
        }
        if (input.startsWith("~~", index)) {
            val end = input.indexOf("~~", index + 2)
            if (end > index) {
                flush()
                out += MdInline.Strike(parseInlines(input.substring(index + 2, end)))
                index = end + 2
                continue
            }
        }
        if (input.startsWith("**", index) || input.startsWith("__", index)) {
            val marker = input.substring(index, index + 2)
            val end = indexOfClosing(input, index + 2, marker)
            if (end > index) {
                flush()
                out += MdInline.Bold(parseInlines(input.substring(index + 2, end)))
                index = end + marker.length
                continue
            }
        }
        if ((char == '*' || char == '_') && canOpenEmphasis(input, index, char)) {
            val end = findClosingEmphasis(input, index + 1, char)
            if (end > index) {
                flush()
                out += MdInline.Italic(parseInlines(input.substring(index + 1, end)))
                index = end + 1
                continue
            }
        }
        if (char == '!' && input.getOrNull(index + 1) == '[') {
            val link = tryParseLink(input, index + 1)
            if (link != null && link.inline is MdInline.Link) {
                flush()
                out += MdInline.Image(plainText(link.inline.children), link.inline.url)
                index = link.next
                continue
            }
        }
        if (char == '[') {
            val link = tryParseLink(input, index)
            if (link != null) {
                flush()
                out += link.inline
                index = link.next
                continue
            }
        }
        buffer.append(char)
        index++
    }
    flush()
    return out
}

private data class ParsedLink(val inline: MdInline, val next: Int)

private fun tryParseLink(input: String, start: Int): ParsedLink? {
    if (input.getOrNull(start) != '[') return null
    val labelEnd = indexOfUnescaped(input, ']', start + 1)
    if (labelEnd < 0 || input.getOrNull(labelEnd + 1) != '(') return null
    val urlStart = labelEnd + 2
    val closing: Int
    val rawUrl: String
    if (input.getOrNull(urlStart) == '<') {
        val end = input.indexOf('>', urlStart + 1)
        if (end < 0 || input.getOrNull(end + 1) != ')') return null
        rawUrl = input.substring(urlStart + 1, end)
        closing = end + 1
    } else {
        val end = indexOfUnescaped(input, ')', urlStart)
        if (end < 0) return null
        rawUrl = input.substring(urlStart, end).trim()
        closing = end
    }
    return ParsedLink(MdInline.Link(parseInlines(input.substring(start + 1, labelEnd)), rawUrl), closing + 1)
}

private fun indexOfUnescaped(input: String, target: Char, start: Int): Int {
    var index = start
    while (index < input.length) {
        if (input[index] == '\\') {
            index += 2
            continue
        }
        if (input[index] == target) return index
        index++
    }
    return -1
}

private fun indexOfClosing(input: String, start: Int, marker: String): Int {
    var index = start
    while (index < input.length) {
        if (input[index] == '\\') {
            index += 2
            continue
        }
        if (input.startsWith(marker, index)) return index
        index++
    }
    return -1
}

private fun canOpenEmphasis(input: String, index: Int, marker: Char): Boolean {
    val next = input.getOrNull(index + 1) ?: return false
    if (next.isWhitespace() || next == marker) return false
    if (marker == '_' && input.getOrNull(index - 1)?.isLetterOrDigit() == true) return false
    return true
}

private fun findClosingEmphasis(input: String, start: Int, marker: Char): Int {
    var index = start
    while (index < input.length) {
        if (input[index] == '\\') {
            index += 2
            continue
        }
        if (input[index] == marker) {
            if (marker == '*' && input.getOrNull(index + 1) == '*') {
                index += 2
                continue
            }
            if (input.getOrNull(index - 1)?.isWhitespace() == true) {
                index++
                continue
            }
            if (marker == '_' && input.getOrNull(index + 1)?.isLetterOrDigit() == true) {
                index++
                continue
            }
            return index
        }
        index++
    }
    return -1
}

private fun plainText(inlines: List<MdInline>): String = buildString {
    inlines.forEach { item ->
        when (item) {
            is MdInline.Text -> append(item.value)
            is MdInline.Code -> append(item.value)
            is MdInline.Bold -> append(plainText(item.children))
            is MdInline.Italic -> append(plainText(item.children))
            is MdInline.Strike -> append(plainText(item.children))
            is MdInline.Link -> append(plainText(item.children))
            is MdInline.Image -> append(item.alt)
        }
    }
}
