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

        // Ladebildschirm (reine Views), bis die Datenbank bereit ist
        setContentView(buildLoadingScreen())

        Thread {
            // Maximal 3 Sekunden auf die DB-Vorwaermung warten, dann starten
            (application as EinkaufslisteApp).dbReady.await(3, java.util.concurrent.TimeUnit.SECONDS)
            runOnUiThread {
                ReportLog.info("Startup", "DB bereit (oder Timeout) – Compose wird gestartet")
                setContent {
                    EinkaufslisteTheme {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            EinkaufslisteApp()
                        }
                    }
                }
            }
        }.start()
    }

    /** Minimaler Ladebildschirm im Comic-Look (ohne Compose). */
    private fun buildLoadingScreen(): android.widget.TextView {
        val text = android.widget.TextView(this)
        text.text = "EINKAUFSLISTE"
        text.textSize = 28f
        text.gravity = android.view.Gravity.CENTER
        text.setTextColor(android.graphics.Color.rgb(0xD6, 0x28, 0x28))
        text.setBackgroundColor(android.graphics.Color.rgb(0xFF, 0xF6, 0xE5))
        return text
    }
}

@Composable
fun EinkaufslisteApp(viewModel: ListViewModel = viewModel()) {
    ListScreen(viewModel)
}
