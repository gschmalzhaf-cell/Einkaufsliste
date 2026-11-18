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
        ReportLog.info("Startup", "Ladebildschirm gesetzt")

        Thread {
            // Auf DB-Vorwaermung warten (max. 3 Sekunden)
            (application as EinkaufslisteApp).dbReady.await(3, java.util.concurrent.TimeUnit.SECONDS)
            runOnUiThread {
                ReportLog.info("Startup", "DB bereit – Compose-Start geplant")
                // Mehrere echte Frames des Ladebildschirms zulassen, dann Compose starten
                loadingScreen.postDelayed({ startComposeUi() }, 800)
            }
        }.start()
    }

    private fun startComposeUi() {
        if (composeStarted.getAndSet(true)) return
        ReportLog.info("Startup", "Compose wird gestartet")
        setContent {
            EinkaufslisteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EinkaufslisteApp()
                }
            }
        }
    }

    private val composeStarted = java.util.concurrent.atomic.AtomicBoolean(false)

    /** Minimaler Ladebildschirm im Comic-Look (ohne Compose). */
    private fun buildLoadingScreen(): android.widget.TextView {
        val text = android.widget.TextView(this)
        text.text = "EINKAUFSLISTE"
        text.textSize = 28f
        text.gravity = android.view.Gravity.CENTER
        text.setTextColor(android.graphics.Color.rgb(0xD6, 0x28, 0x28))
        text.setBackgroundColor(android.graphics.Color.rgb(0xFF, 0xF6, 0xE5))
        text.setOnClickListener { startComposeUi() }
        return text
    }
}

@Composable
fun EinkaufslisteApp(viewModel: ListViewModel = viewModel()) {
    ListScreen(viewModel)
}
