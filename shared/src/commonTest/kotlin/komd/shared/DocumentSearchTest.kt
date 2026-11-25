package komd.shared

import kotlin.test.Test
import kotlin.test.assertEquals

class DocumentSearchTest {
    @Test
    fun queryShorterThanTwoCharsFindsNothing() {
        assertEquals(emptyList(), findMatches("a b a", "a"))
        assertEquals(emptyList(), findMatches("abc", ""))
    }

    @Test
    fun findsAllOccurrencesIgnoringCase() {
        assertEquals(listOf(0 until 6, 10 until 16), findMatches("Kotlin vs kotlin", "KOTLIN"))
    }

    @Test
    fun matchesDoNotOverlap() {
        assertEquals(listOf(0 until 2, 2 until 4), findMatches("aaaaa", "aa"))
    }

    @Test
    fun firstMatchFromCaretWraps() {
        val matches = findMatches("ab xx ab xx ab", "ab")
        assertEquals(1, firstMatchFrom(matches, 4))
        assertEquals(0, firstMatchFrom(matches, 13))
        assertEquals(-1, firstMatchFrom(emptyList(), 0))
    }

    @Test
    fun stepWrapsBothWays() {
        val matches = findMatches("ab ab ab", "ab")
        assertEquals(1, stepMatch(matches, 0))
        assertEquals(0, stepMatch(matches, 2))
        assertEquals(2, stepMatch(matches, 0, forward = false))
        assertEquals(-1, stepMatch(emptyList(), 0))
    }
}
