package komd

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import komd.shared.App
import komd.shared.AppCommands

fun main() {
    // Show the Compose MenuBar in the macOS system menu bar instead of inside the window.
    System.setProperty("apple.laf.useScreenMenuBar", "true")
    application {
        val commands = remember { AppCommands() }
        InstallQuitHandler(commands)
        Window(
            onCloseRequest = commands::requestExit,
            title = "KoMD",
            state = rememberWindowState(width = 1400.dp, height = 900.dp)
        ) {
            KomdMenuBar(commands)
            App(commands = commands, onExit = ::exitApplication)
        }
    }
}
