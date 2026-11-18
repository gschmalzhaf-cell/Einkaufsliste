package de.romerlap.einkaufsliste

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Speichert den Mistral-API-Schlüssel verschlüsselt auf dem Gerät (mit Fallback bei Keystore-Fehler). */
object ApiKeyStore {

    private const val PREFS_NAME = "mistral_prefs"
    private const val FALLBACK_PREFS_NAME = "mistral_prefs_fallback"
    private const val KEY = "mistral_api_key"

    /** Liest den Schlüssel – wirft nie, sondern protokolliert und liefert null. */
    fun get(context: Context): String? {
        val appCtx = context.applicationContext
        val encrypted = try {
            prefs(appCtx).getString(KEY, null)?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            ReportLog.error("ApiKeyStore", "Verschlüsseltes Lesen fehlgeschlagen (${e.javaClass.simpleName}): ${e.message}")
            null
        }
        if (encrypted != null) return encrypted
        return try {
            fallbackPrefs(appCtx).getString(KEY, null)?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            ReportLog.error("ApiKeyStore", "Fallback-Lesen fehlgeschlagen: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    /** Speichert den Schlüssel synchron – mit automatischer Reparatur & Fallback. */
    fun set(context: Context, value: String?) {
        val appCtx = context.applicationContext
        val cleaned = value?.trim()?.takeIf { it.isNotBlank() }
        var savedEncrypted = false
        try {
            savedEncrypted = prefs(appCtx).edit().putString(KEY, cleaned).commit()
        } catch (e: Exception) {
            ReportLog.error("ApiKeyStore", "Verschlüsseltes Speichern fehlgeschlagen, setze Store zurück: ${e.javaClass.simpleName}")
            try {
                appCtx.deleteSharedPreferences(PREFS_NAME)
                savedEncrypted = prefs(appCtx).edit().putString(KEY, cleaned).commit()
            } catch (retryEx: Exception) {
                ReportLog.error("ApiKeyStore", "Neuaufbau fehlgeschlagen, nutze Fallback: ${retryEx.javaClass.simpleName}")
            }
        }
        // Fallback immer synchron mitpflegen, falls Keystore auf dem Gerät instabil ist
        try {
            fallbackPrefs(appCtx).edit().putString(KEY, cleaned).commit()
        } catch (_: Exception) {
        }
        if (cleaned == null) {
            ReportLog.info("Einstellungen", "API-Schlüssel entfernt")
        } else {
            ReportLog.info(
                "Einstellungen",
                "API-Schlüssel gespeichert (${if (savedEncrypted) "verschlüsselt" else "Fallback"})"
            )
        }
    }

    /** Liefert eine maskierte Vorschau wie "sk-m••••1a2b" zur Anzeige in der UI. */
    fun maskedPreview(key: String?): String? {
        val k = key?.trim()?.takeIf { it.isNotBlank() } ?: return null
        return if (k.length <= 8) {
            "•••• (${k.length} Zeichen)"
        } else {
            "${k.take(4)}••••${k.takeLast(4)} (${k.length} Zeichen)"
        }
    }

    private fun fallbackPrefs(context: Context) =
        context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)

    private fun prefs(context: Context): EncryptedSharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences
    }
}

