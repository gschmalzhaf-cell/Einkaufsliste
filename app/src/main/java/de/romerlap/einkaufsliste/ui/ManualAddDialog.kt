package de.romerlap.einkaufsliste.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.romerlap.einkaufsliste.AisleClassifier
import de.romerlap.einkaufsliste.ApiKeyStore
import de.romerlap.einkaufsliste.MistralApi
import de.romerlap.einkaufsliste.ReportLog
import de.romerlap.einkaufsliste.RetailCatalog
import de.romerlap.einkaufsliste.SmartInputParser
import de.romerlap.einkaufsliste.ui.components.TagField
import kotlinx.coroutines.launch

@Composable
fun ManualAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, detail: String?, retailHint: String?, tag: String?) -> Unit,
    existingTags: List<String> = emptyList(),
    initialItem: de.romerlap.einkaufsliste.data.ShoppingItem? = null
) {
    val isEdit = initialItem != null
    var name by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.name.orEmpty()) }
    var detail by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.detail.orEmpty()) }
    var tag by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.tag.orEmpty()) }
    var aiHint by rememberSaveable(initialItem?.id) { mutableStateOf(initialItem?.retailHint.orEmpty()) }
    var researching by rememberSaveable { mutableStateOf(false) }
    var researchFailed by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasKey = remember { ApiKeyStore.get(context) != null }

    // Smart-Eingabe: erkennt "2 kg Kartoffeln #Sonntag" automatisch
    val parsed = remember(name) { if (!isEdit) SmartInputParser.parse(name) else SmartInputParser.ParsedInput(name.trim(), null, null) }
    val effectiveName = parsed.name.ifBlank { name.trim() }
    val effectiveDetail = detail.trim().ifBlank { parsed.quantity.orEmpty() }.ifBlank { null }
    val effectiveTag = tag.trim().ifBlank { parsed.tag.orEmpty() }.ifBlank { null }

    val suggestions = remember(effectiveName, isEdit) {
        if (isEdit) emptyList() else RetailCatalog.suggest(effectiveName, limit = 5)
    }
    val catalogHint = if (effectiveName.isNotBlank()) RetailCatalog.findHint(effectiveName) else null
    val shownHint = aiHint.ifBlank { catalogHint }
    val detectedAisle = remember(effectiveName) {
        if (effectiveName.isNotBlank()) AisleClassifier.classify(effectiveName) else null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Eintrag bearbeiten" else "Eintrag hinzufügen") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Artikel") },
                    placeholder = { if (!isEdit) Text("z. B. 2 kg Kartoffeln #Sonntag") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
                if (suggestions.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        items(suggestions) { suggestion ->
                            SuggestionChip(
                                onClick = {
                                    // Übernimmt Vorschlag, behält ggf. bereits erkannte Menge/Tag
                                    if (parsed.quantity != null && detail.isBlank()) {
                                        detail = parsed.quantity
                                    }
                                    if (parsed.tag != null && tag.isBlank()) {
                                        tag = parsed.tag
                                    }
                                    name = suggestion
                                },
                                label = { Text(suggestion) }
                            )
                        }
                    }
                }
                if (!isEdit && (parsed.quantity != null || parsed.tag != null)) {
                    val parts = listOfNotNull(
                        parsed.name.takeIf { it.isNotBlank() },
                        parsed.quantity?.let { "Menge: $it" },
                        parsed.tag?.let { "#$it" }
                    )
                    Text(
                        "✨ Erkannt: ${parts.joinToString(" · ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("Menge / Hinweis (optional)") },
                    placeholder = { if (parsed.quantity != null) Text(parsed.quantity) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TagField(
                    tag = tag,
                    onTagChange = { tag = it },
                    existingTags = existingTags,
                    modifier = Modifier.padding(top = 8.dp)
                )
                OutlinedTextField(
                    value = aiHint,
                    onValueChange = { aiHint = it },
                    label = { Text("Packungsgröße / Handelsüblich (optional)") },
                    placeholder = { if (catalogHint != null) Text("Vorschlag: $catalogHint") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
                if (detectedAisle != null) {
                    Text(
                        "${detectedAisle.emoji} Abteilung: ${detectedAisle.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                if (shownHint != null && aiHint.isBlank()) {
                    Text(
                        "Handelsüblich (Rewe/Aldi): $shownHint",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (hasKey && effectiveName.isNotBlank()) {
                    TextButton(
                        onClick = {
                            val apiKey = ApiKeyStore.get(context) ?: return@TextButton
                            researching = true
                            researchFailed = false
                            scope.launch {
                                try {
                                    aiHint = MistralApi.researchRetailHint(
                                        effectiveName,
                                        effectiveDetail,
                                        apiKey
                                    )
                                } catch (e: Exception) {
                                    ReportLog.error("KI-Recherche", "Artikel '$effectiveName': ${e.javaClass.simpleName}: ${e.message}")
                                    researchFailed = true
                                } finally {
                                    researching = false
                                }
                            }
                        },
                        enabled = !researching
                    ) {
                        if (researching) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Packungsgröße per KI recherchieren")
                        }
                    }
                    if (researchFailed) {
                        Text(
                            "Recherche fehlgeschlagen. Bitte erneut versuchen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(effectiveName, effectiveDetail, shownHint, effectiveTag) },
                enabled = effectiveName.isNotBlank()
            ) { Text(if (isEdit) "Speichern" else "Hinzufügen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}


