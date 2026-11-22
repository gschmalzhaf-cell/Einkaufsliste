package de.romerlap.einkaufsliste

import java.util.Locale

/**
 * Supermarkt-Abteilungen in typischer Laufreihenfolge deutscher Supermärkte (Rewe, Aldi, Edeka).
 */
enum class Aisle(val label: String, val emoji: String, val sortOrder: Int) {
    PRODUCE("Obst & Gemüse", "🥬", 0),
    BAKERY("Backwaren", "🥖", 1),
    DAIRY("Kühlregal & Milch", "🧀", 2),
    MEAT("Fleisch & Fisch", "🥩", 3),
    PANTRY("Vorrat & Trocken", "🍝", 4),
    BAKING_SPICES("Backen & Gewürze", "🧂", 5),
    FROZEN("Tiefkühl", "🧊", 6),
    DRINKS("Getränke", "🥤", 7),
    SNACKS("Süßes & Snacks", "🍫", 8),
    HOUSEHOLD("Haushalt & Drogerie", "🧼", 9),
    OTHER("Sonstiges", "📦", 10)
}

/**
 * Ordnet Artikelnamen offline der passenden Supermarkt-Abteilung zu.
 */
object AisleClassifier {

    fun classify(name: String): Aisle {
        val lower = name.lowercase(Locale.GERMAN)
        val tokens = lower.split(Regex("[^a-zäöüß]+")).filter { it.isNotEmpty() }

        for ((aisle, keywords) in rules) {
            if (keywords.any { kw ->
                    if (kw.length <= 3) tokens.any { it == kw } else lower.contains(kw)
                }
            ) {
                return aisle
            }
        }
        return Aisle.OTHER
    }

