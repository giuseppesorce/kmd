package komd.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class RecentFilesTest {
    private fun file(n: Int) = RecentFile("/docs/f$n.md", "f$n.md")

    @Test
    fun newEntryGoesOnTopAndListIsCapped() {
        val list = (1..5).fold(emptyList<RecentFile>()) { acc, n -> acc.withRecent(file(n)) }
        assertEquals(listOf(5, 4, 3, 2, 1).map(::file), list)
        val updated = list.withRecent(file(6))
        assertEquals(MAX_RECENT_FILES, updated.size)
        assertEquals(listOf(6, 5, 4, 3, 2).map(::file), updated)
    }

    @Test
    fun reopeningMovesEntryToTopWithoutDuplicates() {
        val list = listOf(file(1), file(2), file(3))
        val renamed = RecentFile("/docs/f3.md", "f3.md", bookmark = "abc")
        assertEquals(listOf(renamed, file(1), file(2)), list.withRecent(renamed))
    }

    @Test
    fun removeByPath() {
        assertEquals(listOf(file(1), file(3)), listOf(file(1), file(2), file(3)).withoutRecent("/docs/f2.md"))
    }

    @Test
    fun encodeDecodeRoundTripWithSpecialCharacters() {
        val list = listOf(
            RecentFile("/Users/me/Note %20 50%.md", "Note %20 50%.md", "AAEC/+=="),
            RecentFile("C:\\Docs\\tab\there.md", "tab\there.md"),
            RecentFile("content://com.android/doc/1%3A2", "doc.md", "Y29udGVudA=="),
        )
        assertEquals(list, decodeRecentFiles(encodeRecentFiles(list)))
    }

    @Test
    fun decodeSkipsGarbageAndMissingValues() {
        assertEquals(emptyList(), decodeRecentFiles(null))
        assertEquals(emptyList(), decodeRecentFiles(""))
        val decoded = decodeRecentFiles("garbage\n/a/b.md\t\t\n\t\tx")
        assertEquals(listOf(RecentFile("/a/b.md", "b.md", null)), decoded)
    }
}
