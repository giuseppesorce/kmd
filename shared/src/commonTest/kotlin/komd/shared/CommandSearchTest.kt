package komd.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommandSearchTest {
    @Test
    fun emptyQueryMatchesEverything() {
        assertEquals(0, commandMatchScore("Salva", ""))
        assertEquals(listOf("a", "b"), rankByQuery(listOf("a", "b"), "  ") { it })
    }

    @Test
    fun nonSubsequenceDoesNotMatch() {
        assertNull(commandMatchScore("Salva", "xyz"))
        assertNull(commandMatchScore("Salva", "avs"))
    }

    @Test
    fun matchIsCaseAndAccentInsensitive() {
        assertNotNull(commandMatchScore("Attività", "ATTIVITA"))
        assertNotNull(commandMatchScore("Aperçu", "apercu"))
    }

    @Test
    fun prefixAndWordStartsRankFirst() {
        val ranked = rankByQuery(listOf("Esporta HTML…", "Salva con nome…", "Salva"), "salva") { it }
        assertEquals(listOf("Salva con nome…", "Salva"), ranked)
        val byWord = rankByQuery(listOf("Elenco puntato", "Titolo 1", "Esporta PDF…"), "pdf") { it }
        assertEquals("Esporta PDF…", byWord.first())
    }

    @Test
    fun contiguousBeatsScattered() {
        val contiguous = commandMatchScore("Grassetto", "gra")!!
        val scattered = commandMatchScore("Gestisci ricerca avanzata", "gra")!!
        assertTrue(contiguous > scattered)
    }

    @Test
    fun tiesKeepOriginalOrder() {
        assertEquals(listOf("Salva", "Salvb"), rankByQuery(listOf("Salva", "Salvb"), "sal") { it })
    }
}
