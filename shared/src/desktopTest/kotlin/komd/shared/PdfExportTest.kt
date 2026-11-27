package komd.shared

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PdfExportTest {

    private val sample = """
        # Relazione trimestrale

        Testo con **grassetto**, *corsivo*, `codice` e accenti: perché, città, già.

        - [x] fatto
        - [ ] da fare

        | Voce | Valore |
        |------|-------:|
        | Uno  |     10 |

        ```kotlin
        fun main() = println("ciao")
        ```

        > Una citazione.
    """.trimIndent()

    @Test
    fun rendersAPdfWithTheDocumentText() = runBlocking {
        val bytes = renderPdf(buildHtmlExport(sample, "Relazione", null, ExportMedium.Print).html, null)
        assertEquals("%PDF", bytes.copyOfRange(0, 4).decodeToString())
        Loader.loadPDF(bytes).use { pdf ->
            val text = PDFTextStripper().getText(pdf)
            assertTrue("Relazione trimestrale" in text, text)
            assertTrue("perché, città, già" in text, text)
            assertTrue("println(\"ciao\")" in text, text)
        }
    }

    @Test
    fun longDocumentsSpanSeveralPages() = runBlocking {
        val md = (1..120).joinToString("\n\n") { "Paragrafo numero $it con un po' di testo per riempire la riga." }
        val bytes = renderPdf(buildHtmlExport(md, "Lungo", null, ExportMedium.Print).html, null)
        Loader.loadPDF(bytes).use { pdf -> assertTrue(pdf.numberOfPages > 1) }
    }

    @Test
    fun localImagesAreEmbedded() = runBlocking {
        val docs = Files.createTempDirectory("komd-pdf").toFile()
        val png = File(docs, "dot.png")
        javax.imageio.ImageIO.write(java.awt.image.BufferedImage(40, 20, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", png)
        val pending = PendingPdf(buildHtmlExport("![Dot](dot.png)", "Img", docs.absolutePath, ExportMedium.Print).html, directoryFileUri(docs.absolutePath))
        val target = File(docs, "out.pdf")
        writePdfExport(PlatformFile(target), pending)
        Loader.loadPDF(target).use { pdf ->
            val images = pdf.getPage(0).resources.xObjectNames.count { pdf.getPage(0).resources.isImageXObject(it) }
            assertEquals(1, images)
        }
    }
}
