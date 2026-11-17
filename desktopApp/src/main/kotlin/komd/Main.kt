package komd

import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.vinceglb.filekit.PlatformFile
import komd.shared.App
import komd.shared.AppCommands
import komd.shared.ExternalOpenRequests
import java.awt.Desktop
import java.io.File

fun main(args: Array<String>) {
    // Show the Compose MenuBar in the macOS system menu bar instead of inside the window.
    // Set before anything touches AWT (Desktop.getDesktop() below).
    System.setProperty("apple.laf.useScreenMenuBar", "true")

    // Must be registered before the UI starts: on macOS the "open document" Apple Event of a cold
    // launch (Finder "Open With…", double click) is delivered right after startup and is lost if
    // no handler is installed yet. Later events (app already running) go through the same handler.
    registerOpenFileHandler()
    // Windows/Linux file associations and `KoMD file.md` from a terminal pass the paths as arguments.
    args.forEach(::requestOpen)

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

private fun registerOpenFileHandler() {
    if (!Desktop.isDesktopSupported()) return
    val desktop = Desktop.getDesktop()
    if (!desktop.isSupported(Desktop.Action.APP_OPEN_FILE)) return
    desktop.setOpenFileHandler { event -> event.files.forEach { requestOpen(it.path) } }
}

private val markdownExtensions = setOf("md", "markdown", "mdown", "mkd", "mkdn")

private fun requestOpen(path: String) {
    // Ignore JVM/launcher flags such as -psn_0_12345 that macOS may append on older systems.
    if (path.startsWith("-")) return
    val file = File(path).absoluteFile
    if (!file.isFile || file.extension.lowercase() !in markdownExtensions) return
    ExternalOpenRequests.open(PlatformFile(file))
}
