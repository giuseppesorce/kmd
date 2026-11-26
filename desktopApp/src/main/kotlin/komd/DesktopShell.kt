package komd

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import komd.shared.AppCommands
import komd.shared.AppShortcut
import java.awt.Desktop

private val isMacOs = System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)

/** Cmd on macOS, Ctrl on Windows/Linux. */
private fun AppShortcut.toKeyShortcut() = KeyShortcut(key, meta = isMacOs, ctrl = !isMacOs, shift = shift, alt = alt)

/**
 * Native menus rendered from the app's action registry (labels, shortcuts and handlers all come
 * from [AppCommands.menus]). On macOS it is shown in the system menu bar (see
 * `apple.laf.useScreenMenuBar` in main), on Windows/Linux inside the window.
 */
@Composable
fun FrameWindowScope.KomdMenuBar(commands: AppCommands) {
    val menus = commands.menus
    if (menus.isEmpty()) return
    MenuBar {
        menus.forEach { menu ->
            Menu(menu.title) {
                menu.items.forEach { item ->
                    if (item.separatorBefore) Separator()
                    val checked = item.checked
                    if (checked != null) {
                        CheckboxItem(
                            item.label,
                            checked = checked,
                            enabled = item.enabled,
                            shortcut = item.shortcut?.toKeyShortcut(),
                            onCheckedChange = { item.onClick() }
                        )
                    } else {
                        Item(item.label, enabled = item.enabled, shortcut = item.shortcut?.toKeyShortcut(), onClick = item.onClick)
                    }
                }
            }
        }
    }
}

/**
 * Routes the macOS "Quit" command (Cmd+Q / app menu) through [AppCommands.requestExit],
 * so unsaved changes are confirmed instead of the JVM exiting immediately.
 */
@Composable
fun InstallQuitHandler(commands: AppCommands) {
    DisposableEffect(commands) {
        val desktop = Desktop.getDesktop().takeIf {
            Desktop.isDesktopSupported() && it.isSupported(Desktop.Action.APP_QUIT_HANDLER)
        }
        desktop?.setQuitHandler { _, response ->
            response.cancelQuit()
            commands.requestExit()
        }
        onDispose { desktop?.setQuitHandler(null) }
    }
}
