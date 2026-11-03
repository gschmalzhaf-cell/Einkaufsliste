package de.romerlap.einkaufsliste.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import de.romerlap.einkaufsliste.DraftItem
import de.romerlap.einkaufsliste.MistralApi
import de.romerlap.einkaufsliste.RecipeParser
import de.romerlap.einkaufsliste.RetailCatalog
import de.romerlap.einkaufsliste.WebRecipeFetcher
import de.romerlap.einkaufsliste.ui.components.SelectableRow
import kotlinx.coroutines.launch

private fun isLink(input: String): Boolean =
    input.startsWith("http://") || input.startsWith("https://")

private fun draftSupporting(draft: DraftItem): String? =
    listOfNotNull(
        draft.detail,
        draft.retailHint?.let { "Handelsüblich: $it" }
    ).joinToString(" · ").ifEmpty { null }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeSheet(
    onDismiss: () -> Unit,
    onAdd: (List<DraftItem>) -> Unit
) {
    var recipeInput by rememberSaveable { mutableStateOf("") }
    var parsed by remember { mutableStateOf<List<DraftItem>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasKey = remember { ApiKeyStore.get(context) != null }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (parsed == null) {
            OutlinedTextField(
                value = recipeInput,
                onValueChange = { recipeInput = it },
                label = { Text("Rezepttext oder Link") },
                placeholder = { Text("https://rezept-seite.de/… oder\n200 g Mehl\n2 Eier") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 16.dp)
            )
            OutlinedButton(
                onClick = {
                    val input = recipeInput.trim()
                    loading = true
                    error = null
                    scope.launch {
                        try {
                            val baseText = if (isLink(input)) {
                                WebRecipeFetcher.fetch(input)
                            } else {
                                input
                            }
                            val apiKey = ApiKeyStore.get(context)
                            parsed = if (apiKey != null) {
                                MistralApi.extractFromRecipeText(baseText, apiKey)
                                    .map { DraftItem(name = it.name, detail = it.quantity, retailHint = it.retailHint) }
                            } else {
                                RecipeParser.extractIngredients(baseText)
                                    .map {
                                        DraftItem(
                                            name = it.name,
                                            detail = it.quantity,
                                            retailHint = RetailCatalog.findHint(it.name)
                                        )
                                    }
                            }
                        } catch (e: Exception) {
                            error = if (isLink(input)) {
                                "Seite konnte nicht geladen werden. Bitte Link prüfen (manche Websites blockieren das automatische Laden)."
                            } else {
                                "KI-Auswertung fehlgeschlagen. Bitte erneut versuchen oder Schlüssel prüfen."
                            }
                        } finally {
                            loading = false
                        }
                    }
                },
                enabled = !loading && recipeInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (hasKey) "Zutaten per KI erkennen" else "Zutaten erkennen")
                }
            }
            if (error != null) {
                Text(
                    error!!,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            val list = parsed!!
            var selected by remember(list) {
                mutableStateOf(list.associateWith { true })
            }
            Text(
                "Erkannte Zutaten (${list.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(list, key = { it.name + (it.detail ?: "") }) { draft ->
                    SelectableRow(
                        title = draft.name,
                        supportingText = draftSupporting(draft),
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
                        Text("Hinzufügen (${chosen.size})")
                    }
                }
            }
        }
    }
}
