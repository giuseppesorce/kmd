package komd.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One row of the palette: an action of the registry, an open document or a recent file. */
internal data class PaletteEntry(
    val label: String,
    val detail: String? = null,
    val icon: ImageVector? = null,
    val shortcut: String? = null,
    val run: () -> Unit,
)

/**
 * ⌘K command palette: a filter field over every enabled action of the registry plus the open
 * documents and recent files. ↑/↓ move, Enter runs, Esc (or a click outside) closes.
 */
@Composable
internal fun CommandPalette(entries: List<PaletteEntry>, placeholder: String, emptyLabel: String, onDismiss: () -> Unit) {
    val colors = KomdTheme.colors
    var query by remember { mutableStateOf("") }
    var highlighted by remember { mutableIntStateOf(0) }
    val shown = remember(entries, query) { rankByQuery(entries, query) { "${it.label} ${it.detail.orEmpty()}" } }
    val focus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    LaunchedEffect(highlighted) { if (highlighted in shown.indices) listState.animateScrollToItem(highlighted) }
    val run: (PaletteEntry) -> Unit = { entry ->
        onDismiss()
        entry.run()
    }

    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (colors.isDark) 0.45f else 0.18f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            Modifier.padding(top = 72.dp, start = 16.dp, end = 16.dp).widthIn(max = 560.dp).fillMaxWidth()
                .shadow(24.dp, RoundedCornerShape(12.dp))
                .clip(RoundedCornerShape(12.dp))
                .background(colors.background)
                .border(1.dp, colors.divider, RoundedCornerShape(12.dp))
                // Swallow clicks so they do not reach the scrim.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
        ) {
            Row(
                Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KomdIcon(KomdIcons.Search, colors.secondary)
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text(placeholder, color = colors.faint, fontSize = 14.sp)
                    BasicTextField(
                        query,
                        { query = it; highlighted = 0 },
                        Modifier.fillMaxWidth().focusRequester(focus).onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.DirectionDown -> { if (shown.isNotEmpty()) highlighted = (highlighted + 1) % shown.size; true }
                                Key.DirectionUp -> { if (shown.isNotEmpty()) highlighted = (highlighted - 1 + shown.size) % shown.size; true }
                                Key.Enter, Key.NumPadEnter -> { shown.getOrNull(highlighted)?.let(run); true }
                                Key.Escape -> { onDismiss(); true }
                                else -> false
                            }
                        },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.foreground, fontSize = 14.sp),
                        cursorBrush = SolidColor(colors.accent)
                    )
                }
                Text("esc", color = colors.faint, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.divider))
            if (shown.isEmpty()) {
                Text(emptyLabel, Modifier.padding(16.dp), color = colors.secondary, fontSize = 13.sp)
            } else {
                LazyColumn(Modifier.heightIn(max = 380.dp).padding(6.dp), state = listState) {
                    itemsIndexed(shown) { index, entry ->
                        val active = index == highlighted
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
                                .background(if (active) colors.accentSoft else Color.Transparent)
                                .clickable { run(entry) }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                                entry.icon?.let { KomdIcon(it, if (active) colors.accent else colors.secondary) }
                            }
                            Text(entry.label, color = colors.foreground, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            entry.detail?.let {
                                Text(it, Modifier.weight(1f, fill = false), color = colors.faint, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Spacer(Modifier.weight(1f))
                            entry.shortcut?.let {
                                Text(
                                    it,
                                    Modifier.border(1.dp, colors.divider, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp),
                                    color = colors.secondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
