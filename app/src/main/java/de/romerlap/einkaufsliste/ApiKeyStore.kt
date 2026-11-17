package de.romerlap.einkaufsliste

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Speichert den Mistral-API-Schlüssel verschlüsselt auf dem Gerät. */
object ApiKeyStore {

    private const val PREFS_NAME = "mistral_prefs"
    private const val KEY = "mistral_api_key"

    /** Liest den Schlüssel – wirft nie, sondern protokolliert und liefert null. */
    fun get(context: Context): String? = try {
        prefs(context).getString(KEY, null)?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        ReportLog.error("ApiKeyStore", "Lesen fehlgeschlagen: ${e.javaClass.simpleName}: ${e.message}")
        null
    }

    /** Speichert den Schlüssel – wirft nie, sondern protokolliert. */
    fun set(context: Context, value: String?) {
        try {
            prefs(context).edit().putString(KEY, value?.trim()).apply()
            if (value.isNullOrBlank()) {
                ReportLog.info("Einstellungen", "API-Schlüssel entfernt")
            } else {
                ReportLog.info("Einstellungen", "API-Schlüssel gespeichert")
            }
        } catch (e: Exception) {
            ReportLog.error("ApiKeyStore", "Speichern fehlgeschlagen: ${e.javaClass.simpleName}: ${e.message}")
        }
    }

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
