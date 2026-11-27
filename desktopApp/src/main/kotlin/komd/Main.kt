// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd

import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragData
import androidx.compose.ui.draganddrop.dragData
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
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
import java.net.URI
import javax.imageio.ImageIO

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
            // Without it Windows and Linux show the Java cup in the title bar and taskbar:
            // the installer icons (.ico/.png) only brand the launcher, not the AWT window.
            icon = appIcon,
            state = rememberWindowState(width = 1400.dp, height = 900.dp)
        ) {
            KomdMenuBar(commands)
            // Markdown files dropped on the window open in a new tab, like "Open With…".
            Box(Modifier.fillMaxSize().dragAndDropTarget(shouldStartDragAndDrop = { true }, target = fileDropTarget)) {
                App(commands = commands, onExit = ::exitApplication)
            }
        }
    }
}

// KoMD.png (1024 px) is also the Linux package icon; Compose scales it to each window icon size.
private val appIcon: Painter? by lazy {
    Thread.currentThread().contextClassLoader.getResourceAsStream("KoMD.png")
        ?.use { BitmapPainter(ImageIO.read(it).toComposeImageBitmap()) }
}

@OptIn(ExperimentalComposeUiApi::class)
private val fileDropTarget = object : DragAndDropTarget {
    override fun onDrop(event: DragAndDropEvent): Boolean {
        val files = event.dragData() as? DragData.FilesList ?: return false
        val paths = files.readFiles().mapNotNull { uri -> runCatching { File(URI(uri)).path }.getOrNull() }
        paths.forEach(::requestOpen)
        return paths.isNotEmpty()
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
