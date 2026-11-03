package de.romerlap.einkaufsliste

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Laedt Rezept-Quellen und extrahiert daraus die Zutaten mit Mengen.
 * Chefkoch-Links werden ueber die oeffentliche Chefkoch-API ausgelesen
 * (die Seite selbst laedt Zutatennamen erst per JavaScript); andere Seiten
 * werden bevorzugt ueber strukturierte Daten (JSON-LD bzw. Microdata)
 * ausgelesen, sonst ueber den lesbaren Seitentext, wobei getrennt
 * vorliegende Mengen- und Artikelzeilen paarweise verbunden werden.
 */
object WebRecipeFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Mobile Safari/537.36"

    suspend fun fetch(url: String, maxChars: Int = 60_000): String = withContext(Dispatchers.IO) {
        extractChefkochId(url)?.let { id ->
            try {
                return@withContext fetchChefkochRecipe(id)
            } catch (e: Exception) {
            }
        }
        extractRecipeText(downloadHtml(url)).take(maxChars)
    }

    private suspend fun downloadHtml(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept-Language", "de-DE,de;q=0.9,en;q=0.5")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body!!.string()
        }
    }

    private fun extractChefkochId(url: String): String? =
        Regex("""chefkoch\.de/rezepte/(\d+)""").find(url)?.groupValues?.get(1)

    private suspend fun fetchChefkochRecipe(id: String): String {
        val json = JSONObject(
            downloadJson("https://api.chefkoch.de/v2/recipes/$id")
        )
        val groups = json.optJSONArray("ingredientGroups") ?: throw IOException("Keine Zutaten")
        val lines = mutableListOf<String>()
        for (g in 0 until groups.length()) {
            val group = groups.optJSONObject(g) ?: continue
            val ingredients = group.optJSONArray("ingredients") ?: continue
            for (i in 0 until ingredients.length()) {
                val ingredient = ingredients.optJSONObject(i) ?: continue
                val name = ingredient.optString("name").trim()
                if (name.isEmpty()) continue
                val amount = ingredient.optDouble("amount", 0.0)
                val unit = ingredient.optString("unit").trim()
                val usageInfo = ingredient.optString("usageInfo").trim()
                var line = when {
                    amount > 0 && unit.isNotEmpty() -> "${formatAmount(amount)} $unit $name"
                    amount > 0 -> "${formatAmount(amount)} $name"
                    else -> name
                }
                if (usageInfo.isNotEmpty()) line += " ($usageInfo)"
                lines.add(line)
            }
        }
        if (lines.isEmpty()) throw IOException("Keine Zutaten")
        return lines.joinToString("\n")
    }

    private suspend fun downloadJson(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body!!.string()
        }
    }

    private fun formatAmount(amount: Double): String =
        if (amount == amount.toLong().toDouble()) {
            amount.toLong().toString()
        } else {
            amount.toString().replace('.', ',')
        }

    fun extractRecipeText(html: String): String {
        extractJsonLdIngredients(html)?.let { return it.joinToString("\n") }
        extractMicrodataIngredients(html)?.let { return it.joinToString("\n") }
        return pairQuantityLines(extractText(html))
    }

    private val quantityOnlyLine = Regex(
        """^\d+(?:[.,]\d+)?\s*(?:g|kg|ml|l|TL|EL|Pck\.?|Packung(?:en)?|Prise(?:n)?|Bund|Stk\.?|Stücke?|Stück(?:en)?|Dose(?:n)?|Glas|Becher|Würfel|Zweig(?:en)?|Blätter|Handvoll|Spritzer|Etwas)?\s*$"""
    )

    private fun pairQuantityLines(text: String): String {
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val out = mutableListOf<String>()
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val next = lines.getOrNull(i + 1)
            if (next != null && quantityOnlyLine.matches(line) && !next.firstOrNull()?.isDigit()!!) {
                out += "$line $next"
                i += 2
            } else {
                out += line
                i++
            }
        }
        return out.joinToString("\n")
    }

    private fun extractJsonLdIngredients(html: String): List<String>? {
        val ingredients = mutableListOf<String>()
        val blockRegex = Regex("""(?is)<script[^>]*type=["']application/ld\+json["'][^>]*>(.*?)</script>""")
        blockRegex.findAll(html).forEach { match ->
            try {
                collectIngredients(JSONObject(match.groupValues[1].trim()), ingredients)
            } catch (e: Exception) {
            }
        }
        return ingredients.takeIf { it.isNotEmpty() }?.distinct()
    }

    private fun collectIngredients(node: Any?, out: MutableList<String>) {
        when (node) {
            is JSONObject -> {
                node.optJSONArray("recipeIngredient")?.let { arr ->
                    for (i in 0 until arr.length()) {
                        arr.optString(i).trim().takeIf { it.isNotEmpty() }?.let(out::add)
                    }
                }
                node.optJSONArray("@graph")?.let { graph ->
                    for (i in 0 until graph.length()) collectIngredients(graph.opt(i), out)
                }
            }
            is JSONArray -> for (i in 0 until node.length()) collectIngredients(node.opt(i), out)
        }
    }

    private fun extractMicrodataIngredients(html: String): List<String>? {
        val regex = Regex("""(?is)<[^>]*itemprop=["']recipeIngredient["'][^>]*>(.*?)</\w+>""")
        val items = regex.findAll(html)
            .map { extractText(it.groupValues[1]) }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .toList()
        return items.takeIf { it.isNotEmpty() }
    }

    fun extractText(html: String): String {
        val cleaned = html
            .replace(Regex("""(?is)<(script|style|noscript|head|nav|footer)[^>]*>.*?</\1>"""), " ")
            .replace(Regex("""(?i)<br\s*/?>|</p>|</li>|</h[1-6]>"""), "\n")
            .replace(Regex("<[^>]+>"), " ")
        val decoded = Html.fromHtml(cleaned, Html.FROM_HTML_MODE_LEGACY).toString()
        return decoded.lines()
            .joinToString("\n") { it.trim() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
