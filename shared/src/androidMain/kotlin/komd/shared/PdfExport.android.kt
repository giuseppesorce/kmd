package komd.shared

internal actual val pdfExportSupported: Boolean = false

internal actual suspend fun renderPdf(html: String, baseUri: String?): ByteArray =
    throw UnsupportedOperationException("PDF export is available only on desktop")
