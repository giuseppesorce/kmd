package komd.shared

import androidx.compose.ui.input.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals

class KomdShortcutTest {
    @Test
    fun labelsFollowPlatformConventions() {
        val saveAs = KomdShortcut(Key.S, "S", shift = true)
        assertEquals("⇧⌘S", saveAs.label(useCommand = true))
        assertEquals("Ctrl+Shift+S", saveAs.label(useCommand = false))
        assertEquals("⌥⌘K", KomdShortcut(Key.K, "K", alt = true).label(useCommand = true))
    }

    @Test
    fun menusInsertSeparatorsBetweenSections() {
        val registry = ActionRegistry(
            listOf(
                KomdAction(ActionId.NewDocument, "Nuovo", KomdMenu.File, 0) {},
                KomdAction(ActionId.Open, "Apri", KomdMenu.File, 0) {},
                KomdAction(ActionId.Save, "Salva", KomdMenu.File, 1, KomdShortcut(Key.S, "S")) {},
                KomdAction(ActionId.CommandPalette, "Palette", null) {},
            )
        )
        val menus = registry.menus { it.name }
        assertEquals(listOf("File"), menus.map { it.title })
        assertEquals(listOf(false, false, true), menus.single().items.map { it.separatorBefore })
        assertEquals(AppShortcut(Key.S, shift = false, alt = false), menus.single().items.last().shortcut)
    }
}
