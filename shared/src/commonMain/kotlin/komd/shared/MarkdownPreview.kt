package komd.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay

private sealed interface MdInline {
    data class Text(val value: String) : MdInline
    data class Code(val value: String) : MdInline
    data class Bold(val children: List<MdInline>) : MdInline
    data class Italic(val children: List<MdInline>) : MdInline
    data class Strike(val children: List<MdInline>) : MdInline
    data class Link(val children: List<MdInline>, val url: String) : MdInline
    data class Image(val alt: String, val source: String) : MdInline
    data class Styled(val children: List<MdInline>, val look: InlineLook) : MdInline
}

private enum class InlineBaseline { None, Sup, Sub }

private data class InlineLook(
    val color: Color? = null,
    val background: Color? = null,
    val fontWeight: FontWeight? = null,
    val fontStyle: FontStyle? = null,
    val decoration: TextDecoration? = null,
    val baseline: InlineBaseline = InlineBaseline.None,
    val monospace: Boolean = false,
    val scale: Float = 1f,
    val pad: Boolean = false,
)

private sealed interface MdBlock {
    data class Heading(val level: Int, val inlines: List<MdInline>) : MdBlock
    data class Paragraph(val inlines: List<MdInline>) : MdBlock
    data class Quote(val blocks: List<MdBlock>) : MdBlock
    data class Bullets(val items: List<BulletItem>) : MdBlock
    data class Numbers(val items: List<Pair<Int, List<MdInline>>>) : MdBlock
    data class Code(val text: String) : MdBlock
    data class Table(val header: List<List<MdInline>>, val aligns: List<MdAlign>, val rows: List<List<List<MdInline>>>) : MdBlock
    data class Image(val image: PreviewImage) : MdBlock
    data object Rule : MdBlock
}

private enum class MdAlign { Left, Center, Right }

private data class BulletItem(val checked: Boolean?, val inlines: List<MdInline>)

