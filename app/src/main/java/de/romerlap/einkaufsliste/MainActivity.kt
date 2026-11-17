package de.romerlap.einkaufsliste

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.viewModel
import de.romerlap.einkaufsliste.ui.ListScreen
import de.romerlap.einkaufsliste.ui.theme.EinkaufslisteTheme

class MainActivity : ComponentActivity() {

    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Diagnose-Stufenbildschirm: reine Android-Views OHNE Compose.
        // Bleibt er sichtbar, haengt/failt Compose; verschwindet er, rendert Compose.
        setContentView(buildStatusScreen())
        step("App gestartet – Datenbank wird geoeffnet …")

        ReportLog.info("Startup", "MainActivity onCreate")

        // Datenbank-Initialisierung anstossen und im Hintergrund pruefen
        Thread {
            try {
                AppDatabase.get(application).itemDao().observeAll()
                runOnUiThread { step("Datenbank OK – Compose wird gestartet …") }
            } catch (e: Exception) {
                ReportLog.error("Startup", "Datenbank: ${e.javaClass.simpleName}: ${e.message}")
                runOnUiThread { step("Datenbank-FEHLER: ${e.javaClass.simpleName}") }
            }
        }.start()

        setContent {
            EinkaufslisteTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EinkaufslisteApp()
                }
            }
        }
    }

    private fun step(message: String) {
        ReportLog.info("Startup", message)
        if (::statusView.isInitialized) {
            statusView.post { statusView.text = message }
        }
    }

    /** Einfacher Status-Bildschirm ohne Compose-Abhaengigkeit. */
    private fun buildStatusScreen(): TextView {
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.gravity = Gravity.CENTER
        container.setBackgroundColor(Color.rgb(0xFF, 0xF6, 0xE5))
        container.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        val title = TextView(this)
        title.text = "Einkaufsliste startet …"
        title.textSize = 22f
        title.setPadding(32, 0, 32, 24)
        title.gravity = Gravity.CENTER
        title.setTextColor(Color.rgb(0xD6, 0x28, 0x28))
        container.addView(title)
        statusView = TextView(this)
        statusView.text = "Initialisierung …"
        statusView.textSize = 16f
        statusView.gravity = Gravity.CENTER
        statusView.setPadding(32, 0, 32, 0)
        statusView.setTextColor(Color.rgb(0x1D, 0x1D, 0x1B))
        container.addView(statusView)
        return container
    }
}

@Composable
fun EinkaufslisteApp(viewModel: ListViewModel = viewModel()) {
    ListScreen(viewModel)
}