    private val rules: List<Pair<Aisle, List<String>>> = listOf(
        Aisle.FROZEN to listOf(
            "tiefkühl", "tk ", "tk-", "eiscreme", "speiseeis", "pommes", "fischstäbchen",
            "tiefkühlpizza", "spinat"
        ),
        Aisle.PRODUCE to listOf(
            "apfel", "äpfel", "banane", "orange", "zitrone", "limette", "beere", "erdbeer",
            "himbeer", "blaubeer", "heidelbeer", "traube", "birne", "pfirsich", "mango",
            "ananas", "kiwi", "melone", "avocado", "tomate", "gurke", "paprika", "salat",
            "rucola", "feldsalat", "karotte", "möhre", "kartoffel", "zwiebel", "knoblauch",
            "lauch", "porree", "zucchini", "aubergine", "brokkoli", "blumenkohl", "kohl",
            "kürbis", "spargel", "champignon", "pilz", "ingwer", "petersilie", "basilikum",
            "schnittlauch", "koriander", "kräuter", "radieschen", "sellerie", "fenchel",
            "kohlrabi", "mais", "obst", "gemüse"
        ),
        Aisle.BAKERY to listOf(
            "brot", "brötchen", "toast", "baguette", "ciabatta", "croissant", "brezel",
            "laugen", "semmel", "kuchen", "torte", "muffin", "hefezopf", "knäckebrot",
            "wrapp", "wrap", "fladenbrot", "pita"
        ),
        Aisle.DAIRY to listOf(
            "milch", "butter", "margarine", "käse", "gouda", "emmentaler", "parmesan",
            "mozzarella", "feta", "frischkäse", "quark", "joghurt", "skyr", "sahne",
            "schmand", "crème fraîche", "creme fraiche", "buttermilch", "kefir", "ei",
            "eier", "eigelb", "eiweiß", "tofu", "hefewürfel", "frische hefe", "blätterteig",
            "pizzateig", "spätzle", "gnocchi"
        ),
        Aisle.MEAT to listOf(
            "fleisch", "hack", "rinder", "schwein", "hähnchen", "hühnchen", "pute",
            "schnitzel", "steak", "wurst", "würstchen", "salami", "schinken", "speck",
            "bacon", "leberwurst", "fisch", "lachs", "thunfisch", "garnelen", "shrimps",
            "forelle", "hering", "bratwurst", "fleischkäse", "mett"
        ),
        Aisle.BAKING_SPICES to listOf(
            "mehl", "zucker", "puderzucker", "vanillezucker", "backpulver", "natron",
            "sahnesteif", "trockenhefe", "hefe", "kakao", "backkakao", "speisestärke",
            "stärke", "puddingpulver", "tortenguss", "salz", "pfeffer", "zimt", "curry",
            "muskat", "paprikapulver", "oregano", "thymian", "rosmarin", "kreuzkümmel",
            "gewürz", "brühe", "fondor", "soßenbinder", "vanille", "lebkuchen", "spekulatius",
            "kokosraspel", "mandel", "haselnuss", "walnuss", "nüsse"
        ),
        Aisle.DRINKS to listOf(
            "wasser", "sprudel", "saft", "orangensaft", "apfelsaft", "cola", "limo",
            "fanta", "sprite", "spezi", "eistee", "tee", "kaffee", "espresso", "bier",
            "wein", "rotwein", "weißwein", "sekt", "prosecco", "glühwein", "haferdrink",
            "mandeldrink", "sojadrink", "energy", "schorle", "rum"
        ),
        Aisle.SNACKS to listOf(
            "schokolade", "schoko", "chips", "flips", "salzstangen", "cracker", "keks",
            "kekse", "gummibär", "haribo", "bonbon", "riegel", "popcorn", "erdnüsse",
            "pistazien", "cashew", "studentenfutter", "pralinen", "eis"
        ),
        Aisle.HOUSEHOLD to listOf(
            "toilettenpapier", "klopapier", "küchenrolle", "zewa", "taschentücher",
            "spülmittel", "spülmaschinen", "tabs", "waschmittel", "weichspüler",
            "putzmittel", "reiniger", "müllbeutel", "frischhaltefolie", "alufolie",
            "backpapier", "schwamm", "seife", "shampoo", "duschgel", "zahnpasta",
            "zahnbürste", "deo", "creme", "windeln", "kerze", "batterie", "katzenfutter",
            "hundefutter"
        ),
        Aisle.PANTRY to listOf(
            "nudel", "pasta", "spaghetti", "penne", "fusilli", "lasagne", "reis", "couscous",
            "bulgur", "quinoa", "linsen", "bohnen", "kichererbsen", "erbsen", "haferflocken",
            "müsli", "cornflakes", "grieß", "öl", "olivenöl", "rapsöl", "essig", "balsamico",
            "tomatenmark", "passierte", "gestückelte", "pesto", "soße", "sauce", "ketchup",
            "senf", "mayo", "mayonnaise", "honig", "marmelade", "konfitüre", "nutella",
            "nussnougat", "erdnusscreme", "aufstrich", "konserve", "dose", "suppe",
            "kokosmilch", "sojasoße", "oliven", "kapern"
        )
    )
}

/**
 * Zerlegt natürliche Schnelleingaben wie "2 kg Kartoffeln #Sonntag" oder "3x Paprika"
 * in Artikelname, Menge und optionales Tag.
 */
object SmartInputParser {

    data class ParsedInput(
        val name: String,
        val quantity: String?,
        val tag: String?
    )

