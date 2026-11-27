// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

internal actual val pdfExportSupported: Boolean = false

internal actual suspend fun renderPdf(html: String, baseUri: String?): ByteArray =
    throw UnsupportedOperationException("PDF export is available only on desktop")
