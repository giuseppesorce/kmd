package komd.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Bridge between the platform shell (desktop menu bar, window close button, quit handler)
 * and the editor state that lives inside [App].
 *
 * [App] publishes its action registry as [menus]: the shell renders them as native menus and
 * their accelerators become the keyboard shortcuts. When a shell passes an instance to [App],
 * App stops handling shortcuts itself, so each shortcut fires exactly once.
 */
class AppCommands {
    /** Native menus in the current UI language, derived from the app's action registry. */
    var menus: List<AppMenu> by mutableStateOf(emptyList())
        internal set

    internal var exitHandler: (() -> Unit)? = null

    /** Exit request from the window close button or the system quit command: asks for confirmation when there are unsaved changes. */
    fun requestExit() { exitHandler?.invoke() }
}
