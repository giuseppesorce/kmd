// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Stroke icons of the Ink design (24×24 grid, 1.6 stroke, round caps), drawn as vectors so the
 * app needs no icon font or extra dependency. Path data mirrors the SVG symbols of the mockup.
 */
internal object KomdIcons {
    val File = icon("M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z", "M14 3v5h5")
    val Folder = icon("M3 7a2 2 0 0 1 2-2h4l2 2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z")
    val Search = icon(circle(11f, 11f, 7f), "m20 20-3.5-3.5")
    val BulletList = icon("M9 6h11M9 12h11M9 18h11M4 6h.01M4 12h.01M4 18h.01")
    val NumberedList = icon("M10 6h10M10 12h10M10 18h10", "M4 5l1.5-1v4.5", "M4 14.6a1.4 1.4 0 1 1 2.4 1L4 18.5h3")
    val Task = icon(rect(4f, 4f, 16f, 16f, 3f), "m8 12 3 3 5-6")
    val Quote = icon("M7 7h4v4c0 3-2 5-4 5M14 7h4v4c0 3-2 5-4 5")
    val Table = icon(rect(3f, 4f, 18f, 16f, 2f), "M3 10h18M9 4v16")
    val Link = icon("M10 14a4 4 0 0 0 5.7 0l3-3a4 4 0 0 0-5.7-5.7l-1 1", "M14 10a4 4 0 0 0-5.7 0l-3 3a4 4 0 0 0 5.7 5.7l1-1")
    val Image = icon(rect(3f, 4f, 18f, 16f, 2f), circle(9f, 10f, 2f), "m21 16-5-5-9 9")
    val Code = icon("m8 8-4 4 4 4M16 8l4 4-4 4")
    val CodeBlock = icon(rect(3f, 4f, 18f, 16f, 2f), "m10 10-2 2 2 2M14 10l2 2-2 2")
    val Gear = icon(
        circle(12f, 12f, 3f),
        "M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-2.9 1.2V21a2 2 0 1 1-4 0v-.1A1.7 1.7 0 0 0 7 19.4l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1A1.7 1.7 0 0 0 3 13.7H3a2 2 0 1 1 0-4h.1A1.7 1.7 0 0 0 4.6 7l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1A1.7 1.7 0 0 0 10.3 3V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 2.9 1.2l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0 1.2 2.9H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"
    )
    val Outline = icon("M4 6h16M8 12h12M12 18h8")
    val Plus = icon("M12 5v14M5 12h14")
    val Clock = icon(circle(12f, 12f, 8f), "M12 8v4l3 2")
    val Share = icon("M12 3v12M7 8l5-5 5 5M5 14v5a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-5")
    val Save = icon("M5 3h11l3 3v13a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2z", "M8 3v5h7V3M8 21v-7h8v7")
    val Pdf = icon("M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8z", "M9 13h6M9 17h4")
    val Sidebar = icon(rect(3f, 4f, 18f, 16f, 2f), "M9 4v16")
    val ChevronRight = icon("m9 6 6 6-6 6")
    val ChevronLeft = icon("m15 6-6 6 6 6")
    val ChevronDown = icon("m6 9 6 6 6-6")
    val Trash = icon("M4 7h16M10 11v6M14 11v6", "M6 7l1 12a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-12", "M9 7V4h6v3")
    val Close = icon("M7 7l10 10M17 7 7 17")
    val Sun = icon(circle(12f, 12f, 4f), "M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4")
    val Moon = icon("M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z")
    val Eye = icon("M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z", circle(12f, 12f, 3f))
    val Command = icon("M9 6a3 3 0 1 0-3 3h12a3 3 0 1 0-3-3v12a3 3 0 1 0 3-3H6a3 3 0 1 0 3 3z")
}

@Composable
internal fun KomdIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier, size: Dp = 16.dp) {
    Icon(icon, contentDescription = null, tint = tint, modifier = modifier.size(size))
}

private fun icon(vararg paths: String): ImageVector {
    val builder = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
    paths.forEach { data ->
        builder.addPath(
            pathData = addPathNodes(data),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0z"

private fun rect(x: Float, y: Float, w: Float, h: Float, r: Float) =
    "M${x + r} ${y}h${w - 2 * r}a$r $r 0 0 1 $r ${r}v${h - 2 * r}a$r $r 0 0 1 ${-r} ${r}h${-(w - 2 * r)}a$r $r 0 0 1 ${-r} ${-r}v${-(h - 2 * r)}a$r $r 0 0 1 $r ${-r}z"
