package kmd

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kmd.shared.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "KMD") {
        App()
    }
}
