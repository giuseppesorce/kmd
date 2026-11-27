// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Design tokens of the "Ink" theme: the navy of the K#M icon with a sky-blue accent.
 *
 * Every composable reads colors through [KomdTheme.colors]; nothing outside this file should
 * hard-code a UI color, so light/dark (and any future theme) is a single switch in [KomdTheme].
 */
@Immutable
internal data class KomdColors(
    /** Document surface: editor and preview. */
    val background: Color,
    /** Window chrome: title/tab bar, status bar. */
    val chrome: Color,
    /** Rail and side panel. */
    val sidebar: Color,
    /** Hover / pressed / selected-neutral fill. */
    val hover: Color,
    val foreground: Color,
    /** Secondary text (muted). */
    val secondary: Color,
    /** Tertiary text: line numbers, hints, section labels. */
    val faint: Color,
    /** Borders and separators. */
    val divider: Color,
    val accent: Color,
    /** Tinted fill behind accent content (active rail item, selected row). */
    val accentSoft: Color,
    /** Text/icons drawn on top of [accent]. */
    val onAccent: Color,
    /** Headings in the preview. */
    val heading: Color,
    val editorText: Color,
    val codeText: Color,
    /** Strings in code, and the "saved" status dot. */
    val string: Color,
    val codeBackground: Color,
    val isDark: Boolean,
)

internal val InkLight = KomdColors(
    background = Color(0xFFFFFFFF),
    chrome = Color(0xFFF5F6FA),
    sidebar = Color(0xFFF5F6FA),
    hover = Color(0xFFE9EBF3),
    foreground = Color(0xFF121826),
    secondary = Color(0xFF5B6478),
    faint = Color(0xFFA3AABB),
    divider = Color(0xFFE3E6EE),
    accent = Color(0xFF0A84C2),
    accentSoft = Color(0xFFE2F1FA),
    onAccent = Color(0xFFFFFFFF),
    heading = Color(0xFF17345A),
    editorText = Color(0xFF121826),
    codeText = Color(0xFF3B4357),
    string = Color(0xFF0F7B6C),
    codeBackground = Color(0xFFF5F6FA),
    isDark = false,
)

internal val InkDark = KomdColors(
    background = Color(0xFF0B1322),
    chrome = Color(0xFF08101D),
    sidebar = Color(0xFF08101D),
    hover = Color(0xFF16213A),
    foreground = Color(0xFFE7EDF7),
    secondary = Color(0xFF93A0B8),
    faint = Color(0xFF4C5A75),
    divider = Color(0xFF1B2740),
    accent = Color(0xFF7DCBF5),
    accentSoft = Color(0xFF0E2A3F),
    onAccent = Color(0xFF08101D),
    heading = Color(0xFFE7EDF7),
    editorText = Color(0xFFDCE5F1),
    codeText = Color(0xFFC6D0E2),
    string = Color(0xFF7FD6C2),
    codeBackground = Color(0xFF101B2E),
    isDark = true,
)

internal val LocalKomdColors = staticCompositionLocalOf { InkLight }

internal object KomdTheme {
    val colors: KomdColors
        @Composable @ReadOnlyComposable get() = LocalKomdColors.current
}

@Composable
internal fun KomdTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalKomdColors provides if (dark) InkDark else InkLight, content = content)
}
