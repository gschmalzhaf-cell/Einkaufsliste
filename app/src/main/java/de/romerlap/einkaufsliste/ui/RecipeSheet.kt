package de.romerlap.einkaufsliste.ui

import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.romerlap.einkaufsliste.ApiKeyStore
import de.romerlap.einkaufsliste.DraftItem
import de.romerlap.einkaufsliste.MistralApi
import de.romerlap.einkaufsliste.RecipeParser
import de.romerlap.einkaufsliste.ui.components.SelectableRow
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeSheet(
    onDismiss: () -> Unit,
    onAdd: (List<DraftItem>) -> Unit
) {
    var recipeText by rememberSaveable { mutableStateOf("") }
    var parsed by remember { mutableStateOf<List<DraftItem>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasKey = remember { ApiKeyStore.get(context) != null }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (parsed == null) {
            OutlinedTextField(
                value = recipeText,
                onValueChange = { recipeText = it },
                label = { Text("Rezepttext") },
                placeholder = { Text("z. B. 200 g Mehl\n2 Eier\n100 ml Milch …") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 16.dp)
            )
            OutlinedButton(
                onClick = {
                    val apiKey = ApiKeyStore.get(context)
                    if (apiKey != null) {
                        loading = true
                        failed = false
                        scope.launch {
                            try {
                                parsed = MistralApi.extractFromRecipeText(recipeText, apiKey)
                                    .map { DraftItem(name = it.name, detail = it.quantity) }
                            } catch (e: Exception) {
                                failed = true
                            } finally {
                                loading = false
                            }
                        }
                    } else {
                        parsed = RecipeParser.extractIngredients(recipeText)
                            .map { DraftItem(name = it.name, detail = it.quantity) }
                    }
                },
                enabled = !loading && recipeText.isNotBlank(),
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
            if (failed) {
                Text(
                    "KI-Auswertung fehlgeschlagen. Bitte erneut versuchen oder Schlüssel prüfen.",
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
                        supportingText = draft.detail,
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
