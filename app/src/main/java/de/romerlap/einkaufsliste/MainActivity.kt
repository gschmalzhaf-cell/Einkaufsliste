package de.romerlap.einkaufsliste

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import de.romerlap.einkaufsliste.data.AppDatabase
import de.romerlap.einkaufsliste.ui.ListScreen
import de.romerlap.einkaufsliste.ui.theme.EinkaufslisteTheme

/**
 * Diagnose-Start: Die App öffnet zuerst einen reinen Android-View-Bildschirm
 * (ohne Compose). Von dort aus kann die Compose-UI bewusst gestartet und der
 * Diagnose-Bericht jederzeit geteilt werden – auch wenn Compose nicht rendert.
 */
class MainActivity : ComponentActivity() {

    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        ReportLog.info("Startup", "MainActivity onCreate (Diagnose-Modus)")
        setContentView(buildStatusScreen())
        step("Datenbank wird geprüft …")

        Thread {
            try {
                AppDatabase.get(application).itemDao().observeAll()
                runOnUiThread { step("Datenbank OK. Tippe unten auf „Einkaufsliste öffnen“.") }
            } catch (e: Exception) {
                ReportLog.error("Startup", "Datenbank: ${e.javaClass.simpleName}: ${e.message}")
                runOnUiThread { step("Datenbank-FEHLER: ${e.javaClass.simpleName} – bitte Bericht teilen.") }
            }
        }.start()
    }

    private fun startCompose(useComicFonts: Boolean) {
        ReportLog.info("Startup", "Compose-Start durch Nutzer (Comic-Schriften: $useComicFonts)")
        step("Compose wird gestartet …")
        setContent {
            EinkaufslisteTheme(useComicFonts = useComicFonts) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    EinkaufslisteApp()
                }
            }
        }
    }

    /** UI-Bisect in Stufen: Farbschema -> Surface -> Scaffold -> kompletter Screen. */
    @OptIn(ExperimentalMaterial3Api::class)
    private fun startStage(stage: Int) {
        ReportLog.info("Startup", "UI-Stufe $stage gestartet")
        setContent {
            EinkaufslisteTheme(useComicFonts = false) {
                when (stage) {
                    1 -> androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            androidx.compose.material3.Text("Stufe 1 OK: Farbschema + Surface")
                        }
                    }
                    2 -> androidx.compose.material3.Scaffold(
                        topBar = {
                            androidx.compose.material3.TopAppBar(
                                title = { androidx.compose.material3.Text("Stufe 2") }
                            )
                        }
                    ) { innerPadding ->
                        androidx.compose.material3.Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            androidx.compose.material3.Text("Stufe 2 OK: Scaffold + TopAppBar")
                        }
                    }
                    else -> {
                        ReportLog.info("Startup", "Stufe 3: ViewModel wird erstellt")
                        val vm = viewModel<ListViewModel>()
                        ReportLog.info("Startup", "Stufe 3: ViewModel erstellt")
                        EinkaufslisteApp(vm)
                    }
                }
            }
        }
    }

    /** Nackter Compose-Test: Text ohne eigenes Theme, Fonts oder Scaffold. */
    private fun startComposeMiniTest() {
        ReportLog.info("Startup", "Compose Mini-Test gestartet (nackter Text)")
        setContent {
            androidx.compose.material3.MaterialTheme {
                androidx.compose.material3.Text(
                    "Compose Mini-Test OK – Compose rendert!",
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color.Black
                )
            }
        }
    }

    private fun shareReport() {
        val report = try {
            ReportLog.generate(
                context = this,
                openCount = 0,
                doneCount = 0,
                tags = emptyList(),
                apiKeySet = false
            )
        } catch (e: Exception) {
            "Bericht konnte nicht erzeugt werden: ${e.javaClass.simpleName}: ${e.message}"
        }
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Einkaufsliste – Diagnose-Bericht")
            putExtra(Intent.EXTRA_TEXT, report)
        }
        startActivity(Intent.createChooser(send, "Diagnose-Bericht teilen"))
    }

    private fun step(message: String) {
        ReportLog.info("Startup", message)
        if (::statusView.isInitialized) {
            statusView.post { statusView.text = message }
        }
    }

    /** Reiner Android-View-Bildschirm ohne Compose-Abhängigkeit. */
    private fun buildStatusScreen(): LinearLayout {
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.gravity = Gravity.CENTER
        container.setBackgroundColor(Color.rgb(0xFF, 0xF6, 0xE5))
        container.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        container.setPadding(48, 96, 48, 96)

        val title = TextView(this)
        title.text = "EINKAUFSLISTE"
        title.textSize = 26f
        title.setTypeface(null, Typeface.BOLD)
        title.gravity = Gravity.CENTER
        title.setTextColor(Color.rgb(0xD6, 0x28, 0x28))
        container.addView(title)

        val version = TextView(this)
        version.text = "Diagnose-Modus v1.18"
        version.textSize = 14f
        version.gravity = Gravity.CENTER
        version.setPadding(0, 8, 0, 32)
        version.setTextColor(Color.rgb(0x4A, 0x44, 0x3A))
        container.addView(version)

        statusView = TextView(this)
        statusView.text = "Initialisierung …"
        statusView.textSize = 16f
        statusView.gravity = Gravity.CENTER
        statusView.setPadding(0, 0, 0, 48)
        statusView.setTextColor(Color.rgb(0x1D, 0x1D, 0x1B))
        container.addView(statusView)

        fun stageButton(label: String, stage: Int, bg: Int): Button {
            val b = Button(this)
            b.text = label
            b.textSize = 15f
            b.setTextColor(Color.WHITE)
            b.setBackgroundColor(bg)
            b.setOnClickListener { startStage(stage) }
            val lp = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            lp.topMargin = 16
            b.layoutParams = lp
            return b
        }
        container.addView(stageButton("Stufe 1: Farbschema + Surface", 1, Color.rgb(0xD6, 0x28, 0x28)))
        container.addView(stageButton("Stufe 2: Scaffold + TopAppBar", 2, Color.rgb(0x1D, 0x6F, 0xD1)))
        container.addView(stageButton("Stufe 3: Kompletter Bildschirm (ListScreen)", 3, Color.rgb(0x2E, 0x7D, 0x32)))



        val reportButton = Button(this)
        reportButton.text = "Diagnose-Bericht teilen"
        reportButton.textSize = 14f
        reportButton.setTextColor(Color.rgb(0x1D, 0x1D, 0x1B))
        reportButton.setBackgroundColor(Color.rgb(0xFF, 0xC8, 0x00))
        reportButton.setOnClickListener {
            Toast.makeText(this, "Bericht wird erzeugt …", Toast.LENGTH_SHORT).show()
            shareReport()
        }
        val params = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        params.topMargin = 24
        reportButton.layoutParams = params
        container.addView(reportButton)

        return container
    }
}

@Composable
fun EinkaufslisteApp(viewModel: ListViewModel = viewModel()) {
    ReportLog.info("Startup", "EinkaufslisteApp betreten (Startliste wird komponiert)")
    ListScreen(viewModel)
}
