package komd.shared

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.helper.W3CDom
import java.io.ByteArrayOutputStream
import java.io.File

internal actual val pdfExportSupported: Boolean = true

/*
 * HTML5 → jsoup → W3C DOM → OpenHTMLtoPDF (PDFBox). The engine needs a well-formed XML DOM, which
 * jsoup builds from the HTML5 page (void tags, unquoted attributes…).
 *
 * Fonts: without embedded fonts the engine falls back to the PDF base-14 fonts, which only cover
 * Latin-1. The writer therefore embeds TrueType fonts found on the system under the families used by
 * the print stylesheet: "KoMd Sans", "KoMd Mono" and "KoMd Unicode" (wide coverage, per-glyph fallback).
 * Only subsets of the used glyphs end up in the PDF.
 */
internal actual suspend fun renderPdf(html: String, baseUri: String?): ByteArray = withContext(Dispatchers.IO) {
    val document = W3CDom().fromJsoup(Jsoup.parse(html, baseUri.orEmpty()))
    val out = ByteArrayOutputStream()
    PdfRendererBuilder()
        .useFastMode()
        .withW3cDocument(document, baseUri)
        .apply { registerSystemFonts(this) }
        .toStream(out)
        .run()
    out.toByteArray()
}

/** One family: regular, bold, italic, bold italic candidates (first existing file wins for each face). */
private class FontFamily(val name: String, val regular: List<String>, val bold: List<String> = emptyList(),
                         val italic: List<String> = emptyList(), val boldItalic: List<String> = emptyList())

private val macFonts = "/System/Library/Fonts/Supplemental"
private val winFonts = (System.getenv("WINDIR") ?: "C:\\Windows") + "\\Fonts"
private val linuxFonts = "/usr/share/fonts/truetype"

private val fontFamilies = listOf(
    FontFamily(
        "KoMd Sans",
        regular = listOf("$macFonts/Arial.ttf", "$winFonts\\arial.ttf", "$linuxFonts/dejavu/DejaVuSans.ttf", "$linuxFonts/liberation/LiberationSans-Regular.ttf"),
        bold = listOf("$macFonts/Arial Bold.ttf", "$winFonts\\arialbd.ttf", "$linuxFonts/dejavu/DejaVuSans-Bold.ttf", "$linuxFonts/liberation/LiberationSans-Bold.ttf"),
        italic = listOf("$macFonts/Arial Italic.ttf", "$winFonts\\ariali.ttf", "$linuxFonts/dejavu/DejaVuSans-Oblique.ttf", "$linuxFonts/liberation/LiberationSans-Italic.ttf"),
        boldItalic = listOf("$macFonts/Arial Bold Italic.ttf", "$winFonts\\arialbi.ttf", "$linuxFonts/dejavu/DejaVuSans-BoldOblique.ttf", "$linuxFonts/liberation/LiberationSans-BoldItalic.ttf"),
    ),
    FontFamily(
        "KoMd Mono",
        regular = listOf("$winFonts\\consola.ttf", "$linuxFonts/dejavu/DejaVuSansMono.ttf", "$macFonts/Courier New.ttf", "$linuxFonts/liberation/LiberationMono-Regular.ttf"),
        bold = listOf("$winFonts\\consolab.ttf", "$linuxFonts/dejavu/DejaVuSansMono-Bold.ttf", "$macFonts/Courier New Bold.ttf", "$linuxFonts/liberation/LiberationMono-Bold.ttf"),
        italic = listOf("$winFonts\\consolai.ttf", "$linuxFonts/dejavu/DejaVuSansMono-Oblique.ttf", "$macFonts/Courier New Italic.ttf"),
        boldItalic = listOf("$winFonts\\consolaz.ttf", "$linuxFonts/dejavu/DejaVuSansMono-BoldOblique.ttf", "$macFonts/Courier New Bold Italic.ttf"),
    ),
    // Wide-coverage fallback for glyphs missing above (Greek, Cyrillic, symbols, CJK where available).
    FontFamily(
        "KoMd Unicode",
        regular = listOf("$macFonts/Arial Unicode.ttf", "$winFonts\\seguisym.ttf", "$linuxFonts/dejavu/DejaVuSans.ttf"),
    ),
)

private fun registerSystemFonts(builder: PdfRendererBuilder) {
    fontFamilies.forEach { family ->
        listOf(
            Triple(family.regular, 400, FontStyle.NORMAL),
            Triple(family.bold, 700, FontStyle.NORMAL),
            Triple(family.italic, 400, FontStyle.ITALIC),
            Triple(family.boldItalic, 700, FontStyle.ITALIC),
        ).forEach { (candidates, weight, style) ->
            candidates.asSequence().map(::File).firstOrNull { it.isFile }
                ?.let { builder.useFont(it, family.name, weight, style, true) }
        }
    }
}
