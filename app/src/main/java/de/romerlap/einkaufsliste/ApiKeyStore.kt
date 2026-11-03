package de.romerlap.einkaufsliste

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** Speichert den Mistral-API-Schlüssel verschlüsselt auf dem Gerät. */
object ApiKeyStore {

    private const val PREFS_NAME = "mistral_prefs"
    private const val KEY = "mistral_api_key"

    fun get(context: Context): String? =
        prefs(context).getString(KEY, null)?.takeIf { it.isNotBlank() }

    fun set(context: Context, value: String?) {
        prefs(context).edit().putString(KEY, value?.trim()).apply()
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
