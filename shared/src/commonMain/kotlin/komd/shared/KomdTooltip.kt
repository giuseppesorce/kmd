// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Where the bubble opens relative to the hovered control. */
internal enum class TooltipSide { Below, Above, End }

/**
 * Ink tooltip: shown on mouse hover (long press on touch) over an icon-only control.
 *
 * The bubble is inverted against the chrome (foreground colour as fill) so it reads on every
 * surface in both themes, and shows the action name plus its shortcut, if any.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun KomdTooltip(
    label: String,
    shortcut: String? = null,
    modifier: Modifier = Modifier,
    side: TooltipSide = TooltipSide.Below,
    content: @Composable () -> Unit,
) {
    val anchor = when (side) {
        TooltipSide.Below -> TooltipAnchorPosition.Below
        TooltipSide.Above -> TooltipAnchorPosition.Above
        TooltipSide.End -> TooltipAnchorPosition.End
    }
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(anchor, 6.dp),
        tooltip = { TooltipBubble(label, shortcut) },
        state = rememberTooltipState(),
        modifier = modifier,
        focusable = false,
        content = content,
    )
}

/** Tooltip for a registry action: label and shortcut come from the single [ActionRegistry]. */
@Composable
internal fun KomdTooltip(
    action: KomdAction,
    modifier: Modifier = Modifier,
    side: TooltipSide = TooltipSide.Below,
    content: @Composable () -> Unit,
) = KomdTooltip(action.label.removeSuffix("…"), action.shortcut?.label(), modifier, side, content)

@Composable
private fun TooltipBubble(label: String, shortcut: String?) {
    val colors = KomdTheme.colors
    val shape = RoundedCornerShape(6.dp)
    Row(
        Modifier.shadow(6.dp, shape).background(colors.foreground, shape).padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label, color = colors.background, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        shortcut?.let {
            Text(it, color = colors.background.copy(alpha = 0.6f), fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
        }
    }
}
