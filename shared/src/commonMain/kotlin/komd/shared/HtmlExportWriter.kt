// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

package komd.shared

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.path
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.write
import io.github.vinceglb.filekit.writeString

/** Outcome of [writeHtmlExport]: images copied into `images/`, and images that could not be copied. */
internal data class HtmlExportResult(val copiedImages: Int, val skippedImages: Int)

/**
 * Writes [export] to [target] (the .html picked in the save dialog) and, when the page references
 * local images, copies them into an `images/` folder next to it.
 *
 * Images are copied only when [target] is a real filesystem path (desktop, iOS). On Android the save
 * dialog returns a content:// URI with no reachable parent folder, so the images are reported as
 * skipped and the page keeps its `images/…` links.
 */
internal suspend fun writeHtmlExport(target: PlatformFile, export: HtmlExport): HtmlExportResult {
    target.writeString(export.html)
    if (export.images.isEmpty()) return HtmlExportResult(0, 0)
    val folder = parentDirectory(target.path)?.let { PlatformFile(it) / EXPORT_IMAGES_DIR }
        ?: return HtmlExportResult(0, export.images.size)
    folder.createDirectories()
    var copied = 0
    export.images.forEach { image ->
        val ok = runCatching {
            val source = PlatformFile(image.sourcePath)
            if (!source.exists()) return@runCatching false
            (folder / image.fileName).write(source.readBytes())
            true
        }.getOrDefault(false)
        if (ok) copied++
    }
    return HtmlExportResult(copied, export.images.size - copied)
}
