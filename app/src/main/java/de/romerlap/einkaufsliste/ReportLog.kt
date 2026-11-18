package de.romerlap.einkaufsliste

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Zentrales Ereignis- und Fehlerprotokoll für den In-App-Diagnose-Bericht.
 * Speichert die letzten Ereignisse im Speicher (Ringpuffer) und den
 * letzten Absturz dauerhaft; erzeugt daraus einen Markdown-Bericht.
 */
object ReportLog {

    private const val MAX_ENTRIES = 200
    private val entries = ArrayDeque<String>()
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.GERMANY)
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.GERMANY)

    fun info(source: String, message: String) = add("INFO", source, message)
    fun warn(source: String, message: String) = add("WARN", source, message)
    fun error(source: String, message: String) = add("ERROR", source, message)

    @Synchronized
    private fun add(level: String, source: String, message: String) {
        if (entries.size >= MAX_ENTRIES) entries.removeFirst()
        val firstLine = message.lineSequence().firstOrNull().orEmpty().take(300)
        val entry = "${timeFormat.format(Date())} $level $source: $firstLine"
        entries.addLast(entry)
        persist(entry)
    }

    /** Ereignis dauerhaft speichern – ueberlebt App-Neustarts ( fuer die Diagnose ). */
    private fun persist(entry: String) {
        appContext?.let { ctx ->
            try {
                val key = "event_log"
                val prefs = ctx.getSharedPreferences("report_log", Context.MODE_PRIVATE)
                val existing = prefs.getString(key, null).orEmpty()
                val lines = (if (existing.isEmpty()) emptyList() else existing.split("\n")).toMutableList()
                lines.add(entry)
                while (lines.size > MAX_ENTRIES) lines.removeAt(0)
                prefs.edit().putString(key, lines.joinToString("\n")).apply()
            } catch (_: Exception) {
                // Persistenz darf den Betrieb niemals blockieren
            }
        }
    }

    /** Kontext fuer die persistente Speicherung (von der Application gesetzt). */
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    @Synchronized
    fun snapshot(): List<String> = entries.toList()

    /** Speichert den letzten Absturz dauerhaft (aus dem Uncaught-ExceptionHandler). */
    fun persistCrash(context: Context, throwable: Throwable) {
        val entry = "${dateFormat.format(Date())} CRASH " +
                "${throwable.javaClass.simpleName}: ${throwable.message?.take(500)} " +
                "@ ${throwable.stackTrace.firstOrNull()?.toString().orEmpty()}"
        prefs(context).edit().putString(KEY_LAST_CRASH, entry).apply()
    }

    fun lastCrash(context: Context): String? =
        prefs(context).getString(KEY_LAST_CRASH, null)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("report_log", Context.MODE_PRIVATE)

    /**
     * Erzeugt den Markdown-Diagnose-Bericht.
     * Format: Markdown – kompakt, strukturiert und direkt weitergebbar.
     */
    fun generate(
        context: Context,
        openCount: Int,
        doneCount: Int,
        tags: List<String>,
        apiKeySet: Boolean
    ): String {
        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (_: Exception) {
            "?"
        }
        val versionCode = try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        } catch (_: Exception) {
            0
        }
        val nightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES

        return buildString {
            appendLine("# Einkaufsliste – Diagnose-Bericht")
            appendLine()
            appendLine("Erstellt: ${dateFormat.format(Date())}")
            appendLine()
            appendLine("## Gerät & App")
            appendLine("- App-Version: $versionName ($versionCode)")
            appendLine("- Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("- Gerät: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("- Dunkelmodus: ${if (nightMode) "aktiv" else "inaktiv"}")
            appendLine()
            appendLine("## Daten")
            appendLine("- Einträge gesamt: ${openCount + doneCount} (offen: $openCount, erledigt: $doneCount)")
            appendLine("- Tags: ${tags.size}${if (tags.isNotEmpty()) " (${tags.joinToString(", ")})" else ""}")
            appendLine("- Mistral-API-Schlüssel: ${if (apiKeySet) "eingerichtet" else "nicht eingerichtet"}")
            appendLine()
            appendLine("## Letzter Absturz")
            appendLine(lastCrash(context) ?: "Keiner seit der letzten Installation bekannt.")
            appendLine()
            appendLine("## Ereignisprotokoll (persistent, max. $MAX_ENTRIES, neueste zuletzt)")
            val log = try {
                prefs(context).getString("event_log", null)
                    ?.split("\n")
                    ?.filter { it.isNotBlank() }
                    ?: snapshot()
            } catch (_: Exception) {
                snapshot()
            }
            if (log.isEmpty()) {
                appendLine("(leer)")
            } else {
                log.forEach { appendLine("- $it") }
            }
        }
    }

    private const val KEY_LAST_CRASH = "last_crash"
}
