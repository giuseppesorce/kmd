package komd

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import komd.shared.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "KoMD") {
        App()
    }
}
