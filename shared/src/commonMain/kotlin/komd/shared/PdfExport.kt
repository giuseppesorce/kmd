package komd.shared

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.write

/*
 * Markdown → PDF export.
 *
 * Flow: [buildHtmlExport] with [ExportMedium.Print] (pure, shared) → [renderPdf] (per platform) →
 * the bytes are written to the file picked in the save dialog.
 *
 * Only the desktop app renders PDFs for now (OpenHTMLtoPDF on the JVM); on Android and iOS
 * [pdfExportSupported] is false and the "Export PDF" entries are hidden.
 */

/** True where [renderPdf] is implemented. */
internal expect val pdfExportSupported: Boolean

/**
 * Renders [html] (a page built with [ExportMedium.Print]) as a PDF document. [baseUri] resolves the
 * relative URLs left in the page, if any (the folder of the .md as a file:// URI, or null).
 */
internal expect suspend fun renderPdf(html: String, baseUri: String?): ByteArray

/** Print page prepared when the save dialog opens; [baseUri] is the .md folder as file:// URI. */
internal data class PendingPdf(val html: String, val baseUri: String?)

/** Renders [pdf] and writes it to [target] (the .pdf picked in the save dialog). */
internal suspend fun writePdfExport(target: PlatformFile, pdf: PendingPdf) {
    target.write(renderPdf(pdf.html, pdf.baseUri))
}
