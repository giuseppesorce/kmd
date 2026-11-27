// SPDX-FileCopyrightText: 2026 Giuseppe Sorce
// SPDX-License-Identifier: GPL-3.0-only

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

class PreviewSearchCountTest {
    @Test
    fun countsRenderedTextNotMarkdownSyntax() {
        val blocks = parseMarkdown("# Kotlin\n\n**Kotlin** e [link](https://kotlin.org)\n\n- kotlin\n- altro")
        assertEquals(listOf(1, 1, 1), blocks.map { searchCount(it, "kotlin") })
    }

    @Test
    fun countsQuotesTablesAndCode() {
        val md = "> ab ab\n\n| ab | x |\n| --- | --- |\n| y | ab |\n\n```\nab\n```"
        assertEquals(5, parseMarkdown(md).sumOf { searchCount(it, "ab") })
    }

    @Test
    fun shortQueryCountsNothing() {
        assertEquals(0, parseMarkdown("aaa").sumOf { searchCount(it, "a") })
    }
}
