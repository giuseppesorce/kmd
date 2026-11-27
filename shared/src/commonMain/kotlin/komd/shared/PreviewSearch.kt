// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString

/**
 * Search state handed to the preview.
 *
 * The preview searches its own *rendered* text (no `**`, link URLs, …), so its hits can differ
 * from the source ones. They are paired by order: the n-th source match is the n-th rendered hit
 * ([current], clamped to the last hit). [anchor] receives where the current hit was laid out,
 * so the preview can scroll to it.
 */
internal class PreviewSearch(
    val query: String,
    val current: Int,
    val soft: Color,
    val strong: Color,
    val anchor: HitAnchor,
)

/** Last known position of the current preview hit: [coordinates] of its Text plus the hit's [top] inside it. */
internal class HitAnchor {
    var ordinal: Int = -1
    var coordinates: LayoutCoordinates? = null
    var top: Float = 0f
}

internal val LocalPreviewSearch = compositionLocalOf<PreviewSearch?> { null }

/** Highlighted text plus what the Text needs to report the current hit position. */
internal class HighlightedText(
    val text: AnnotatedString,
    val modifier: Modifier,
    val onTextLayout: (TextLayoutResult) -> Unit,
)

/**
 * Adds search highlights to one rendered text unit whose first hit has global ordinal [base].
 * Must be called with the same [base] that `searchCount` used when numbering the units.
 */
@Composable
internal fun rememberSearchHighlight(text: AnnotatedString, base: Int): HighlightedText {
    val search = LocalPreviewSearch.current
    val query = search?.query.orEmpty()
    val matches = remember(text.text, query) { findMatches(text.text, query) }
    if (search == null || matches.isEmpty()) return HighlightedText(text, Modifier, {})
    val local = search.current - base
    val highlighted = remember(text, matches, local, search.soft, search.strong) {
        buildAnnotatedString {
            append(text)
            matches.forEachIndexed { index, range ->
                addStyle(SpanStyle(background = if (index == local) search.strong else search.soft), range.first, range.last + 1)
            }
        }
    }
    if (local !in matches.indices) return HighlightedText(highlighted, Modifier, {})
    val anchor = search.anchor
    val ordinal = search.current
    val start = matches[local].first
    return HighlightedText(
        highlighted,
        Modifier.onGloballyPositioned { anchor.ordinal = ordinal; anchor.coordinates = it },
        { layout -> if (start < layout.layoutInput.text.length) anchor.top = layout.getBoundingBox(start).top },
    )
}
