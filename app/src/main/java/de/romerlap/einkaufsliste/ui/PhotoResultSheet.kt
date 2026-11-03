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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import de.romerlap.einkaufsliste.DraftItem
import de.romerlap.einkaufsliste.ui.components.SelectableRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoResultSheet(
    bitmap: Bitmap,
    onDismiss: () -> Unit,
    onAdd: (List<DraftItem>) -> Unit
) {
    var recognizedLines by remember { mutableStateOf<List<String>?>(null) }
    var failed by remember { mutableStateOf(false) }
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

    LaunchedEffect(bitmap) {
        recognizer.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { visionText ->
                recognizedLines = visionText.textBlocks
                    .flatMap { it.lines }
                    .map { it.text.trim() }
                    .filter { it.isNotEmpty() }
                    .distinct()
            }
            .addOnFailureListener { failed = true }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        when {
            failed -> {
                Text(
                    "Text konnte nicht erkannt werden.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
            recognizedLines == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            recognizedLines!!.isEmpty() -> {
                Text(
                    "Kein Text auf dem Foto erkannt.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                )
            }
            else -> {
                val lines = recognizedLines!!
                var selected by remember(lines) {
                    mutableStateOf(lines.associateWith { true })
                }
                Text(
                    "Erkannte Einträge",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(lines, key = { it }) { line ->
                        SelectableRow(
                            title = line,
                            supportingText = null,
                            checked = selected[line] == true,
                            onCheckedChange = { checked ->
                                selected = selected.toMutableMap().apply { put(line, checked) }
                            }
                        )
                    }
                    item {
                        val chosen = lines.filter { selected[it] == true }
                        Button(
                            onClick = { onAdd(chosen.map { DraftItem(name = it) }) },
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
