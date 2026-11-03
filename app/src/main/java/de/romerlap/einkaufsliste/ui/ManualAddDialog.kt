package de.romerlap.einkaufsliste.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import de.romerlap.einkaufsliste.ApiKeyStore
import de.romerlap.einkaufsliste.MistralApi
import de.romerlap.einkaufsliste.RetailCatalog
import kotlinx.coroutines.launch

@Composable
fun ManualAddDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, detail: String?, retailHint: String?) -> Unit
) {
    var name by rememberSaveable { mutableStateOf("") }
    var detail by rememberSaveable { mutableStateOf("") }
    var aiHint by rememberSaveable { mutableStateOf("") }
    var researching by rememberSaveable { mutableStateOf(false) }
    var researchFailed by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasKey = remember { ApiKeyStore.get(context) != null }
    val catalogHint = if (name.isNotBlank()) RetailCatalog.findHint(name.trim()) else null
    val shownHint = aiHint.ifBlank { catalogHint }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eintrag hinzufügen") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Artikel") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = detail,
                    onValueChange = { detail = it },
                    label = { Text("Menge / Hinweis (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (shownHint != null) {
                    Text(
                        "Handelsüblich (Rewe/Aldi): $shownHint",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (hasKey && name.isNotBlank()) {
                    TextButton(
                        onClick = {
                            val apiKey = ApiKeyStore.get(context) ?: return@TextButton
                            researching = true
                            researchFailed = false
                            scope.launch {
                                try {
                                    aiHint = MistralApi.researchRetailHint(
                                        name.trim(),
                                        detail.trim().ifBlank { null },
                                        apiKey
                                    )
                                } catch (e: Exception) {
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
                onClick = { onConfirm(name, detail.ifBlank { null }, shownHint) },
                enabled = name.isNotBlank()
            ) { Text("Hinzufügen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
