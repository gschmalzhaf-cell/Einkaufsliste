package de.romerlap.einkaufsliste

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import de.romerlap.einkaufsliste.ui.v2.HomeScreen
import de.romerlap.einkaufsliste.ui.v2.SeasonTheme

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
        ReportLog.info("Startup", "Ladebildschirm gesetzt")

        val openButton = loadingScreen.findViewWithTag<android.widget.Button>("open")
        Thread {
            (application as EinkaufslisteApp).dbReady.await()
            ReportLog.info("Startup", "DB vorgewärmt (dbReady)")
            runOnUiThread {
                // Automatischer Start, sobald die DB bereit ist (Compose ohne Surface-Wurzel)
                loadingScreen.postDelayed({ startComposeUi() }, 600)
                // Rueckfall: erscheint der Start nicht, bietet sich der Knopf an
                loadingScreen.postDelayed({
                    if (!composeStarted.get()) {
                        openButton.visibility = android.view.View.VISIBLE
                        ReportLog.info("Startup", "Rückfall-Knopf sichtbar")
                    }
                }, 3200)
            }
        }.start()
        armWatchdog()
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

    /** Watchdog: startet die Komposition nie (Marke 1), wird der Bericht geteilt. */
    private fun armWatchdog() {
        Thread {
            try {
                Thread.sleep(30_000)
            } catch (_: InterruptedException) {
                return@Thread
            }
            val started = try {
                val log = getSharedPreferences("report_log", MODE_PRIVATE)
                    .getString("event_log", "").orEmpty()
                log.contains("1 Komposition betreten")
            } catch (_: Exception) {
                false
            }
            if (!started) {
                ReportLog.error("Startup", "Watchdog: Komposition nie gestartet – Bericht wird geteilt")
                runOnUiThread { shareReportNow() }
            }
        }.start()
    }

    private fun startComposeUi() {
        if (composeStarted.getAndSet(true)) return
        ReportLog.info("Startup", "Compose wird gestartet (Thread ${Thread.currentThread().name})")
        // Exakt der Aufrufbaum, der am 11:24:50-Beweisprotokoll funktionierte
        // (v1.25, adaptiert): SeasonTheme -> viewModel -> HomeScreen.
        // KEIN Surface-Wrapper (blockiert auf Android 17 die Komposition).
        setContent {
            SeasonTheme {
                val vm = viewModel<ListViewModel>()
                HomeScreen(vm)
            }
        }
    }

    private val composeStarted = java.util.concurrent.atomic.AtomicBoolean(false)

    /** Minimaler Ladebildschirm im Saison-Look (ohne Compose, ohne Diagnose-Elemente). */
    private fun buildLoadingScreen(): android.widget.LinearLayout {
        val container = android.widget.LinearLayout(this)
        container.orientation = android.widget.LinearLayout.VERTICAL
        container.gravity = android.view.Gravity.CENTER
        container.setBackgroundColor(android.graphics.Color.rgb(0xF2, 0xF5, 0xFA))
        container.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.setPadding(48, 96, 48, 96)

        val text = android.widget.TextView(this)
        text.text = "EINKAUFSLISTE"
        text.textSize = 28f
        text.gravity = android.view.Gravity.CENTER
        text.setTextColor(android.graphics.Color.rgb(0x5B, 0x7F, 0xB5))
        container.addView(text)

        val hint = android.widget.TextView(this)
        hint.text = "Startet …"
        hint.textSize = 14f
        hint.gravity = android.view.Gravity.CENTER
        hint.setPadding(0, 16, 0, 0)
        hint.setTextColor(android.graphics.Color.rgb(0x71, 0x80, 0x9B))
        container.addView(hint)

        // Rueckfall-Knopf: unsichtbar, bis der automatische Start nicht klappen sollte
        val openButton = android.widget.Button(this)
        openButton.tag = "open"
        openButton.text = "Einkaufsliste öffnen"
        openButton.textSize = 16f
        openButton.setTextColor(android.graphics.Color.WHITE)
        openButton.setBackgroundColor(android.graphics.Color.rgb(0x5B, 0x7F, 0xB5))
        openButton.visibility = android.view.View.GONE
        openButton.setOnClickListener { startComposeUi() }
        val params = android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = 48
        openButton.layoutParams = params
        container.addView(openButton)
        return container
    }
}
