package komd.shared

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** What the search field shows on its right: the n/m counter, "no results" or the minimum-length hint. */
internal sealed interface SearchStatus {
    data object Idle : SearchStatus
    data object TooShort : SearchStatus
    data object NoMatches : SearchStatus
    data class Found(val current: Int, val total: Int) : SearchStatus
}

/** Magnifier drawn on a canvas (no icon library in the project). */
@Composable
internal fun SearchGlyph(color: Color, size: Dp = 14.dp) {
    Canvas(Modifier.size(size)) {
        val stroke = this.size.minDimension * 0.13f
        val radius = this.size.minDimension * 0.31f
        val center = Offset(radius + stroke, radius + stroke)
        drawCircle(color, radius, center, style = Stroke(stroke))
        val edge = radius * 0.7071f
        drawLine(
            color, Offset(center.x + edge, center.y + edge),
            Offset(this.size.width - stroke / 2, this.size.height - stroke / 2), stroke, StrokeCap.Round
        )
    }
}

@Composable
private fun ArrowGlyph(color: Color, size: Dp = 12.dp) {
    Canvas(Modifier.size(size)) {
        val stroke = this.size.minDimension * 0.14f
        val w = this.size.width
        val midY = this.size.height / 2
        drawLine(color, Offset(stroke, midY), Offset(w - stroke, midY), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.58f, midY - w * 0.34f), Offset(w - stroke, midY), stroke, StrokeCap.Round)
        drawLine(color, Offset(w * 0.58f, midY + w * 0.34f), Offset(w - stroke, midY), stroke, StrokeCap.Round)
    }
}

/** Toolbar button that opens the search field; highlighted while the field is open. */
@Composable
internal fun SearchToggleButton(active: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = KomdTheme.colors
    Box(
        modifier.clip(RoundedCornerShape(7.dp))
            .background(if (active) colors.accentSoft else colors.hover)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label }
            .padding(horizontal = 7.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        SearchGlyph(if (active) colors.accent else colors.foreground)
    }
}

/**
 * Single-line search field with the arrow button at the end.
 * Enter or the arrow search forward, Shift+Enter backward, Esc closes.
 * [focusRequests] is bumped by the caller (lens tap, Cmd/Ctrl+F) to move the focus here.
 */
@Composable
internal fun DocumentSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: (forward: Boolean) -> Unit,
    onClose: () -> Unit,
    status: SearchStatus,
    focusRequests: Int,
    modifier: Modifier = Modifier
) {
    val colors = KomdTheme.colors
    val ui = LocalUiStrings.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(focusRequests) { focusRequester.requestFocus() }
    val canSearch = query.length >= MIN_SEARCH_LENGTH
    Row(
        modifier.height(32.dp).clip(RoundedCornerShape(8.dp)).background(colors.background)
            .border(1.dp, if (status == SearchStatus.TooShort) colors.accent else colors.divider, RoundedCornerShape(8.dp))
            .padding(start = 11.dp, end = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) Text(ui.searchPlaceholder, color = colors.secondary, fontSize = 11.sp, maxLines = 1)
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.foreground, fontSize = 12.sp),
                cursorBrush = SolidColor(colors.accent),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSubmit(true) }),
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester).onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.Enter, Key.NumPadEnter -> { onSubmit(!event.isShiftPressed); true }
                        Key.Escape -> { onClose(); true }
                        else -> false
                    }
                }
            )
        }
        val statusText = when (status) {
            SearchStatus.Idle -> null
            SearchStatus.TooShort -> ui.searchMinChars
            SearchStatus.NoMatches -> ui.noMatches
            is SearchStatus.Found -> "${status.current}/${status.total}"
        }
        statusText?.let {
            Spacer(Modifier.width(6.dp))
            Text(it, color = if (status == SearchStatus.TooShort) colors.accent else colors.secondary, fontSize = 10.sp, maxLines = 1)
        }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                .background(if (canSearch) colors.accent else colors.hover)
                .clickable(role = Role.Button) { onSubmit(true) }
                .semantics { contentDescription = ui.find },
            contentAlignment = Alignment.Center
        ) {
            ArrowGlyph(if (canSearch) colors.onAccent else colors.secondary)
        }
    }
}

/**
 * Paints search hits behind the editor text without changing it (identity offset mapping).
 * [current] gets the strong colour, the other [matches] the soft one.
 */
internal class SearchHighlightTransformation(
    private val matches: List<IntRange>,
    private val current: Int,
    private val soft: Color,
    private val strong: Color,
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (matches.isEmpty()) return TransformedText(text, OffsetMapping.Identity)
        val highlighted = buildAnnotatedString {
            append(text)
            matches.forEachIndexed { index, range ->
                if (range.last < text.length) {
                    addStyle(SpanStyle(background = if (index == current) strong else soft), range.first, range.last + 1)
                }
            }
        }
        return TransformedText(highlighted, OffsetMapping.Identity)
    }

    override fun equals(other: Any?) = other is SearchHighlightTransformation &&
        other.matches == matches && other.current == current && other.soft == soft && other.strong == strong

    override fun hashCode() = (matches.hashCode() * 31 + current) * 31 + strong.hashCode()
}
