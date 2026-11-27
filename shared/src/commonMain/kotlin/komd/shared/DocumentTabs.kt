// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/** What a tab needs to draw itself: decoupled from the editor's Document model. */
internal data class TabItem(val name: String, val dirty: Boolean, val path: String?)

/**
 * Document tabs of the title bar.
 *
 * Layout: `‹ [scrollable strip] › + N▾`. The strip is a [LazyRow]; when the tabs do not fit:
 * - the arrows page the strip and are disabled at the ends ([LazyListState.canScrollBackward]/Forward);
 * - the vertical mouse wheel scrolls it horizontally (trackpads scroll it natively);
 * - the edges fade where more tabs are hidden;
 * - the selected tab is always brought into view;
 * - the counter opens a filterable list of every open tab.
 */
@Composable
internal fun DocumentTabs(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    /** Registry actions: the + button runs [newAction]; [closeAction] only lends label and shortcut to the × hint. */
    newAction: KomdAction,
    closeAction: KomdAction,
    modifier: Modifier = Modifier,
) {
    val colors = KomdTheme.colors
    val ui = LocalUiStrings.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val canBack by remember { derivedStateOf { listState.canScrollBackward } }
    val canForward by remember { derivedStateOf { listState.canScrollForward } }

    // Keep the selected tab fully visible (new document, ⌘K "go to", menu selection…).
    LaunchedEffect(selectedIndex, tabs.size) {
        if (selectedIndex !in tabs.indices) return@LaunchedEffect
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
        val fullyVisible = item != null && item.offset >= info.viewportStartOffset &&
            item.offset + item.size <= info.viewportEndOffset
        if (fullyVisible) return@LaunchedEffect
        if (item != null && item.offset + item.size > info.viewportEndOffset) {
            listState.animateScrollBy((item.offset + item.size - info.viewportEndOffset).toFloat())
        } else {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Row(modifier, verticalAlignment = Alignment.Bottom) {
        TabNavButton(KomdIcons.ChevronLeft, ui.scrollTabsBack, enabled = canBack, modifier = Modifier.align(Alignment.CenterVertically)) {
            scope.launch { listState.animateScrollBy(-listState.layoutInfo.viewportSize.width * 0.6f) }
        }
        LazyRow(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxHeight()
                .fadingEdges(fadeStart = canBack, fadeEnd = canForward)
                .verticalWheelScrollsHorizontally(listState),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            itemsIndexed(tabs, key = { index, tab -> tab.path ?: "untitled-$index-${tab.name}" }) { index, tab ->
                val selected = index == selectedIndex
                DocumentTab(
                    tab, selected = selected, onClick = { onSelect(index) }, onClose = { onClose(index) },
                    closeLabel = closeAction.label,
                    // ⌘W closes the current document only, so the hint belongs to the selected tab.
                    closeShortcut = closeAction.shortcut?.takeIf { selected }?.label(),
                )
            }
        }
        TabNavButton(KomdIcons.ChevronRight, ui.scrollTabsForward, enabled = canForward, modifier = Modifier.align(Alignment.CenterVertically)) {
            scope.launch { listState.animateScrollBy(listState.layoutInfo.viewportSize.width * 0.6f) }
        }
        TabNavButton(KomdIcons.Plus, newAction.label, newAction.shortcut?.label(), enabled = true, modifier = Modifier.align(Alignment.CenterVertically), onClick = newAction.run)
        Box(Modifier.align(Alignment.CenterVertically)) {
            var menuOpen by remember { mutableStateOf(false) }
            KomdTooltip(ui.allTabs) {
                Row(
                    Modifier.height(26.dp).clip(RoundedCornerShape(6.dp)).clickable { menuOpen = true }.padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("${tabs.size}", color = colors.secondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                    KomdIcon(KomdIcons.ChevronDown, colors.secondary, size = 12.dp)
                }
            }
            AllTabsMenu(
                expanded = menuOpen,
                onDismiss = { menuOpen = false },
                tabs = tabs,
                selectedIndex = selectedIndex,
                onSelect = { menuOpen = false; onSelect(it) },
                placeholder = ui.filterTabs,
                modifiedLabel = ui.modified,
            )
        }
    }
}

@Composable
private fun DocumentTab(tab: TabItem, selected: Boolean, onClick: () -> Unit, onClose: () -> Unit, closeLabel: String, closeShortcut: String?) {
    val colors = KomdTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    Row(
        Modifier.height(34.dp).widthIn(max = 190.dp)
            .clip(shape)
            .then(
                if (selected) Modifier.background(colors.background).border(1.dp, colors.divider, shape)
                else if (hovered) Modifier.background(colors.hover) else Modifier
            )
            .hoverable(interaction)
            .clickable(onClick = onClick)
            .padding(start = 14.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (tab.dirty) Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(colors.accent))
        Text(
            tab.name,
            Modifier.weight(1f, fill = false),
            color = if (selected) colors.foreground else colors.secondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        KomdTooltip(closeLabel, closeShortcut) {
            Box(
                Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)).clickable(onClick = onClose),
                contentAlignment = Alignment.Center
            ) {
                if (selected || hovered) KomdIcon(KomdIcons.Close, colors.faint, size = 12.dp)
            }
        }
    }
}

@Composable
private fun TabNavButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    shortcut: String? = null,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val colors = KomdTheme.colors
    KomdTooltip(label, shortcut, modifier) {
        Box(
            Modifier.size(26.dp).clip(RoundedCornerShape(6.dp))
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center
        ) {
            KomdIcon(icon, colors.secondary.copy(alpha = if (enabled) 1f else 0.3f), size = 14.dp)
        }
    }
}

@Composable
private fun AllTabsMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    tabs: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    placeholder: String,
    modifiedLabel: String,
) {
    val colors = KomdTheme.colors
    var filter by remember(expanded) { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val shown = remember(tabs, filter) { rankByQuery(tabs.withIndex().toList(), filter) { it.value.name } }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = DpOffset(0.dp, 6.dp),
        containerColor = colors.background,
        modifier = Modifier.width(310.dp).border(1.dp, colors.divider, RoundedCornerShape(10.dp))
    ) {
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        Row(
            Modifier.padding(horizontal = 6.dp).fillMaxWidth().height(30.dp)
                .border(1.dp, colors.divider, RoundedCornerShape(6.dp)).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KomdIcon(KomdIcons.Search, colors.faint, size = 14.dp)
            Box(Modifier.weight(1f)) {
                if (filter.isEmpty()) Text(placeholder, color = colors.faint, fontSize = 12.sp)
                BasicTextField(
                    filter,
                    { filter = it },
                    Modifier.fillMaxWidth().focusRequester(focus).onPreviewKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown && event.key == Key.Enter) {
                            shown.firstOrNull()?.let { onSelect(it.index) }
                            true
                        } else false
                    },
                    singleLine = true,
                    textStyle = TextStyle(color = colors.foreground, fontSize = 12.sp),
                    cursorBrush = SolidColor(colors.accent)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Column(Modifier.heightIn(max = 360.dp)) {
            shown.forEach { (index, tab) ->
                val selected = index == selectedIndex
                Row(
                    Modifier.padding(horizontal = 6.dp).fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        .background(if (selected) colors.accentSoft else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KomdIcon(KomdIcons.File, if (selected) colors.accent else colors.secondary, size = 14.dp)
                    Text(
                        tab.name, Modifier.weight(1f), color = if (selected) colors.foreground else colors.secondary,
                        fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    if (tab.dirty) Text(modifiedLabel, color = colors.faint, fontSize = 11.sp)
                }
            }
        }
    }
}

/** Fades the start/end edge of a scrolling strip, only where there is hidden content. */
internal fun Modifier.fadingEdges(fadeStart: Boolean, fadeEnd: Boolean, width: androidx.compose.ui.unit.Dp = 28.dp): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val fade = width.toPx().coerceAtMost(size.width / 3f)
            if (fadeStart) drawRect(
                Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = 0f, endX = fade),
                blendMode = BlendMode.DstIn
            )
            if (fadeEnd) drawRect(
                Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - fade, endX = size.width),
                blendMode = BlendMode.DstIn
            )
        }

/** A plain (vertical) mouse wheel scrolls the strip horizontally; horizontal deltas are left to the LazyRow. */
private fun Modifier.verticalWheelScrollsHorizontally(state: LazyListState): Modifier = pointerInput(state) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type != PointerEventType.Scroll) continue
            val change = event.changes.firstOrNull() ?: continue
            val delta = change.scrollDelta
            if (delta.y != 0f && delta.x == 0f) {
                state.dispatchRawDelta(delta.y * 48f)
                change.consume()
            }
        }
    }
}