    private val tagRegex = Regex("""#([\p{L}\p{N}_-]+)""")
    private val leadingMultiplier = Regex("""^(\d+(?:[.,]\d+)?)\s*[x×]\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val leadingQtyUnit = Regex(
        """^(\d+(?:[.,]\d+)?)\s*(kg|g|mg|l|ml|dl|cl|stück|stk|st|packung|pack|pkg|pck|becher|dose|dosen|flasche|flaschen|netz|bund|knolle|el|tl|scheiben|kugeln|tafeln|rollen)\b\.?\s+(.+)$""",
        RegexOption.IGNORE_CASE
    )
    private val leadingNumberOnly = Regex("""^(\d+(?:[.,]\d+)?)\s+(.+)$""")

    fun parse(raw: String): ParsedInput {
        var text = raw.trim()
        var foundTag: String? = null

        val tagMatch = tagRegex.find(text)
        if (tagMatch != null) {
            foundTag = tagMatch.groupValues[1]
            text = text.removeRange(tagMatch.range).replace(Regex("""\s{2,}"""), " ").trim()
        }

        val mult = leadingMultiplier.matchEntire(text)
        if (mult != null) {
            return ParsedInput(
                name = capitalizeFirst(mult.groupValues[2].trim()),
                quantity = "${mult.groupValues[1]} Stück",
                tag = foundTag
            )
        }

        val qtyUnit = leadingQtyUnit.matchEntire(text)
        if (qtyUnit != null) {
            return ParsedInput(
                name = capitalizeFirst(qtyUnit.groupValues[3].trim()),
                quantity = "${qtyUnit.groupValues[1]} ${qtyUnit.groupValues[2]}",
                tag = foundTag
            )
        }

        val numOnly = leadingNumberOnly.matchEntire(text)
        if (numOnly != null) {
            return ParsedInput(
                name = capitalizeFirst(numOnly.groupValues[2].trim()),
                quantity = numOnly.groupValues[1],
                tag = foundTag
            )
        }

        return ParsedInput(
            name = text,
            quantity = null,
            tag = foundTag
        )
    }

    private fun capitalizeFirst(s: String): String =
        s.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.GERMAN) else it.toString() }
}

/**
 * Führt Mengen zweier identischer Artikel intelligent zusammen (z. B. "200 g" + "300 g" -> "500 g",
 * "500 ml" + "1 l" -> "1,5 l", "2" + "3" -> "5").
 */
object QuantityMerger {

    private val qtyRegex = Regex("""^(\d+(?:[.,]\d+)?)\s*(.*)$""")

    fun normalizeName(name: String): String =
        name.trim().lowercase(Locale.GERMAN).replace(Regex("""\s+"""), " ")

    fun merge(existing: String?, added: String?): String? {
        val a = existing?.trim()?.takeIf { it.isNotEmpty() }
        val b = added?.trim()?.takeIf { it.isNotEmpty() }
        if (a == null) return b
        if (b == null) return a
        if (a.equals(b, ignoreCase = true)) {
            // Identische Menge zweimal hinzugefügt -> versuchen zu verdoppeln
            return mergeNumeric(a, b) ?: "$a + $b"
        }
        return mergeNumeric(a, b) ?: "$a + $b"
    }

    private fun mergeNumeric(a: String, b: String): String? {
        val ma = qtyRegex.matchEntire(a) ?: return null
        val mb = qtyRegex.matchEntire(b) ?: return null
        val va = ma.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val vb = mb.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
        val ua = ma.groupValues[2].trim().lowercase(Locale.GERMAN)
        val ub = mb.groupValues[2].trim().lowercase(Locale.GERMAN)

        // Gewicht: g <-> kg
        val weightA = toGrams(va, ua)
        val weightB = toGrams(vb, ub)
        if (weightA != null && weightB != null) {
            val total = weightA + weightB
            return if (total >= 1000.0) "${formatNumber(total / 1000.0)} kg" else "${formatNumber(total)} g"
        }

        // Volumen: ml <-> l
        val volA = toMilliliters(va, ua)
        val volB = toMilliliters(vb, ub)
        if (volA != null && volB != null) {
            val total = volA + volB
            return if (total >= 1000.0) "${formatNumber(total / 1000.0)} l" else "${formatNumber(total)} ml"
        }

        // Gleiche Einheit (oder beide ohne Einheit / Stück)
        val normA = normalizePieceUnit(ua)
        val normB = normalizePieceUnit(ub)
        if (normA == normB) {
            val sum = formatNumber(va + vb)
            val displayUnit = ma.groupValues[2].trim().ifEmpty { mb.groupValues[2].trim() }
            return if (displayUnit.isEmpty()) sum else "$sum $displayUnit"
        }
        return null
    }

    private fun toGrams(value: Double, unit: String): Double? = when (unit) {
        "g", "gr", "gramm" -> value
        "kg", "kilo", "kilogramm" -> value * 1000.0
        else -> null
    }

    private fun toMilliliters(value: Double, unit: String): Double? = when (unit) {
        "ml", "milliliter" -> value
        "dl" -> value * 100.0
        "cl" -> value * 10.0
        "l", "liter" -> value * 1000.0
        else -> null
    }

    private fun normalizePieceUnit(unit: String): String = when (unit) {
        "", "x", "×", "st", "stk", "stk.", "stück" -> "piece"
        else -> unit
    }

    private fun formatNumber(value: Double): String {
        val rounded = Math.round(value * 100.0) / 100.0
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString().replace('.', ',').trimEnd('0').trimEnd(',')
        }
    }
}
