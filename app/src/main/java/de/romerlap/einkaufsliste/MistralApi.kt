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
 * Anbindung an die Mistral-API: extrahiert aus Fotos (Vision-Modell Pixtral)
 * und Rezepttexten Artikel mit Menge als strukturiertes JSON.
 */
object MistralApi {

    data class ExtractedItem(
        val name: String,
        val quantity: String?
    )

    class MistralException(message: String) : IOException(message)

    private const val ENDPOINT = "https://api.mistral.ai/v1/chat/completions"
    private const val VISION_MODEL = "pixtral-12-latest"
    private const val TEXT_MODEL = "mistral-small-latest"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    private val prompt = """
        Du bist ein Einkaufslisten-Assistent. Analysiere den gegebenen Inhalt
        (ein Foto einer Liste/Notiz/Rezept oder einen Rezepttext) und extrahiere
        alle Artikel, die man einkaufen könnte, mit ihrer Menge, falls angegeben.
        Antworte ausschließlich als JSON-Objekt im Format
        {"items": [{"name": "Artikel", "quantity": "Menge oder null"}]}.
        Nutze normale deutsche Artikelbezeichnungen in der Einzahl oder so,
        wie sie auf einer Einkaufsliste stehen. Keine Aufzählungszeichen,
        keine Zubereitungsschritte, keine Überschriften.
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
        return request(VISION_MODEL, content, apiKey)
    }

    suspend fun extractFromRecipeText(text: String, apiKey: String): List<ExtractedItem> {
        val content = JSONArray().apply {
            put(JSONObject().put("type", "text").put("text", prompt + "\n\nRezept:\n" + text))
        }
        return request(TEXT_MODEL, content, apiKey)
    }

    private suspend fun request(model: String, content: JSONArray, apiKey: String): List<ExtractedItem> =
        withContext(Dispatchers.IO) {
            val body = JSONObject().apply {
                put("model", model)
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
                val contentOut = json.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content")
                parse(contentOut)
            }
        }

    fun parse(content: String): List<ExtractedItem> {
        val itemsJson = JSONObject(content).getJSONArray("items")
        return (0 until itemsJson.length()).map { i ->
            val obj = itemsJson.getJSONObject(i)
            ExtractedItem(
                name = obj.getString("name"),
                quantity = if (obj.has("quantity") && !obj.isNull("quantity"))
                    obj.getString("quantity") else null
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
