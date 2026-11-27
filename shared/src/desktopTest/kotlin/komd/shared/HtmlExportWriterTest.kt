package komd.shared

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HtmlExportWriterTest {

    @Test
    fun withoutImagesOnlyTheHtmlFileIsWritten() = runBlocking {
        val dir = Files.createTempDirectory("komd-export").toFile()
        val target = File(dir, "out/page.html").also { it.parentFile.mkdirs() }
        val export = buildHtmlExport("# Hi", "page", dir.absolutePath)
        val result = writeHtmlExport(PlatformFile(target), export)
        assertEquals(HtmlExportResult(0, 0), result)
        assertTrue(target.readText().contains("<h1>Hi</h1>"))
        assertFalse(File(target.parentFile, EXPORT_IMAGES_DIR).exists())
    }

    @Test
    fun localImagesAreCopiedIntoImagesFolder() = runBlocking {
        val docs = Files.createTempDirectory("komd-doc").toFile()
        val bytes = byteArrayOf(1, 2, 3, 4)
        File(docs, "assets").mkdirs()
        File(docs, "assets/my pic.png").writeBytes(bytes)
        val md = "![a](assets/my%20pic.png)\n\n![missing](assets/nope.png)\n\n![remote](https://example.com/x.png)"
        val out = Files.createTempDirectory("komd-out").toFile()
        val target = File(out, "doc.html")
        val result = writeHtmlExport(PlatformFile(target), buildHtmlExport(md, "doc", docs.absolutePath))
        assertEquals(HtmlExportResult(copiedImages = 1, skippedImages = 1), result)
        assertContentEquals(bytes, File(out, "images/my pic.png").readBytes())
        assertTrue(target.readText().contains("src=\"images/my%20pic.png\""))
    }
}
