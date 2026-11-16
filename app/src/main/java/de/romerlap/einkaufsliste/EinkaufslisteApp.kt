package de.romerlap.einkaufsliste

import android.app.Application

/** Application-Klasse: registriert Absturz-Protokollierung und Start-Ereignis. */
class EinkaufslisteApp : Application() {

    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                ReportLog.persistCrash(this, throwable)
            } catch (_: Throwable) {
                // Protokollierung darf den Absturz niemals blockieren
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
        ReportLog.info("App", "Gestartet (Android ${android.os.Build.VERSION.RELEASE})")
    }
}
