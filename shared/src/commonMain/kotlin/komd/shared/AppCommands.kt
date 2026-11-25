package komd.shared

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Bridge between the platform shell (desktop menu bar, window close button, quit handler)
 * and the editor state that lives inside [App].
 *
 * The shell calls the commands; [App] registers the actual implementations while it is in
 * composition and publishes localized [labels] for the menu. When a shell passes an instance
 * to [App], the shell owns the keyboard shortcuts (menu accelerators) and App stops handling
 * them itself, so each shortcut fires exactly once.
 */
class AppCommands {
    internal var target: Target? = null

    /** Menu labels in the current UI language. */
    var labels: MenuLabels by mutableStateOf(MenuLabels.fromStrings(italianStrings))
        internal set

    fun newDocument() { target?.newDocument?.invoke() }
    fun open() { target?.open?.invoke() }
    fun save() { target?.save?.invoke() }
    fun saveAs() { target?.saveAs?.invoke() }

    /** Exports the current document as HTML (plus an `images/` folder when it has local images). */
    fun exportHtml() { target?.exportHtml?.invoke() }

    /** Exports the current document as PDF (desktop only for now). */
    fun exportPdf() { target?.exportPdf?.invoke() }

    /** Opens (or focuses) the in-document search field. */
    fun find() { target?.find?.invoke() }

    /** Closes the current document, or asks to exit when no document is open. */
    fun close() { target?.close?.invoke() }

    /** Exit request from the window close button or the system quit command: asks for confirmation when there are unsaved changes. */
    fun requestExit() { target?.requestExit?.invoke() }

    internal class Target(
        val newDocument: () -> Unit,
        val open: () -> Unit,
        val save: () -> Unit,
        val saveAs: () -> Unit,
        val exportHtml: () -> Unit,
        val exportPdf: () -> Unit,
        val close: () -> Unit,
        val requestExit: () -> Unit,
        val find: () -> Unit,
    )
}

data class MenuLabels(
    val file: String,
    val newDocument: String,
    val open: String,
    val close: String,
    val save: String,
    val saveAs: String,
    val exportHtml: String,
    val exportPdf: String,
    val edit: String,
    val find: String,
) {
    internal companion object {
        fun fromStrings(strings: AppStrings) = MenuLabels(
            file = strings.menuFile,
            newDocument = strings.newDocument,
            open = strings.openFile,
            close = strings.closeDocument,
            save = strings.save,
            saveAs = strings.saveAs,
            exportHtml = strings.exportHtml,
            exportPdf = strings.exportPdf,
            edit = strings.menuEdit,
            find = strings.find,
        )
    }
}
