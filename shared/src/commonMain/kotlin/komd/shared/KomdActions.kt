// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * Single registry of user actions.
 *
 * Every command of the app (file, formatting, view…) is declared once as a [KomdAction] with its
 * label, shortcut, icon and handler. Native menu, toolbar, rail, command palette and keyboard
 * shortcuts are all *projections* of the same list, so a label or shortcut can never drift
 * between surfaces and there is exactly one handler per command.
 */
internal enum class ActionId {
    // File
    NewDocument, Open, ClearRecentFiles, Save, SaveAs, Close, ExportHtml, ExportPdf, Settings,
    // Edit
    Find,
    // Format
    H1, H2, H3, Bold, Italic, Strikethrough, InlineCode, Link,
    BulletList, NumberedList, TaskList, Quote,
    InsertTable, InsertImage, InsertCodeBlock, InsertLineBreak, InsertHorizontalRule, InsertCustomLink,
    // View
    CommandPalette, ToggleSidebar, ShowFiles, ShowOutline, ShowExport,
    ViewEditor, ViewSplit, ViewPreview, ToggleSyncScroll,
    ZoomIn, ZoomOut, ZoomReset, ToggleTheme, NextTab, PreviousTab,
}

internal enum class KomdMenu { File, Edit, Format, View }

/** A shortcut with the platform modifier (⌘ on Apple, Ctrl elsewhere) plus optional Shift/Alt. */
internal data class KomdShortcut(val key: Key, val keyLabel: String, val shift: Boolean = false, val alt: Boolean = false) {
    fun label(useCommand: Boolean = shortcutUsesCommand): String = if (useCommand) {
        buildString {
            if (alt) append('⌥')
            if (shift) append('⇧')
            append('⌘')
            append(keyLabel)
        }
    } else {
        buildString {
            append("Ctrl+")
            if (alt) append("Alt+")
            if (shift) append("Shift+")
            append(keyLabel)
        }
    }

    fun matches(event: KeyEvent, useCommand: Boolean = shortcutUsesCommand): Boolean {
        val primary = if (useCommand) event.isMetaPressed else event.isCtrlPressed
        return primary && event.key == key && event.isShiftPressed == shift && event.isAltPressed == alt
    }

    fun toPublic() = AppShortcut(key, shift, alt)
}

internal class KomdAction(
    val id: ActionId,
    val label: String,
    /** Menu that shows it; null = palette/toolbar only. */
    val menu: KomdMenu?,
    /** Items with different sections inside the same menu are divided by a separator. */
    val section: Int = 0,
    val shortcut: KomdShortcut? = null,
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    /** Checked state for toggles (view mode, sync scroll…); null when not a toggle. */
    val checked: Boolean? = null,
    val run: () -> Unit,
)

internal class ActionRegistry(val all: List<KomdAction>) {
    private val byId = all.associateBy { it.id }

    operator fun get(id: ActionId): KomdAction = byId.getValue(id)

    fun invoke(id: ActionId) {
        byId[id]?.takeIf { it.enabled }?.run?.invoke()
    }

    /** Runs the action bound to [event], if any. Used when no native menu owns the accelerators. */
    fun handleKey(event: KeyEvent): Boolean {
        if (event.type != KeyEventType.KeyDown) return false
        val action = all.firstOrNull { it.enabled && it.shortcut?.matches(event) == true } ?: return false
        action.run()
        return true
    }

    /** Public, platform-neutral snapshot of the menus, rendered by the desktop shell. */
    fun menus(titles: (KomdMenu) -> String): List<AppMenu> = KomdMenu.entries.mapNotNull { menu ->
        val items = all.filter { it.menu == menu }
        if (items.isEmpty()) return@mapNotNull null
        AppMenu(
            title = titles(menu),
            items = items.mapIndexed { index, action ->
                AppMenuItem(
                    label = action.label,
                    shortcut = action.shortcut?.toPublic(),
                    enabled = action.enabled,
                    checked = action.checked,
                    separatorBefore = index > 0 && items[index - 1].section != action.section,
                    onClick = action.run,
                )
            }
        )
    }
}

/** Menu model exposed to the platform shell (see desktop `KomdMenuBar`). */
data class AppMenu(val title: String, val items: List<AppMenuItem>)

data class AppMenuItem(
    val label: String,
    val shortcut: AppShortcut?,
    val enabled: Boolean,
    val checked: Boolean?,
    val separatorBefore: Boolean,
    val onClick: () -> Unit,
)

/** [key] plus the platform modifier (⌘ / Ctrl), optionally with Shift and Alt. */
data class AppShortcut(val key: Key, val shift: Boolean, val alt: Boolean)
