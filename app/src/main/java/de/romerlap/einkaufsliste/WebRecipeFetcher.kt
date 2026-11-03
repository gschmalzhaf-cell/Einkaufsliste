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
 * Laedt eine Rezept-Webseite und extrahiert daraus die Zutaten.
 * Bevorzugt strukturierte Daten (Schema.org JSON-LD bzw. Microdata), da dort
 * die Mengenangaben sauber und vollstaendig enthalten sind; sonst Fallback
 * auf den lesbaren Seitentext.
 */
object WebRecipeFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetch(url: String, maxChars: Int = 60_000): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0 Mobile Safari/537.36"
            )
            .header("Accept-Language", "de-DE,de;q=0.9,en;q=0.5")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            extractRecipeText(response.body!!.string()).take(maxChars)
        }
    }

    fun extractRecipeText(html: String): String {
        extractJsonLdIngredients(html)?.let { return it.joinToString("\n") }
        extractMicrodataIngredients(html)?.let { return it.joinToString("\n") }
        return extractText(html)
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
