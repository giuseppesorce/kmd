package komd.shared

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// Window chrome of the Ink layout:
//
//   ┌ TitleBar: ‹ tabs › + N▾ │ Cerca o esegui… ⌘K │ Editor·Diviso·Anteprima ┐
//   ├ Rail ┬ side panel ┬ FormatBar ─────────────────────────── crumb ┤
//   │  ▢   │ Files      │ EditorPane │ PreviewPane                     │
//   │  ⌕   │ Search     │                                              │
//   │  ≡   │ Outline    │                                              │
//   │  ⇪   │ Export     ├ StatusBar ───────────────────────────────────┤
//   └──────┴────────────┴──────────────────────────────────────────────┘
//
// Every button here is a projection of a [KomdAction] from the registry built in App.

/** 1dp line on the trailing edge (rail, side panel). */
internal fun Modifier.endBorder(color: Color): Modifier = drawBehind {
    drawRect(color, topLeft = Offset(size.width - 1.dp.toPx(), 0f), size = androidx.compose.ui.geometry.Size(1.dp.toPx(), size.height))
}

/** 1dp line on the bottom edge. */
internal fun Modifier.bottomBorder(color: Color): Modifier = drawBehind {
    drawRect(color, topLeft = Offset(0f, size.height - 1.dp.toPx()), size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
}

/** 1dp line on the top edge. */
internal fun Modifier.topBorder(color: Color): Modifier = drawBehind {
    drawRect(color, size = androidx.compose.ui.geometry.Size(size.width, 1.dp.toPx()))
}

@Composable
internal fun TitleBar(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onNew: () -> Unit,
    paletteLabel: String,
    paletteShortcut: String?,
    onPalette: () -> Unit,
    mode: WorkspaceMode,
    onModeChange: (WorkspaceMode) -> Unit,
) {
    val colors = KomdTheme.colors
    val ui = LocalUiStrings.current
    Row(
        Modifier.fillMaxWidth().height(46.dp).background(colors.chrome).bottomBorder(colors.divider).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        DocumentTabs(tabs, selectedIndex, onSelect, onClose, onNew, Modifier.weight(1f).fillMaxHeight())
        Row(
            Modifier.width(200.dp).height(30.dp).clip(RoundedCornerShape(8.dp)).background(colors.background)
                .border(1.dp, colors.divider, RoundedCornerShape(8.dp))
                .clickable(role = Role.Button, onClick = onPalette)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KomdIcon(KomdIcons.Search, colors.secondary, size = 14.dp)
            Text(paletteLabel, Modifier.weight(1f), color = colors.secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            paletteShortcut?.let { Kbd(it) }
        }
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).background(colors.hover).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            listOf(WorkspaceMode.Editor, WorkspaceMode.Split, WorkspaceMode.Preview).forEach { option ->
                val selected = option == mode
                Text(
                    workspaceModeLabel(option, ui),
                    Modifier.clip(RoundedCornerShape(6.dp))
                        .background(if (selected) colors.background else Color.Transparent)
                        .clickable(role = Role.Tab) { onModeChange(option) }
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    color = if (selected) colors.foreground else colors.secondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun Kbd(label: String) {
    val colors = KomdTheme.colors
    Text(
        label,
        Modifier.border(1.dp, colors.divider, RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp),
        color = colors.secondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1
    )
}

@Composable
internal fun Rail(
    active: SidePanel?,
    onSelect: (SidePanel) -> Unit,
    themeAction: KomdAction,
    settingsAction: KomdAction,
) {
    val colors = KomdTheme.colors
    val ui = LocalUiStrings.current
    Column(
        Modifier.width(52.dp).fillMaxHeight().background(colors.sidebar).endBorder(colors.divider).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val items = listOf(
            Triple(SidePanel.Files, KomdIcons.File, ui.panelFiles),
            Triple(SidePanel.Search, KomdIcons.Search, ui.panelSearch),
            Triple(SidePanel.Outline, KomdIcons.Outline, ui.panelOutline),
            Triple(SidePanel.Export, KomdIcons.Share, ui.panelExport),
        )
        items.forEach { (panel, icon, label) ->
            RailButton(icon, label, selected = active == panel) { onSelect(panel) }
        }
        Spacer(Modifier.weight(1f))
        RailButton(themeAction.icon ?: KomdIcons.Moon, themeAction.label, selected = false, onClick = themeAction.run)
        RailButton(KomdIcons.Gear, settingsAction.label, selected = false, onClick = settingsAction.run)
    }
}

@Composable
private fun RailButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = KomdTheme.colors
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(8.dp))
            .background(if (selected) colors.accentSoft else Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        KomdIcon(icon, if (selected) colors.accent else colors.secondary)
    }
}

@Composable
private fun PanelLabel(text: String, trailing: String? = null) {
    val colors = KomdTheme.colors
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 6.dp)) {
        Text(text.uppercase(), color = colors.faint, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        Spacer(Modifier.weight(1f))
        trailing?.let { Text(it, color = colors.faint, fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun PanelRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    label: String,
    selected: Boolean = false,
    detail: String? = null,
    meta: @Composable (() -> Unit)? = null,
    indent: Dp = 0.dp,
    /** Secondary click (desktop) or long press (touch), with the pointer position inside the row. */
    onContextMenu: ((Offset) -> Unit)? = null,
    onClick: () -> Unit,
) {
    val colors = KomdTheme.colors
    val contextMenu by rememberUpdatedState(onContextMenu)
    val lastPress = remember { ContextPress() }
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
            .background(if (selected) colors.hover else Color.Transparent)
            .then(
                if (onContextMenu != null) Modifier.contextMenuGesture(lastPress, { contextMenu?.invoke(it) }, onClick)
                else Modifier.clickable(onClick = onClick)
            )
            .padding(start = 8.dp + indent, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        icon?.let { KomdIcon(it, if (selected) colors.foreground else colors.secondary) }
        Column(Modifier.weight(1f)) {
            Text(
                label, color = if (selected) colors.foreground else colors.secondary, fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            detail?.let { Text(it, color = colors.faint, fontSize = 10.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
        meta?.invoke()
    }
}

@Composable
internal fun FilesPanel(
    newAction: KomdAction,
    tabs: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    recentFiles: List<RecentFile>,
    openPaths: Set<String>,
    onOpenRecent: (RecentFile) -> Unit,
    onRemoveRecent: (RecentFile) -> Unit,
    clearRecentAction: KomdAction,
) {
    val colors = KomdTheme.colors
    val ui = LocalUiStrings.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 14.dp)) {
        Row(
            Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(8.dp)).background(colors.accent)
                .clickable(role = Role.Button, onClick = newAction.run).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            KomdIcon(KomdIcons.Plus, colors.onAccent)
            Text(newAction.label, Modifier.weight(1f), color = colors.onAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
            // "Ctrl+N" would truncate the label on Windows/Linux; the shortcut is still in the menu and in ⌘K.
            newAction.shortcut?.takeIf { shortcutUsesCommand }?.let { Text(it.label(), color = colors.onAccent.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace) }
        }
        Spacer(Modifier.height(16.dp))
        PanelLabel(ui.documents, "${tabs.size}")
        tabs.forEachIndexed { index, tab ->
            PanelRow(KomdIcons.File, tab.name, selected = index == selectedIndex, detail = null, meta = if (tab.dirty) {
                { Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(colors.accent)) }
            } else null) { onSelect(index) }
        }
        if (recentFiles.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            PanelLabel(ui.recentFiles)
            recentFiles.forEach { recent ->
                var menuAt by remember(recent.path) { mutableStateOf<Offset?>(null) }
                Box {
                    PanelRow(
                        KomdIcons.Clock, recent.name,
                        selected = menuAt != null,
                        detail = parentDirectory(recent.path),
                        meta = if (recent.path in openPaths) {
                            { Box(Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(colors.string)) }
                        } else null,
                        onContextMenu = { menuAt = it }
                    ) { onOpenRecent(recent) }
                    menuAt?.let { at ->
                        // Zero-size anchor at the pointer, so the menu opens where the user clicked.
                        Box(Modifier.offset { IntOffset(at.x.roundToInt(), at.y.roundToInt()) }.size(0.dp)) {
                            RecentFileMenu(
                                onDismiss = { menuAt = null },
                                onRemove = { menuAt = null; onRemoveRecent(recent) },
                                clearAction = clearRecentAction,
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Context menu of a recent file. Both items only edit the recents list in the platform preferences:
 * the file itself is never touched on disk.
 */
@Composable
private fun RecentFileMenu(onDismiss: () -> Unit, onRemove: () -> Unit, clearAction: KomdAction) {
    val colors = KomdTheme.colors
    val ui = LocalUiStrings.current
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        containerColor = colors.background,
        modifier = Modifier.width(220.dp).border(1.dp, colors.divider, RoundedCornerShape(10.dp))
    ) {
        ContextMenuItem(KomdIcons.Close, ui.removeFromRecent, onClick = onRemove)
        Box(Modifier.padding(vertical = 4.dp).fillMaxWidth().height(1.dp).background(colors.divider))
        ContextMenuItem(clearAction.icon, clearAction.label, enabled = clearAction.enabled) { onDismiss(); clearAction.run() }
    }
}

@Composable
private fun ContextMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector?, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    val colors = KomdTheme.colors
    val alpha = if (enabled) 1f else 0.4f
    Row(
        Modifier.padding(horizontal = 6.dp).fillMaxWidth().clip(RoundedCornerShape(6.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        icon?.let { KomdIcon(it, colors.secondary.copy(alpha = alpha), size = 15.dp) }
        Text(label, color = colors.foreground.copy(alpha = alpha), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Click + context menu: a secondary-button press (right click / two-finger tap on desktop) or a
 * long press (touch) calls [onContextMenu] with the pointer position; a primary click calls [onClick].
 */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.contextMenuGesture(press: ContextPress, onContextMenu: (Offset) -> Unit, onClick: () -> Unit): Modifier =
    pointerInput(press) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (event.type != PointerEventType.Press) continue
                val position = event.changes.first().position
                press.position = position
                if (event.buttons.isSecondaryPressed) {
                    event.changes.forEach { it.consume() }
                    onContextMenu(position)
                }
            }
        }
    }.combinedClickable(onLongClick = { onContextMenu(press.position) }, onClick = onClick)

/** Last press position of a row: long press has no position of its own, so it reuses this one. */
private class ContextPress(var position: Offset = Offset.Zero)

@Composable
internal fun SearchPanel(
    searchField: @Composable (Modifier) -> Unit,
    content: String,
    matches: List<IntRange>,
    current: Int,
    onSelect: (Int) -> Unit,
) {
    val colors = KomdTheme.colors
    val ui = LocalUiStrings.current
    Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 14.dp)) {
        searchField(Modifier.fillMaxWidth())
        Spacer(Modifier.height(14.dp))
        if (matches.isEmpty()) {
            Text(ui.searchHint, Modifier.padding(horizontal = 8.dp), color = colors.faint, fontSize = 12.sp)
            return@Column
        }
        PanelLabel(ui.searchResults.replace("{count}", "${matches.size}"))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            matches.forEachIndexed { index, range ->
                val (line, _) = caretPosition(content, range.first)
                val lineStart = content.lastIndexOf('\n', range.first - 1) + 1
                val lineEnd = content.indexOf('\n', range.first).let { if (it < 0) content.length else it }
                // Keep a little context before the hit, then let the row ellipsize the rest.
                val from = maxOf(lineStart, range.first - 18)
                val snippet = buildAnnotatedString {
                    if (from > lineStart) append("…")
                    append(content.substring(from, range.first).trimStart())
                    withStyle(SpanStyle(color = colors.foreground, fontWeight = FontWeight.SemiBold, background = colors.accent.copy(alpha = 0.18f))) {
                        append(content.substring(range.first, (range.last + 1).coerceAtMost(lineEnd)))
                    }
                    append(content.substring((range.last + 1).coerceAtMost(lineEnd), lineEnd))
                }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                        .background(if (index == current) colors.accentSoft else Color.Transparent)
                        .clickable { onSelect(index) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("$line", Modifier.widthIn(min = 22.dp), color = colors.faint, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    Text(snippet, color = colors.secondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
internal fun OutlinePanel(outline: List<OutlineEntry>, caret: Int, onSelect: (OutlineEntry) -> Unit) {
    val colors = KomdTheme.colors
    val ui = LocalUiStrings.current
    // The heading the caret is in: the last one starting at or before it.
    val currentIndex = outline.indexOfLast { it.offset <= caret }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 14.dp)) {
        PanelLabel(ui.panelOutline, if (outline.isEmpty()) null else "${outline.size}")
        if (outline.isEmpty()) {
            Text(ui.noOutline, Modifier.padding(horizontal = 8.dp), color = colors.faint, fontSize = 12.sp)
        }
        val minLevel = outline.minOfOrNull { it.level } ?: 1
        outline.forEachIndexed { index, entry ->
            val selected = index == currentIndex
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp))
                    .background(if (selected) colors.accentSoft else Color.Transparent)
                    .clickable { onSelect(entry) }
                    .padding(start = 8.dp + 14.dp * (entry.level - minLevel), end = 8.dp, top = 5.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("H${entry.level}", color = if (selected) colors.accent else colors.faint, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                Text(
                    entry.title, color = if (selected) colors.foreground else colors.secondary,
                    fontSize = 13.sp, fontWeight = if (entry.level == minLevel) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun ExportPanel(exports: List<Pair<KomdAction, String>>, saves: List<KomdAction>) {
    val colors = KomdTheme.colors
    val ui = LocalUiStrings.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 14.dp)) {
        PanelLabel(ui.panelExport)
        exports.forEach { (action, hint) ->
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp).clip(RoundedCornerShape(8.dp))
                    .background(colors.background).border(1.dp, colors.divider, RoundedCornerShape(8.dp))
                    .clickable(enabled = action.enabled, onClick = action.run)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KomdIcon(action.icon ?: KomdIcons.Share, colors.accent, size = 18.dp)
                Column(Modifier.weight(1f)) {
                    Text(action.label.removeSuffix("…"), color = colors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text(hint, color = colors.faint, fontSize = 11.sp)
                }
                action.shortcut?.let { Kbd(it.label()) }
            }
        }
        Spacer(Modifier.height(14.dp))
        PanelLabel(ui.panelFiles)
        saves.forEach { action ->
            PanelRow(action.icon, action.label, meta = action.shortcut?.let { { Kbd(it.label()) } }, onClick = action.run)
        }
    }
}

/** One-row formatting toolbar: heading menu | inline | blocks | inserts, document path on the right. */
@Composable
internal fun FormatBar(actions: ActionRegistry, currentHeading: Int, crumb: String) {
    val colors = KomdTheme.colors
    val ui = LocalUiStrings.current
    Row(
        Modifier.fillMaxWidth().height(42.dp).background(colors.background).bottomBorder(colors.divider).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box {
            var open by remember { mutableStateOf(false) }
            Row(
                Modifier.height(28.dp).clip(RoundedCornerShape(6.dp))
                    .background(if (currentHeading > 0) colors.hover else Color.Transparent)
                    .clickable(role = Role.Button) { open = true }
                    .semantics { contentDescription = ui.heading }
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    if (currentHeading > 0) "H$currentHeading" else "H",
                    color = if (currentHeading > 0) colors.foreground else colors.secondary,
                    fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold
                )
                KomdIcon(KomdIcons.ChevronDown, colors.secondary, size = 12.dp)
            }
            DropdownMenu(open, onDismissRequest = { open = false }, offset = DpOffset(0.dp, 4.dp), containerColor = colors.background) {
                listOf(ActionId.H1, ActionId.H2, ActionId.H3).forEachIndexed { index, id ->
                    val action = actions[id]
                    Row(
                        Modifier.width(200.dp).clickable { open = false; action.run() }.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            action.label, Modifier.weight(1f), color = colors.heading,
                            fontSize = (18 - index * 2).sp, fontWeight = FontWeight.SemiBold
                        )
                        action.shortcut?.let { Kbd(it.label()) }
                    }
                }
            }
        }
        ToolbarSeparator()
        listOf(ActionId.Bold, ActionId.Italic, ActionId.Strikethrough, ActionId.InlineCode, ActionId.Link).forEach { ActionButton(actions[it]) }
        ToolbarSeparator()
        listOf(ActionId.BulletList, ActionId.NumberedList, ActionId.TaskList, ActionId.Quote).forEach { ActionButton(actions[it]) }
        ToolbarSeparator()
        listOf(ActionId.InsertTable, ActionId.InsertImage, ActionId.InsertCodeBlock).forEach { ActionButton(actions[it]) }
        Spacer(Modifier.weight(1f).widthIn(min = 12.dp))
        Text(crumb, color = colors.faint, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ToolbarSeparator() {
    Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(18.dp).background(KomdTheme.colors.divider))
}

/** Toolbar button for a registry action: its icon, or a styled glyph for B / I / S. */
@Composable
internal fun ActionButton(action: KomdAction, size: Dp = 28.dp) {
    val colors = KomdTheme.colors
    val tint = colors.secondary.copy(alpha = if (action.enabled) 1f else 0.4f)
    val description = action.shortcut?.let { "${action.label} (${it.label()})" } ?: action.label
    Box(
        Modifier.size(size).clip(RoundedCornerShape(6.dp))
            .clickable(enabled = action.enabled, role = Role.Button, onClick = action.run)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        val icon = action.icon
        if (icon != null) {
            KomdIcon(icon, tint)
        } else {
            val glyph = when (action.id) {
                ActionId.Bold -> "B"
                ActionId.Italic -> "I"
                ActionId.Strikethrough -> "S"
                ActionId.H1 -> "H1"
                ActionId.H2 -> "H2"
                ActionId.H3 -> "H3"
                else -> action.label.take(1)
            }
            Text(
                glyph,
                color = tint,
                fontSize = 13.sp,
                fontWeight = if (action.id == ActionId.Italic) FontWeight.Medium else FontWeight.Bold,
                fontStyle = if (action.id == ActionId.Italic) FontStyle.Italic else FontStyle.Normal,
                fontFamily = if (action.id == ActionId.Italic) FontFamily.Serif else null,
                textDecoration = if (action.id == ActionId.Strikethrough) TextDecoration.LineThrough else null,
            )
        }
    }
}

@Composable
internal fun StatusBar(
    dirty: Boolean,
    hasFile: Boolean,
    content: String,
    caret: Int,
    message: String?,
    syncScroll: Boolean,
    onToggleSync: () -> Unit,
    zoom: Float,
    onResetZoom: () -> Unit,
) {
    val colors = KomdTheme.colors
    val ui = LocalUiStrings.current
    val (line, column) = remember(content, caret) { caretPosition(content, caret) }
    val words = remember(content) { content.trim().split(Regex("\\s+")).count { it.isNotEmpty() } }
    val small = 11.5.sp
    Row(
        Modifier.fillMaxWidth().height(28.dp).background(colors.chrome).topBorder(colors.divider).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(if (dirty) colors.accent else colors.string))
            Text(
                when {
                    dirty -> ui.statusModified
                    hasFile -> ui.statusSaved
                    else -> ui.localDraft
                },
                color = colors.secondary, fontSize = small, maxLines = 1
            )
        }
        Text("${ui.line} $line, ${ui.columnShort} $column", color = colors.faint, fontSize = small, maxLines = 1)
        Text("$words ${ui.words}", color = colors.faint, fontSize = small, maxLines = 1)
        message?.let { Text(it, Modifier.weight(1f, fill = false), color = colors.secondary, fontSize = small, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        Spacer(Modifier.weight(1f))
        Text(
            ui.syncedScroll,
            Modifier.clip(RoundedCornerShape(4.dp))
                .toggleable(value = syncScroll, role = Role.Switch, onValueChange = { onToggleSync() })
                .padding(horizontal = 4.dp, vertical = 2.dp),
            color = if (syncScroll) colors.accent else colors.faint, fontSize = small, maxLines = 1
        )
        Text("GFM", color = colors.faint, fontSize = small)
        Text("UTF-8", color = colors.faint, fontSize = small)
        Text(
            "${(zoom * 100).roundToInt()}%",
            Modifier.clip(RoundedCornerShape(4.dp)).clickable(onClick = onResetZoom).padding(horizontal = 4.dp, vertical = 2.dp),
            color = colors.faint, fontSize = small
        )
    }
}

/** Phone / narrow window: tabs on top, then a row of icon actions. */
@Composable
internal fun CompactTopBar(
    tabs: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onClose: (Int) -> Unit,
    onNew: () -> Unit,
    actions: ActionRegistry,
    previewActive: Boolean,
    onPreviewToggle: () -> Unit,
    searchOpen: Boolean,
    onSearchToggle: () -> Unit,
) {
    val colors = KomdTheme.colors
    val ui = LocalUiStrings.current
    Column(Modifier.fillMaxWidth().background(colors.chrome)) {
        Box(Modifier.fillMaxWidth().height(44.dp).bottomBorder(colors.divider).padding(horizontal = 8.dp)) {
            DocumentTabs(tabs, selectedIndex, onSelect, onClose, onNew, Modifier.fillMaxSize())
        }
        Row(
            Modifier.fillMaxWidth().height(40.dp).bottomBorder(colors.divider).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ActionButton(actions[ActionId.Open], 34.dp)
            ActionButton(actions[ActionId.Save], 34.dp)
            ActionButton(actions[ActionId.ExportHtml], 34.dp)
            if (pdfExportSupported) ActionButton(actions[ActionId.ExportPdf], 34.dp)
            Spacer(Modifier.weight(1f))
            ToggleIconButton(KomdIcons.Search, ui.find, searchOpen, onSearchToggle)
            ToggleIconButton(KomdIcons.Eye, ui.preview, previewActive, onPreviewToggle)
            ActionButton(actions[ActionId.CommandPalette], 34.dp)
            ActionButton(actions[ActionId.ToggleTheme], 34.dp)
            ActionButton(actions[ActionId.Settings], 34.dp)
        }
    }
}

@Composable
private fun ToggleIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    val colors = KomdTheme.colors
    Box(
        Modifier.size(34.dp).clip(RoundedCornerShape(6.dp))
            .background(if (active) colors.accentSoft else Color.Transparent)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center
    ) {
        KomdIcon(icon, if (active) colors.accent else colors.secondary)
    }
}

/** Phone formatting bar at the bottom, horizontally scrollable. */
@Composable
internal fun CompactFormatBar(actions: ActionRegistry) {
    val colors = KomdTheme.colors
    Row(
        Modifier.fillMaxWidth().background(colors.chrome).topBorder(colors.divider)
            .horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(
            ActionId.H1, ActionId.H2, ActionId.H3, ActionId.Bold, ActionId.Italic, ActionId.Link,
            ActionId.BulletList, ActionId.TaskList, ActionId.Quote, ActionId.InsertTable, ActionId.InsertImage, ActionId.InsertCodeBlock,
        ).forEach { ActionButton(actions[it], 38.dp) }
    }
}

@Composable
internal fun EmptyWorkspace(ui: AppStrings, onNew: () -> Unit, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val colors = KomdTheme.colors
    Column(
        modifier.background(colors.background).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(ui.noDocuments, color = colors.foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Text(ui.noDocumentsHint, color = colors.secondary, fontSize = 12.sp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                ui.newDocument,
                Modifier.clip(RoundedCornerShape(8.dp)).background(colors.accent).clickable(onClick = onNew).padding(horizontal = 14.dp, vertical = 8.dp),
                color = colors.onAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold
            )
            Text(
                ui.openFile,
                Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, colors.divider, RoundedCornerShape(8.dp))
                    .clickable(onClick = onOpen).padding(horizontal = 14.dp, vertical = 8.dp),
                color = colors.foreground, fontSize = 13.sp, fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
internal fun EditorPane(
    value: TextFieldValue,
    onChange: (TextFieldValue) -> Unit,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    highlight: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val colors = KomdTheme.colors
    val density = LocalDensity.current
    var editorHasFocus by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val measurer = rememberTextMeasurer(cacheSize = 64)
    // Editor text follows its own zoom, not the interface text size.
    val documentScale = LocalInterfaceScale.current.coerceAtLeast(0.5f)
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale / documentScale * zoom)) {
        val gutterStyle = TextStyle(color = colors.faint, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            visualTransformation = highlight,
            onTextLayout = { layout = it; onTextLayout(it) },
            modifier = modifier.background(colors.background)
                .verticalScroll(scrollState)
                .onFocusChanged { editorHasFocus = it.isFocused }
                .pointerInput(zoom, editorHasFocus) {
                    awaitPointerEventScope {
                        while (true) {
                            // Intercept modified wheel and wheel-button events before
                            // BasicTextField's selection/context-menu handling sees them.
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (editorHasFocus && event.keyboardModifiers.isMetaPressed) {
                                when (event.type) {
                                    PointerEventType.Scroll -> {
                                        val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                        if (scrollY != 0f) {
                                            val step = if (scrollY < 0f) 1 else -1
                                            onZoomChange(((zoom * 10).roundToInt() + step).coerceIn(7, 20) / 10f)
                                            event.changes.forEach { it.consume() }
                                        }
                                    }
                                    PointerEventType.Press, PointerEventType.Release -> {
                                        // Pressing the wheel sends a separate click event; without
                                        // consuming it, the text field can open its copy/paste menu.
                                        event.changes.forEach { it.consume() }
                                    }
                                    else -> Unit
                                }
                            }
                        }
                    }
                },
            textStyle = TextStyle(color = colors.editorText, fontSize = 13.5.sp, lineHeight = 25.sp, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(colors.accent),
            decorationBox = { inner ->
                Row(Modifier.padding(top = 22.dp, bottom = 22.dp)) {
                    // Line numbers drawn from the text layout, so they stay aligned with wrapped lines.
                    val current = layout
                    Box(
                        Modifier.width(52.dp)
                            .height(with(LocalDensity.current) { (current?.size?.height ?: 0).toDp() })
                            .drawBehind {
                                if (current == null) return@drawBehind
                                val text = current.layoutInput.text.text
                                var offset = 0
                                var number = 1
                                val right = size.width - 18.dp.toPx()
                                while (offset <= text.length) {
                                    val line = current.getLineForOffset(offset)
                                    val label = measurer.measure("$number", gutterStyle)
                                    val y = current.getLineTop(line) + (current.getLineBottom(line) - current.getLineTop(line) - label.size.height) / 2f
                                    drawText(label, topLeft = Offset(right - label.size.width, y))
                                    val next = text.indexOf('\n', offset)
                                    if (next < 0) break
                                    offset = next + 1
                                    number++
                                }
                            }
                    )
                    Box(Modifier.weight(1f).padding(end = 18.dp)) { inner() }
                }
            }
        )
    }
}

@Composable
internal fun PreviewPane(
    markdown: String,
    documentDirectory: String?,
    ui: AppStrings,
    zoom: Float,
    onZoomChange: (Float) -> Unit,
    scrollState: ScrollState,
    onToggleTask: (Int) -> Unit,
    modifier: Modifier = Modifier,
    search: PreviewSearch? = null,
    revealRequests: Int = 0
) {
    val colors = KomdTheme.colors
    val density = LocalDensity.current
    var contentCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    // Scroll so the current hit sits in the upper third; one frame lets the new highlight report its position.
    LaunchedEffect(revealRequests) {
        if (revealRequests == 0 || search == null) return@LaunchedEffect
        withFrameNanos { }
        val anchor = search.anchor
        val hit = anchor.coordinates?.takeIf { it.isAttached && anchor.ordinal >= 0 } ?: return@LaunchedEffect
        val content = contentCoordinates?.takeIf { it.isAttached } ?: return@LaunchedEffect
        val y = content.localPositionOf(hit, Offset(0f, anchor.top)).y
        scrollState.animateScrollTo((y - scrollState.viewportSize / 3f).roundToInt().coerceIn(0, scrollState.maxValue))
    }
    val documentScale = LocalInterfaceScale.current.coerceAtLeast(0.5f)
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale / documentScale * zoom)) {
        Column(
            modifier.background(colors.background).verticalScroll(scrollState)
                .pointerInput(zoom) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            if (event.type == PointerEventType.Scroll && event.keyboardModifiers.isMetaPressed) {
                                val scrollY = event.changes.firstOrNull()?.scrollDelta?.y ?: 0f
                                if (scrollY != 0f) {
                                    onZoomChange(((zoom * 10).roundToInt() + if (scrollY < 0f) 1 else -1).coerceIn(7, 20) / 10f)
                                    event.changes.forEach { it.consume() }
                                }
                            }
                        }
                    }
                }
        ) {
            Column(
                Modifier.widthIn(max = 820.dp).fillMaxWidth()
                    .onGloballyPositioned { contentCoordinates = it }
                    .padding(horizontal = 40.dp, vertical = 30.dp)
            ) {
                CompositionLocalProvider(LocalDocumentDirectory provides documentDirectory, LocalTaskToggle provides onToggleTask, LocalPreviewSearch provides search) {
                    MarkdownDocument(markdown, ui.imageUnsupported)
                }
            }
        }
    }
}

/** Draggable 1dp divider between editor and preview, with a wider invisible grab area. */
@Composable
internal fun Splitter(availableWidthPx: Float, onDrag: (Float) -> Unit) {
    val colors = KomdTheme.colors
    Box(
        Modifier.width(9.dp).fillMaxHeight().background(colors.background)
            .pointerHoverIcon(columnResizePointerIcon)
            .pointerInput(availableWidthPx) {
                detectDragGestures { _, dragAmount ->
                    if (availableWidthPx > 0f) onDrag(dragAmount.x / availableWidthPx)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(Modifier.width(1.dp).fillMaxHeight().background(colors.divider))
    }
}

@Composable
internal fun SettingsMenuRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = KomdTheme.colors
    Text(
        label,
        Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
            .background(if (selected) colors.accentSoft else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 9.dp, vertical = 10.dp),
        color = if (selected) colors.foreground else colors.secondary,
        fontSize = 12.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        maxLines = 1
    )
}

@Composable
internal fun SettingsOptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = KomdTheme.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(7.dp))
            .background(if (selected) colors.accentSoft else Color.Transparent)
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = colors.foreground, fontSize = 12.sp)
        Spacer(Modifier.weight(1f))
        if (selected) Text("✓", color = colors.accent, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun InterfaceScaleControl(scale: Float, onChange: (Float) -> Unit) {
    val colors = KomdTheme.colors
    Row(
        Modifier.clip(RoundedCornerShape(7.dp)).background(colors.hover),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "−",
            Modifier.clickable { onChange(((scale * 10).roundToInt() - 1).coerceAtLeast(9) / 10f) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = colors.foreground,
            fontSize = 16.sp
        )
        Text(
            "${(scale * 100).roundToInt()}%",
            Modifier.clickable { onChange(1f) }.padding(horizontal = 8.dp, vertical = 8.dp),
            color = colors.accent,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "+",
            Modifier.clickable { onChange(((scale * 10).roundToInt() + 1).coerceAtMost(18) / 10f) }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            color = colors.foreground,
            fontSize = 16.sp
        )
    }
}
