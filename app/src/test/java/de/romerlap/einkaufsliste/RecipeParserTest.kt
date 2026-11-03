package de.romerlap.einkaufsliste

import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeParserTest {

    @Test
    fun `extrahiert Menge mit Einheit`() {
        val result = RecipeParser.extractIngredients("200 g Mehl")
        assertEquals(1, result.size)
        assertEquals("Mehl", result[0].name)
        assertEquals("200 g", result[0].quantity)
    }

    @Test
    fun `extrahiert Menge ohne Einheit`() {
        val result = RecipeParser.extractIngredients("2 Eier")
        assertEquals(1, result.size)
        assertEquals("Eier", result[0].name)
        assertEquals("2", result[0].quantity)
    }

    @Test
    fun `ignoriert Ueberschriften und Zubereitung`() {
        val text = """
            Zutaten:
            100 ml Milch
            Die Milch erhitzen und mit dem Mehl verrühren.
        """.trimIndent()
        val result = RecipeParser.extractIngredients(text)
        assertEquals(1, result.size)
        assertEquals("Milch", result[0].name)
    }

    @Test
    fun `entfernt Aufzaehlungszeichen`() {
        val result = RecipeParser.extractIngredients("- 1 Bund Petersilie")
        assertEquals(1, result.size)
        assertEquals("Petersilie", result[0].name)
        assertEquals("1 Bund", result[0].quantity)
    }

    @Test
    fun `entfernt Duplikate`() {
        val result = RecipeParser.extractIngredients("Tomaten\nTomaten")
        assertEquals(1, result.size)
    }
}
