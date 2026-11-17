package komd

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.vinceglb.filekit.PlatformFile
import komd.shared.App
import komd.shared.ExternalOpenRequests
import java.awt.Desktop
import java.io.File

fun main(args: Array<String>) {
    // Must be registered before the UI starts: on macOS the "open document" Apple Event of a cold
    // launch (Finder "Open With…", double click) is delivered right after startup and is lost if
    // no handler is installed yet. Later events (app already running) go through the same handler.
    registerOpenFileHandler()
    // Windows/Linux file associations and `KoMD file.md` from a terminal pass the paths as arguments.
    args.forEach(::requestOpen)

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "KoMD",
            state = rememberWindowState(width = 1400.dp, height = 900.dp)
        ) {
            App()
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
