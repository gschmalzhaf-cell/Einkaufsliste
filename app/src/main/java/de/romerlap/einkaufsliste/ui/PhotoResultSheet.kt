package de.romerlap.einkaufsliste.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import de.romerlap.einkaufsliste.ApiKeyStore
import de.romerlap.einkaufsliste.DraftItem
import de.romerlap.einkaufsliste.MistralApi
import de.romerlap.einkaufsliste.RetailCatalog
import de.romerlap.einkaufsliste.ui.components.SelectableRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoResultSheet(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onAdd: (List<DraftItem>) -> Unit
) {
    var drafts by remember { mutableStateOf<List<DraftItem>?>(null) }
    var failed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    LaunchedEffect(bitmap) {
        val apiKey = ApiKeyStore.get(context)
        if (apiKey != null) {
            try {
                drafts = MistralApi.extractFromImage(bitmap, apiKey)
                    .map { DraftItem(name = it.name, detail = it.quantity, retailHint = it.retailHint) }
            } catch (e: Exception) {
                failed = true
            }
        } else {
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { visionText ->
                    drafts = visionText.textBlocks
                        .flatMap { it.lines }
                        .map { it.text.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct()
                        .map { DraftItem(name = it, retailHint = RetailCatalog.findHint(it)) }
                }
                .addOnFailureListener { failed = true }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        when {
            failed -> {
                SheetMessage("Auswertung fehlgeschlagen. Bitte erneut versuchen oder Schlüssel prüfen.")
            }
            drafts == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            drafts!!.isEmpty() -> {
                SheetMessage("Kein Text auf dem Foto erkannt.")
            }
            else -> {
                val list = drafts!!
                var selected by remember(list) {
                    mutableStateOf(list.associateWith { true })
                }
                Text(
                    "Erkannte Einträge",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(list, key = { it.name + (it.detail ?: "") }) { draft ->
                        SelectableRow(
                            title = draft.name,
                            supportingText = listOfNotNull(
                                draft.detail,
                                draft.retailHint?.let { "Handelsüblich: $it" }
                            ).joinToString(" · ").ifEmpty { null },
                            checked = selected[draft] == true,
                            onCheckedChange = { checked ->
                                selected = selected.toMutableMap().apply { put(draft, checked) }
                            }
                        )
                    }
                    item {
                        val chosen = list.filter { selected[it] == true }
                        Button(
                            onClick = { onAdd(chosen) },
                            enabled = chosen.isNotEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text("Ausgewählte hinzufügen (${chosen.size})")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetMessage(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    )
}
