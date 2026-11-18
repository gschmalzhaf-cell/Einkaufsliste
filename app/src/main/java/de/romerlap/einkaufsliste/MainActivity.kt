package de.romerlap.einkaufsliste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import de.romerlap.einkaufsliste.ui.ListScreen
import de.romerlap.einkaufsliste.ui.theme.EinkaufslisteTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ReportLog.info("Startup", "MainActivity onCreate")

        // Ladebildschirm (reine Views) – muss erst sichtbar rendern,
        // bevor Compose attached wird (Attach im ersten Frame-Fenster
        // blockiert auf Android 17 das komplette Fenster).
        val loadingScreen = buildLoadingScreen()
        setContentView(loadingScreen)
        ReportLog.info("Startup", "Ladebildschirm gesetzt (mit Berichts-Knopf)")

        Thread {
            // Auf DB-Vorwaermung OHNE Timeout warten – der Haupt-Thread baut nie selbst
            (application as EinkaufslisteApp).dbReady.await()
            ReportLog.info("Startup", "DB vorgewärmt (dbReady)")
            runOnUiThread {
                ReportLog.info("Startup", "DB bereit – Compose-Start geplant")
                // Echte Frames des Ladebildschirms zulassen, dann Compose starten
                loadingScreen.postDelayed({ startComposeUi() }, 800)
            }
        }.start()
    }

    /** Teilt den Diagnose-Bericht (reine System-UI, unabhaengig von Compose). */
    private fun shareReportNow() {
        val report = try {
            ReportLog.generate(this, 0, 0, emptyList(), false)
        } catch (e: Exception) {
            "Bericht fehlgeschlagen: ${e.javaClass.simpleName}: ${e.message}"
        }
        ReportLog.info("Startup", "Bericht manuell geteilt")
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Einkaufsliste – Diagnose-Bericht")
            putExtra(android.content.Intent.EXTRA_TEXT, report)
        }
        startActivity(android.content.Intent.createChooser(send, "Diagnose-Bericht teilen"))
    }

    /** Watchdog: bleibt die Komposition haengen, wird der Bericht automatisch geteilt. */
    private fun armWatchdog() {
        Thread {
            try {
                Thread.sleep(10_000)
            } catch (_: InterruptedException) {
                return@Thread
            }
            val finished = try {
                val log = getSharedPreferences("report_log", MODE_PRIVATE)
                    .getString("event_log", "").orEmpty()
                log.contains("4 Scaffold-Content abgeschlossen")
            } catch (_: Exception) {
                false
            }
            if (!finished) {
                ReportLog.error("Startup", "Watchdog: Komposition nach 10 s nicht abgeschlossen – Bericht wird geteilt")
                runOnUiThread { shareReportNow() }
            }
        }.start()
    }

    private fun startComposeUi() {
        if (composeStarted.getAndSet(true)) return
        ReportLog.info("Startup", "Compose wird gestartet (Thread ${Thread.currentThread().name})")
        armWatchdog()
        setContent {
            EinkaufslisteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EinkaufslisteApp()
                }
            }
        }
    }

    private val composeStarted = java.util.concurrent.atomic.AtomicBoolean(false)

    /** Minimaler Ladebildschirm im Comic-Look (ohne Compose) mit Berichts-Knopf. */
    private fun buildLoadingScreen(): android.widget.LinearLayout {
        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.gravity = android.view.Gravity.CENTER
        container.setBackgroundColor(android.graphics.Color.rgb(0xFF, 0xF6, 0xE5))
        container.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.setPadding(48, 96, 48, 96)

        val text = android.widget.TextView(this)
        text.text = "EINKAUFSLISTE"
        text.textSize = 28f
        text.gravity = android.view.Gravity.CENTER
        text.setTextColor(android.graphics.Color.rgb(0xD6, 0x28, 0x28))
        text.setOnClickListener { startComposeUi() }
        container.addView(text)

        val hint = android.widget.TextView(this)
        hint.text = "Startet …"
        hint.textSize = 14f
        hint.gravity = android.view.Gravity.CENTER
        hint.setPadding(0, 16, 0, 48)
        hint.setTextColor(android.graphics.Color.rgb(0x4A, 0x44, 0x3A))
        container.addView(hint)

        val reportButton = android.widget.Button(this)
        reportButton.text = "Diagnose-Bericht teilen"
        reportButton.textSize = 13f
        reportButton.setTextColor(android.graphics.Color.rgb(0x1D, 0x1D, 0x1B))
        reportButton.setBackgroundColor(android.graphics.Color.rgb(0xFF, 0xC8, 0x00))
        reportButton.setOnClickListener { shareReportNow() }
        container.addView(reportButton)
        return container
    }
}

@Composable
fun EinkaufslisteApp(viewModel: ListViewModel = viewModel()) {
    ListScreen(viewModel)
}
