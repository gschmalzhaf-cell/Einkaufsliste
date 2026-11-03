package de.romerlap.einkaufsliste

import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Lädt eine Rezept-Webseite und extrahiert den lesbaren Text daraus. */
object WebRecipeFetcher {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun fetch(url: String, maxChars: Int = 25_000): String = withContext(Dispatchers.IO) {
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
            extractText(response.body!!.string()).take(maxChars)
        }
    }

    fun extractText(html: String): String {
        val cleaned = html
            .replace(Regex("(?is)<(script|style|noscript|head|nav|footer)[^>]*>.*?</\\1>"), " ")
            .replace(Regex("(?i)<br\\s*/?>|</p>|</li>|</h[1-6]>"), "\n")
            .replace(Regex("<[^>]+>"), " ")
        val decoded = Html.fromHtml(cleaned, Html.FROM_HTML_MODE_LEGACY).toString()
        return decoded.lines()
            .joinToString("\n") { it.trim() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }
}