private val headingPattern = Regex("""^(#{1,6})[ \t]+(.+)$""")
private val bulletPattern = Regex("""^[-*+][ \t]+(.+)$""")
private val taskPattern = Regex("""^\[([ xX])\]\s*(.*)$""")
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
                2 -> HeadingStyle(26.sp, 32.sp, 28.dp, 22.dp, FontWeight.Bold)
                3 -> HeadingStyle(20.sp, 26.sp, 22.dp, 18.dp, FontWeight.SemiBold)
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
        is MdBlock.Quote -> {
            val bar = if (colors.background.luminance() > 0.5f) Color(0xFFD0D5DD) else Color(0xFF5C6B7E)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).padding(top = 4.dp, bottom = 16.dp)) {
                Box(Modifier.width(3.dp).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(bar))
                Column(Modifier.weight(1f).padding(start = 16.dp)) {
                    block.blocks.forEach { child -> MarkdownBlock(child, imageUnsupported) }
                }
            }
        }
        is MdBlock.Bullets -> Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            block.items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.Top) {
                    if (item.checked == null) {
                        Text("•", Modifier.width(22.dp), color = colors.foreground, fontSize = 16.sp, lineHeight = 26.sp)
                    } else {
                        TaskMark(item.checked, Modifier.padding(top = 4.dp, end = 10.dp))
                    }
                    RichText(item.inlines, bodyStyle(colors), colors.foreground, Modifier.weight(1f))
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
        is MdBlock.Code -> CodeBlock(block.text)
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

@Composable
private fun TaskMark(checked: Boolean, modifier: Modifier = Modifier) {
    val fill = Color(0xFF2F80ED)
    val ring = Color(0xFFC5CAD3)
    Canvas(modifier.size(18.dp)) {
        if (checked) {
            drawCircle(fill)
            val mark = Path().apply {
                moveTo(size.width * 0.28f, size.height * 0.52f)
                lineTo(size.width * 0.44f, size.height * 0.68f)
                lineTo(size.width * 0.74f, size.height * 0.34f)
            }
            drawPath(
                mark,
                Color.White,
                style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        } else {
            drawCircle(ring, style = Stroke(width = 1.5.dp.toPx()))
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    val ui = LocalUiStrings.current
    val clipboard = LocalClipboardManager.current
    var copied by remember(code) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp)
            .clip(shape)
            .border(1.dp, Color(0xFFD5DBE3), shape)
            .background(Color(0xFFF2F4F8))
    ) {
        Text(
            highlightCode(code.ifEmpty { " " }),
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 18.dp, end = 88.dp, top = 16.dp, bottom = 16.dp),
            color = Color(0xFF1E3A5F),
            fontFamily = FontFamily.Monospace,
            fontSize = 13.5.sp,
            lineHeight = 22.sp
        )
        Text(
            if (copied) ui.copied else ui.copyCode,
            Modifier.align(Alignment.TopEnd).padding(8.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFD5DBE3), RoundedCornerShape(7.dp))
                .clickable {
                    clipboard.setText(AnnotatedString(code))
                    copied = true
                }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            color = Color(0xFF3B4A5A),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

private val codeKeyword = Color(0xFF3B5BDB)
private val codeDeclare = Color(0xFF7048E8)
private val codeReturn = Color(0xFFE03131)
private val codeString = Color(0xFF1C7ED6)
private val codeComment = Color(0xFF868E96)
private val codeNumber = Color(0xFFE8590C)

private val codeDeclarations = setOf("def", "class", "fun", "function", "fn", "func", "interface", "struct", "enum", "trait")
private val codeReturns = setOf("return", "yield", "throw", "raise")
private val codeWords = setOf(
    "if", "elif", "else", "for", "while", "in", "is", "and", "or", "not", "as", "with",
    "import", "from", "try", "except", "finally", "pass", "break", "continue", "lambda",
    "None", "True", "False", "val", "var", "when", "object", "package", "const", "let",
    "new", "this", "public", "private", "static", "void", "null", "undefined", "async",
    "await", "switch", "case", "default", "typeof", "instanceof", "mut", "impl", "pub",
    "use", "mod", "match", "loop", "where", "data", "sealed", "override", "open"
)

private fun highlightCode(code: String) = buildAnnotatedString {
    var index = 0
    while (index < code.length) {
        val char = code[index]
        when {
            char == '#' || (char == '/' && code.getOrNull(index + 1) == '/') -> {
                val end = code.indexOf('\n', index).let { if (it < 0) code.length else it }
                withStyle(SpanStyle(color = codeComment)) { append(code.substring(index, end)) }
                index = end
            }
            char == '"' || char == '\'' -> {
                val triple = code.startsWith("$char$char$char", index)
                val quote = if (triple) "$char$char$char" else char.toString()
                var cursor = index + quote.length
                while (cursor < code.length) {
                    if (code[cursor] == '\\') {
                        cursor += 2
                        continue
                    }
                    if (code.startsWith(quote, cursor)) {
                        cursor += quote.length
                        break
                    }
                    cursor++
                }
                withStyle(SpanStyle(color = codeString)) { append(code.substring(index, cursor.coerceAtMost(code.length))) }
                index = cursor
            }
            char.isDigit() -> {
                var cursor = index + 1
                while (cursor < code.length && (code[cursor].isDigit() || code[cursor] == '.')) cursor++
                withStyle(SpanStyle(color = codeNumber)) { append(code.substring(index, cursor)) }
                index = cursor
            }
            char.isLetter() || char == '_' -> {
                var cursor = index + 1
                while (cursor < code.length && (code[cursor].isLetterOrDigit() || code[cursor] == '_')) cursor++
                val word = code.substring(index, cursor)
                val color = when (word) {
                    in codeDeclarations -> codeDeclare
                    in codeReturns -> codeReturn
                    in codeWords -> codeKeyword
                    else -> null
                }
                if (color == null) append(word) else withStyle(SpanStyle(color = color)) { append(word) }
                index = cursor
            }
            else -> {
                append(char)
                index++
            }
        }
    }
}

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
            appendInlines(inlines, SpanStyle(color = color), colors, style.fontSize)
        }
    }
    Text(annotated, modifier, style = style, textAlign = textAlign)
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendInlines(
    inlines: List<MdInline>,
    extra: SpanStyle,
    colors: KomdPalette,
    fontSize: TextUnit,
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
                        fontSize = fontSize * 0.9f
                    )
                )
            ) {
                append("\u2009")
                append(item.value)
                append("\u2009")
            }
            is MdInline.Bold -> appendInlines(item.children, extra.merge(SpanStyle(fontWeight = FontWeight.Bold)), colors, fontSize)
            is MdInline.Italic -> appendInlines(item.children, extra.merge(SpanStyle(fontStyle = FontStyle.Italic)), colors, fontSize)
            is MdInline.Strike -> appendInlines(
                item.children,
                extra.merge(SpanStyle(textDecoration = TextDecoration.LineThrough)),
                colors,
                fontSize
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
                appendInlines(item.children, SpanStyle(), colors, fontSize)
            }
            is MdInline.Image -> withStyle(extra) { append(item.alt.ifEmpty { item.source }) }
            is MdInline.Styled -> appendStyled(item, extra, colors, fontSize)
        }
    }
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendStyled(
    item: MdInline.Styled,
    extra: SpanStyle,
    colors: KomdPalette,
    fontSize: TextUnit,
) {
    val look = item.look
    val size = if (look.scale != 1f) fontSize * look.scale else fontSize
    var span = extra
    look.color?.let { span = span.merge(SpanStyle(color = it)) }
    look.background?.let { span = span.merge(SpanStyle(background = it)) }
    look.fontWeight?.let { span = span.merge(SpanStyle(fontWeight = it)) }
    look.fontStyle?.let { span = span.merge(SpanStyle(fontStyle = it)) }
    if (look.monospace) span = span.merge(SpanStyle(fontFamily = FontFamily.Monospace, fontWeight = look.fontWeight ?: FontWeight.Medium))
    if (look.scale != 1f) span = span.merge(SpanStyle(fontSize = size))
    val decorations = listOfNotNull(extra.textDecoration, look.decoration)
    if (decorations.isNotEmpty()) span = span.merge(SpanStyle(textDecoration = TextDecoration.combine(decorations)))
    val shift = when (look.baseline) {
        InlineBaseline.Sup -> BaselineShift.Superscript
        InlineBaseline.Sub -> BaselineShift.Subscript
        InlineBaseline.None -> null
    }
    if (shift != null) span = span.merge(SpanStyle(baselineShift = shift))
    if (look.pad) {
        withStyle(span) {
            append("\u2009")
            appendInlines(item.children, span, colors, size)
            append("\u2009")
        }
    } else {
        appendInlines(item.children, span, colors, size)
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

private fun stripOneQuoteMarker(line: String): String {
    val trimmed = line.trimStart()
    if (!trimmed.startsWith(">")) return line
    val rest = trimmed.substring(1)
    return if (rest.startsWith(" ")) rest.substring(1) else rest
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
                quote += stripOneQuoteMarker(lines[index])
                index++
            }
            blocks += MdBlock.Quote(parseMarkdown(quote.joinToString("\n")))
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
            val items = mutableListOf<BulletItem>()
            while (index < lines.size && bulletPattern.matches(lines[index].trim())) {
                val first = bulletPattern.matchEntire(lines[index].trim())!!.groupValues[1]
                val task = taskPattern.matchEntire(first.trim())
                val body = StringBuilder(if (task != null) task.groupValues[2] else first)
                index++
                while (index < lines.size && isListContinuation(lines[index])) {
                    body.append('\n').append(lines[index].trim())
                    index++
                }
                val checked = if (task == null) null else task.groupValues[1].equals("x", ignoreCase = true)
                items += BulletItem(checked, parseInlines(joinLines(body.lines())))
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
            out += MdInline.Text(decodeBasicEntities(buffer.toString()))
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
        if (char == '<') {
            val html = tryParseHtml(input, index)
            if (html != null) {
                flush()
                out += html.inline
                index = html.next
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
            is MdInline.Styled -> append(plainText(item.children))
        }
    }
}

private data class ParsedHtml(val inline: MdInline, val next: Int)

private fun tryParseHtml(input: String, start: Int): ParsedHtml? {
    if (input.getOrNull(start) != '<') return null
    val next = input.getOrNull(start + 1) ?: return null
    if (next == '/' || next == '!' || next == '?') return null
    if (!next.isLetter()) return null
    var index = start + 1
    val nameStart = index
    while (index < input.length && (input[index].isLetterOrDigit() || input[index] == '-')) index++
    val name = input.substring(nameStart, index).lowercase()
    if (name.isEmpty()) return null
    val attrStart = index
    var quote: Char? = null
    while (index < input.length) {
        val char = input[index]
        if (quote != null) {
            if (char == quote) quote = null
            index++
            continue
        }
        if (char == '"' || char == '\'') {
            quote = char
            index++
            continue
        }
        if (char == '>') break
        index++
    }
    if (index >= input.length || input[index] != '>') return null
    val rawAttrs = input.substring(attrStart, index)
    val selfClosing = rawAttrs.trimEnd().endsWith('/')
    val openEnd = index + 1
    val attrs = parseHtmlAttributes(rawAttrs)
    if (selfClosing || name == "br" || name == "hr") {
        return ParsedHtml(if (name == "br") MdInline.Text("\n") else MdInline.Text(""), openEnd)
    }
    if (name == "img") {
        return ParsedHtml(MdInline.Image(attrs["alt"].orEmpty(), attrs["src"].orEmpty()), openEnd)
    }
    val closeAt = findClosingTag(input, openEnd, name) ?: return null
    val children = parseInlines(input.substring(openEnd, closeAt))
    return ParsedHtml(htmlInline(name, attrs, children), closeAt + name.length + 3)
}

private fun findClosingTag(input: String, start: Int, name: String): Int? {
    val openToken = "<$name"
    val closeToken = "</$name>"
    var depth = 1
    var index = start
    while (index < input.length) {
        if (input.startsWith(closeToken, index, ignoreCase = true)) {
            depth--
            if (depth == 0) return index
            index += closeToken.length
            continue
        }
        if (input.startsWith(openToken, index, ignoreCase = true)) {
            val boundary = input.getOrNull(index + openToken.length)
            if (boundary == null || boundary.isWhitespace() || boundary == '>' || boundary == '/') {
                depth++
                index += openToken.length
                continue
            }
        }
        index++
    }
    return null
}

private fun parseHtmlAttributes(raw: String): Map<String, String> {
    val source = raw.trim().removeSuffix("/").trim()
    val attrs = mutableMapOf<String, String>()
    var index = 0
    while (index < source.length) {
        while (index < source.length && source[index].isWhitespace()) index++
        if (index >= source.length) break
        val nameStart = index
        while (index < source.length && (source[index].isLetterOrDigit() || source[index] == '-' || source[index] == ':')) index++
        val name = source.substring(nameStart, index).lowercase()
        while (index < source.length && source[index].isWhitespace()) index++
        if (name.isEmpty()) {
            index++
            continue
        }
        if (index >= source.length || source[index] != '=') {
            attrs[name] = ""
            continue
        }
        index++
        while (index < source.length && source[index].isWhitespace()) index++
        if (index >= source.length) break
        val quote = source[index]
        if (quote == '"' || quote == '\'') {
            val end = source.indexOf(quote, index + 1)
            if (end < 0) break
            attrs[name] = decodeBasicEntities(source.substring(index + 1, end))
            index = end + 1
        } else {
            val end = source.indexOfAny(charArrayOf(' ', '\t', '\n'), index).let { if (it < 0) source.length else it }
            attrs[name] = decodeBasicEntities(source.substring(index, end))
            index = end
        }
    }
    return attrs
}

private fun htmlInline(name: String, attrs: Map<String, String>, children: List<MdInline>): MdInline {
    var look = cssLook(attrs["style"].orEmpty())
    attrs["color"]?.let { parseCssColor(it) }?.let { if (look.color == null) look = look.copy(color = it) }
    look = when (name) {
        "mark" -> look.copy(
            background = look.background ?: Color(0xFFFFF3A0),
            color = look.color ?: Color(0xFF2A2416)
        )
        "sup" -> look.copy(baseline = InlineBaseline.Sup, scale = if (look.scale == 1f) 0.72f else look.scale)
        "sub" -> look.copy(baseline = InlineBaseline.Sub, scale = if (look.scale == 1f) 0.72f else look.scale)
        "kbd" -> look.copy(
            background = look.background ?: Color(0xFFF3F4F6),
            color = look.color ?: Color(0xFF1F2933),
            monospace = true,
            scale = if (look.scale == 1f) 0.86f else look.scale,
            pad = true,
            fontWeight = look.fontWeight ?: FontWeight.Medium
        )
        "b", "strong" -> look.copy(fontWeight = FontWeight.Bold)
        "i", "em" -> look.copy(fontStyle = FontStyle.Italic)
        "u" -> look.copy(decoration = TextDecoration.Underline)
        "s", "del", "strike" -> look.copy(decoration = TextDecoration.LineThrough)
        "code" -> look.copy(monospace = true, pad = true, scale = if (look.scale == 1f) 0.9f else look.scale)
        else -> look
    }
    val bare = look.color == null && look.background == null && look.fontWeight == null && look.fontStyle == null &&
        look.decoration == null && look.baseline == InlineBaseline.None && !look.monospace && look.scale == 1f && !look.pad
    return if (bare) MdInline.Styled(children, InlineLook()) else MdInline.Styled(children, look)
}

private fun cssLook(style: String): InlineLook {
    if (style.isBlank()) return InlineLook()
    var look = InlineLook()
    style.split(';').forEach { declaration ->
        val parts = declaration.split(':', limit = 2)
        if (parts.size < 2) return@forEach
        val key = parts[0].trim().lowercase()
        val value = parts[1].substringBefore("!important").trim().lowercase()
        when (key) {
            "color" -> parseCssColor(value)?.let { look = look.copy(color = it) }
            "background", "background-color" -> parseCssColor(value)?.let { look = look.copy(background = it) }
            "font-weight" -> if (value == "bold" || value == "bolder" || (value.toIntOrNull() ?: 0) >= 600) {
                look = look.copy(fontWeight = FontWeight.Bold)
            }
            "font-style" -> if (value == "italic" || value == "oblique") look = look.copy(fontStyle = FontStyle.Italic)
            "text-decoration", "text-decoration-line" -> when {
                "underline" in value -> look = look.copy(decoration = TextDecoration.Underline)
                "line-through" in value -> look = look.copy(decoration = TextDecoration.LineThrough)
            }
        }
    }
    return look
}

private fun parseCssColor(raw: String): Color? {
    val value = raw.trim().lowercase().removeSuffix(";")
    if (value.isEmpty() || value == "inherit" || value == "currentcolor") return null
    cssNamedColors[value]?.let { return colorFromArgb(it) }
    if (value.startsWith("#")) return parseHexColor(value.drop(1))
    val rgb = Regex("""rgba?\(\s*([0-9.]+%?)\s*,\s*([0-9.]+%?)\s*,\s*([0-9.]+%?)(?:\s*,\s*([0-9.]+%?))?\s*\)""")
        .matchEntire(value) ?: return null
    val red = cssChannel(rgb.groupValues[1]) ?: return null
    val green = cssChannel(rgb.groupValues[2]) ?: return null
    val blue = cssChannel(rgb.groupValues[3]) ?: return null
    val alpha = rgb.groupValues[4].let { channel ->
        when {
            channel.isEmpty() -> 1f
            channel.endsWith("%") -> channel.dropLast(1).toFloatOrNull()?.div(100f)
            else -> channel.toFloatOrNull()
        }
    } ?: return null
    return Color(red, green, blue, alpha.coerceIn(0f, 1f))
}

private fun parseHexColor(hex: String): Color? {
    val expanded = when (hex.length) {
        3, 4 -> hex.map { "$it$it" }.joinToString("")
        6, 8 -> hex
        else -> return null
    }
    val argb = expanded.toLongOrNull(16) ?: return null
    return if (expanded.length == 6) colorFromArgb(0xFF000000 or argb) else colorFromArgb(argb)
}

private fun cssChannel(raw: String): Float? = when {
    raw.endsWith("%") -> raw.dropLast(1).toFloatOrNull()?.div(100f)
    else -> raw.toFloatOrNull()?.div(255f)
}

private fun colorFromArgb(argb: Long): Color {
    val value = argb.toInt()
    return Color(
        alpha = ((value ushr 24) and 0xFF) / 255f,
        red = ((value ushr 16) and 0xFF) / 255f,
        green = ((value ushr 8) and 0xFF) / 255f,
        blue = (value and 0xFF) / 255f,
    )
}

private val decimalEntity = Regex("""&#(\d+);""")
private val hexEntity = Regex("""&#x([0-9a-fA-F]+);""")

private fun decodeBasicEntities(text: String): String {
    if ('&' !in text) return text
    val named = text
        .replace("&nbsp;", "\u00A0")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&amp;", "&")
    val decimal = decimalEntity.replace(named) { match ->
        val code = match.groupValues[1].toIntOrNull()
        if (code != null && code in 0..0xFFFF) code.toChar().toString() else match.value
    }
    return hexEntity.replace(decimal) { match ->
        val code = match.groupValues[1].toIntOrNull(16)
        if (code != null && code in 0..0xFFFF) code.toChar().toString() else match.value
    }
}

private val cssNamedColors = mapOf(
    "black" to 0xFF000000L,
    "white" to 0xFFFFFFFFL,
    "red" to 0xFFFF0000L,
    "green" to 0xFF008000L,
    "lime" to 0xFF00FF00L,
    "blue" to 0xFF0000FFL,
    "yellow" to 0xFFFFFF00L,
    "orange" to 0xFFFFA500L,
    "purple" to 0xFF800080L,
    "gray" to 0xFF808080L,
    "grey" to 0xFF808080L,
    "maroon" to 0xFF800000L,
    "navy" to 0xFF000080L,
    "teal" to 0xFF008080L,
    "aqua" to 0xFF00FFFFL,
    "cyan" to 0xFF00FFFFL,
    "fuchsia" to 0xFFFF00FFL,
    "magenta" to 0xFFFF00FFL,
    "silver" to 0xFFC0C0C0L,
    "olive" to 0xFF808000L,
    "brown" to 0xFFA52A2AL,
    "pink" to 0xFFFFC0CBL,
    "gold" to 0xFFFFD700L,
    "coral" to 0xFFFF7F50L,
    "crimson" to 0xFFDC143CL,
    "tomato" to 0xFFFF6347L,
    "orangered" to 0xFFFF4500L,
    "darkred" to 0xFF8B0000L,
    "darkgreen" to 0xFF006400L,
    "forestgreen" to 0xFF228B22L,
    "seagreen" to 0xFF2E8B57L,
    "lightgreen" to 0xFF90EE90L,
    "darkblue" to 0xFF00008BL,
    "royalblue" to 0xFF4169E1L,
    "dodgerblue" to 0xFF1E90FFL,
    "violet" to 0xFFEE82EEL,
    "indigo" to 0xFF4B0082L,
    "lightgray" to 0xFFD3D3D3L,
    "lightgrey" to 0xFFD3D3D3L,
    "darkgray" to 0xFFA9A9A9L,
    "darkgrey" to 0xFFA9A9A9L,
    "whitesmoke" to 0xFFF5F5F5L,
)
