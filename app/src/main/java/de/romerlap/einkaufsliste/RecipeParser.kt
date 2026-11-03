package de.romerlap.einkaufsliste

/**
 * Extrahiert aus einem Rezept-Text die wahrscheinlichen Zutaten.
 * Erkennt gängige deutsche Mengenangaben wie "200 g Mehl", "2 EL Öl" oder "3 Tomaten".
 */
object RecipeParser {

    data class ParsedIngredient(
        val name: String,
        val quantity: String?
    )

    private val quantityWithUnit = Regex(
        """^\W{0,3}(\d+(?:[.,]\d+)?)\s*([a-zA-ZäöüÄÖÜß]{1,12})\s+(.+)$"""
    )
    private val quantityOnly = Regex(
        """^\W{0,3}(\d+(?:[.,]\d+)?)\s+(.+)$"""
    )
    private val leadingNumber = Regex("""^\d+[.)]\s+""")

    private val headings = listOf(
        "zutaten", "zutaten:", "einkaufsliste", "einkaufsliste:",
        "zubereitung", "zubereitung:", "arbeitsschritte", "zutaten für"
    )

    private val instructionMarkers = listOf(
        "erhitzen", "anbraten", "mischen", "rühren", "backen", "kochen", "schneiden",
        "servieren", "würfeln", "dazugeben", "hinzufügen", "braten", "schmoren", "geben sie"
    )

    fun extractIngredients(text: String): List<ParsedIngredient> {
        val results = mutableListOf<ParsedIngredient>()
        text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                val cleaned = line
                    .replace(leadingNumber, "")
                    .replace(Regex("""^[•*\-–—]+\s*"""), "")
                    .trim()
                if (cleaned.isEmpty()) return@forEach
                val lower = cleaned.lowercase()
                if (lower in headings || lower.endsWith(":")) return@forEach
                if (looksLikeInstruction(lower)) return@forEach
                if (cleaned.length > 60) return@forEach

                val unitMatch = quantityWithUnit.matchEntire(cleaned)
                if (unitMatch != null) {
                    results += ParsedIngredient(
                        name = unitMatch.groupValues[3].trim(),
                        quantity = "${unitMatch.groupValues[1]} ${unitMatch.groupValues[2]}"
                    )
                } else {
                    val plainMatch = quantityOnly.matchEntire(cleaned)
                    if (plainMatch != null) {
                        results += ParsedIngredient(
                            name = plainMatch.groupValues[2].trim(),
                            quantity = plainMatch.groupValues[1]
                        )
                    } else {
                        results += ParsedIngredient(name = cleaned, quantity = null)
                    }
                }
            }
        return results.distinctBy { it.name.lowercase() }
    }

    private fun looksLikeInstruction(lower: String): Boolean {
        if (lower.length > 45) return true
        if (lower.endsWith(".")) return true
        return instructionMarkers.any { lower.contains(it) }
    }
}
