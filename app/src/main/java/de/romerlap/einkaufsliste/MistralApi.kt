package de.romerlap.einkaufsliste

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit

/**
 * Anbindung an die Mistral-API: extrahiert aus Fotos und Rezepttexten
 * Artikel mit Menge und handelsueblicher Packungsgroesse als strukturiertes
 * JSON; recherchiert auf Anfrage auch einzelne Packungsgroessen.
 * Modell: mistral-small-latest (mit Vision-Faehigkeit).
 */
object MistralApi {

    data class ExtractedItem(
        val name: String,
        val quantity: String?,
        val retailHint: String? = null
    )

    class MistralException(message: String) : IOException(message)

    private const val ENDPOINT = "https://api.mistral.ai/v1/chat/completions"
    private const val MODEL = "mistral-small-latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val prompt = """
        Du bist ein Einkaufslisten-Assistent für deutsche Supermärkte (Rewe, Aldi).
        Analysiere den gegebenen Inhalt (ein Foto einer Liste/Notiz/Rezept oder
        einen Rezepttext) und extrahiere alle Artikel, die man einkaufen könnte.
        Antworte ausschließlich als JSON-Objekt im Format
        {"items": [{"name": "Artikel", "quantity": "Menge oder null", "retail_hint": "Packungsgröße"}]}.
        Für quantity nutze die angegebene Menge (z. B. "200 g"), falls vorhanden, sonst null.
        Für retail_hint nenne die handelsübliche Packungsgröße bei Rewe/Aldi, mit der die
        benötigte Menge abgedeckt werden kann (z. B. "1 kg Packung", "2 × 250 g Becher",
        "10er-Packung"). Nutze normale deutsche Artikelbezeichnungen, so wie sie auf
        einer Einkaufsliste stehen. Keine Zubereitungsschritte, keine Überschriften.
    """.trimIndent().replace("\n", " ")

    suspend fun extractFromImage(bitmap: Bitmap, apiKey: String): List<ExtractedItem> {
        val base64 = withContext(Dispatchers.IO) { bitmapToBase64(bitmap) }
        val content = JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", prompt))
            put(
                JSONObject().put("type", "image_url")
                    .put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$base64"))
            )
        }
        return parse(requestRaw(content, apiKey))
    }

    suspend fun extractFromRecipeText(text: String, apiKey: String): List<ExtractedItem> {
        val content = JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", prompt + "\n\nRezept:\n" + text))
        }
        return parse(requestRaw(content, apiKey))
    }

    suspend fun researchRetailHint(name: String, quantity: String?, apiKey: String): String {
        val question = buildString {
            append("Ein Kunde kauft bei deutschen Supermärkten (Rewe, Aldi) ein und braucht \"")
            append(name)
            append("\"")
            if (!quantity.isNullOrBlank()) {
                append(" mit der benötigten Menge ")
                append(quantity)
            }
            append(". Welche handelsübliche Packungsgröße deckt diesen Bedarf ab? ")
            append("Antworte ausschließlich als JSON {\"retail_hint\": \"...\"} mit einer kurzen ")
            append("Angabe wie '1 kg Packung' oder '2 × 250 g Becher'.")
        }
        val content = JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", question))
        }
        val out = JSONObject(requestRaw(content, apiKey))
        return out.optString("retail_hint").trim().takeIf { it.isNotEmpty() }
            ?: throw MistralException("Keine Packungsgröße erhalten")
    }

    private suspend fun requestRaw(content: JSONArray, apiKey: String): String =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("model", MODEL)
                put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
                put("temperature", 0)
                put("response_format", JSONObject().put("type", "json_object"))
            }
            val request = Request.Builder()
                .url(ENDPOINT)
                .header("Authorization", "Bearer $apiKey")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw MistralException("Mistral API: HTTP ${response.code}")
                }
                val json = JSONObject(response.body!!.string())
                json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
            }
        }

    fun parse(content: String): List<ExtractedItem> {
        val itemsJson = JSONObject(content).getJSONArray("items")
        return (0 until itemsJson.length()).map { i ->
            val obj = itemsJson.getJSONObject(i)
            ExtractedItem(
                name = obj.getString("name"),
                quantity = if (obj.has("quantity") && !obj.isNull("quantity"))
                    obj.getString("quantity") else null,
                retailHint = obj.optString("retail_hint").trim().takeIf { it.isNotEmpty() }
            )
        }.filter { it.name.isNotBlank() }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val scaled = scaleDown(bitmap, maxDim = 1280)
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return Base64.getEncoder().encodeToString(stream.toByteArray())
    }

    private fun scaleDown(bitmap: Bitmap, maxDim: Int): Bitmap {
        val maxSide = maxOf(bitmap.width, bitmap.height)
        if (maxSide <= maxDim) return bitmap
        val scale = maxDim.toFloat() / maxSide
        return Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true
        )
    }
}
