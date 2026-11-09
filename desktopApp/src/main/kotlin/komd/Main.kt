package komd

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import komd.shared.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KoMD",
        state = rememberWindowState(width = 1400.dp, height = 900.dp)
    ) {
        App()
    }
}
