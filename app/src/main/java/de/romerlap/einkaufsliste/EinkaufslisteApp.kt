package de.romerlap.einkaufsliste

import android.app.Application

/** Application-Klasse: registriert Absturz-Protokollierung und Start-Ereignis. */
class EinkaufslisteApp : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                try {
                    ReportLog.persistCrash(this, throwable)
                } catch (_: Throwable) {
                    // Protokollierung darf den Absturz niemals blockieren
                }
                defaultHandler?.uncaughtException(thread, throwable)
            }
            ReportLog.init(this)
            ReportLog.info("App", "Gestartet (Android ${android.os.Build.VERSION.RELEASE})")
            // Datenbank im Hintergrund vorwaermen, damit der Haupt-Thread sie sofort vorfindet
            Thread {
                try {
                    de.romerlap.einkaufsliste.data.AppDatabase.get(this)
                    ReportLog.info("App", "Datenbank vorgewärmt")
                } catch (e: Exception) {
                    ReportLog.error("App", "DB-Vorwärmung: ${e.javaClass.simpleName}: ${e.message}")
                }
            }.start()
        } catch (e: Exception) {
            // Der App-Start darf an der Diagnose niemals scheitern
        }
    }
}
