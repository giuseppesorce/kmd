package komd

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import komd.shared.AppCommands
import java.awt.Desktop

private val isMacOs = System.getProperty("os.name").orEmpty().contains("mac", ignoreCase = true)

/** Cmd on macOS, Ctrl on Windows/Linux. */
private fun shortcut(key: Key, shift: Boolean = false) =
    KeyShortcut(key, meta = isMacOs, ctrl = !isMacOs, shift = shift)

/**
 * File and Edit menus with the editor shortcuts. On macOS it is shown in the system menu bar
 * (see `apple.laf.useScreenMenuBar` in main), on Windows/Linux inside the window.
 */
@Composable
fun FrameWindowScope.KomdMenuBar(commands: AppCommands) {
    val labels = commands.labels
    MenuBar {
        Menu(labels.file, mnemonic = 'F') {
            Item(labels.newDocument, shortcut = shortcut(Key.N), onClick = commands::newDocument)
            Item(labels.open, shortcut = shortcut(Key.O), onClick = commands::open)
            Separator()
            Item(labels.close, shortcut = shortcut(Key.W), onClick = commands::close)
            Item(labels.save, shortcut = shortcut(Key.S), onClick = commands::save)
            Item(labels.saveAs, shortcut = shortcut(Key.S, shift = true), onClick = commands::saveAs)
            Separator()
            Item(labels.exportHtml, shortcut = shortcut(Key.E, shift = true), onClick = commands::exportHtml)
        }
        Menu(labels.edit, mnemonic = 'E') {
            Item(labels.find, shortcut = shortcut(Key.F), onClick = commands::find)
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
