package komd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ImagePathsTest {
    private val docDir = "/Users/me/comelit/question_edit"

    @Test
    fun parentDirectoryOfFilePaths() {
        assertEquals("/Users/me/notes", parentDirectory("/Users/me/notes/a.md"))
        assertEquals("C:/docs", parentDirectory("C:\\docs\\a.md"))
        assertNull(parentDirectory("content://com.android.providers/document/42"))
    }

    @Test
    fun imageInSubfolderIsRelative() =
        assertEquals("questionimages/shot.png", relativeImagePath(docDir, "$docDir/questionimages/shot.png"))

    @Test
    fun nearbyImageUsesParentHops() =
        assertEquals("../assets/a.png", relativeImagePath(docDir, "/Users/me/comelit/assets/a.png"))

    @Test
    fun farImageStaysAbsolute() = assertNull(relativeImagePath("/a/b/c/d/e", "/x/y.png"))

    @Test
    fun otherDriveStaysAbsolute() = assertNull(relativeImagePath("C:/docs", "D:/img/a.png"))

    @Test
    fun spacesAndUnicodeArePercentEncoded() =
        assertEquals("my%20pics/caff%C3%A8.png", relativeImagePath(docDir, "$docDir/my pics/caffè.png"))

    @Test
    fun windowsBackslashesBecomeSlashes() =
        assertEquals("img/a.png", relativeImagePath("C:\\docs", "c:\\docs\\img\\a.png"))

    @Test
    fun insertInSavedDocumentIsRelative() =
        assertEquals("questionimages/s.png", imageSourceForInsert("$docDir/questionimages/s.png", docDir))

    @Test
    fun insertInUnsavedDocumentIsFileUri() =
        assertEquals("file:///Users/me/a%20b.png", imageSourceForInsert("/Users/me/a b.png", null))

    @Test
    fun insertKeepsContentUris() =
        assertEquals("content://x/1", imageSourceForInsert("content://x/1", docDir))

    @Test
    fun previewResolvesRelativeAgainstDocument() {
        assertEquals("file:///Users/me/comelit/question_edit/my%20pics/a.png", resolveImageModel("my%20pics/a.png", docDir))
        assertEquals("file:///Users/me/comelit/assets/a.png", resolveImageModel("../assets/a.png", docDir))
        assertEquals("file:///C:/docs/img/a.png", resolveImageModel("img/a.png", "C:/docs"))
        assertNull(resolveImageModel("img/a.png", null))
        assertEquals("https://x.y/a.png", resolveImageModel("https://x.y/a.png", docDir))
        assertEquals("file:///tmp/a.png", resolveImageModel("/tmp/a.png", null))
    }

    @Test
    fun firstSaveMakesAbsoluteSourcesRelative() {
        val md = """<img src="file:///Users/me/comelit/question_edit/questionimages/Screenshot%201.png" alt="Immagine" style="display:block;" />"""
        assertEquals(
            """<img src="questionimages/Screenshot%201.png" alt="Immagine" style="display:block;" />""",
            rebaseImageSources(md, null, docDir)
        )
    }

    @Test
    fun saveAsMovesRelativeSources() {
        val md = "![a](img/a.png) and ![b](<img/b c.png> \"t\")"
        assertEquals(
            "![a](../old/img/a.png) and ![b](<../old/img/b%20c.png> \"t\")",
            rebaseImageSources(md, "/w/old", "/w/new")
        )
    }

    @Test
    fun saveAsFarAwayTurnsRelativeIntoFileUri() =
        assertEquals("![a](file:///w/old/img/a.png)", rebaseImageSources("![a](img/a.png)", "/w/old", "/z/1/2/3/4"))

    @Test
    fun farAbsoluteSourceIsLeftUntouched() {
        val md = "<img src=\"file:///Volumes/ext/a.png\" />"
        assertEquals(md, rebaseImageSources(md, null, "/a/b/c/d/e"))
    }

    @Test
    fun remoteAndCodeBlocksAreLeftUntouched() {
        val md = "![r](https://x.y/a.png)\n```\n<img src=\"/w/doc/a.png\" />\n```\n<img src='/w/doc/a.png' />"
        assertEquals(
            "![r](https://x.y/a.png)\n```\n<img src=\"/w/doc/a.png\" />\n```\n<img src='a.png' />",
            rebaseImageSources(md, null, "/w/doc")
        )
    }

    @Test
    fun unsavedRelativeSourcesAreLeftUntouched() =
        assertEquals("![a](img/a.png)", rebaseImageSources("![a](img/a.png)", null, "/w/doc"))
}
