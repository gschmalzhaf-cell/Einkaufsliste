package de.romerlap.einkaufsliste

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartIntelligenceTest {

    @Test
    fun aisleClassifier_recognizesGermanSupermarketAisles() {
        assertEquals(Aisle.PRODUCE, AisleClassifier.classify("Karotten"))
        assertEquals(Aisle.DAIRY, AisleClassifier.classify("Frische Milch"))
        assertEquals(Aisle.BAKERY, AisleClassifier.classify("Sauerteigbrot"))
        assertEquals(Aisle.BAKING_SPICES, AisleClassifier.classify("Weizenmehl"))
        assertEquals(Aisle.DRINKS, AisleClassifier.classify("Orangensaft"))
        assertEquals(Aisle.HOUSEHOLD, AisleClassifier.classify("Spülmittel"))
    }

    @Test
    fun smartInputParser_extractsQuantityNameAndTag() {
        val p1 = SmartInputParser.parse("2 kg Kartoffeln #Sonntag")
        assertEquals("Kartoffeln", p1.name)
        assertEquals("2 kg", p1.quantity)
        assertEquals("Sonntag", p1.tag)

        val p2 = SmartInputParser.parse("3x Paprika")
        assertEquals("Paprika", p2.name)
        assertEquals("3 Stück", p2.quantity)

        val p3 = SmartInputParser.parse("500 ml Milch")
        assertEquals("Milch", p3.name)
        assertEquals("500 ml", p3.quantity)
    }

    @Test
    fun quantityMerger_addsCompatibleUnits() {
        assertEquals("500 g", QuantityMerger.merge("200 g", "300 g"))
        assertEquals("1,5 kg", QuantityMerger.merge("500 g", "1 kg"))
        assertEquals("1,5 l", QuantityMerger.merge("500 ml", "1 l"))
        assertEquals("5", QuantityMerger.merge("2", "3"))
        assertEquals("200 g + 1 Becher", QuantityMerger.merge("200 g", "1 Becher"))
    }

    @Test
    fun retailCatalog_suggestsMatches() {
        val hits = RetailCatalog.suggest("Kar")
        assertTrue(hits.any { it.equals("Karotten", ignoreCase = true) })
        assertTrue(hits.any { it.equals("Kartoffeln", ignoreCase = true) })
    }
}
