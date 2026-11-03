package de.romerlap.einkaufsliste.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.romerlap.einkaufsliste.DraftItem
import de.romerlap.einkaufsliste.RecipeParser
import de.romerlap.einkaufsliste.ui.components.SelectableRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeSheet(
    onDismiss: () -> Unit,
    onAdd: (List<DraftItem>) -> Unit
) {
    var recipeText by rememberSaveable { mutableStateOf("") }
    var parsed by remember { mutableStateOf<List<RecipeParser.ParsedIngredient>?>(null) }

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
                onClick = { parsed = RecipeParser.extractIngredients(recipeText) },
                enabled = recipeText.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Zutaten erkennen")
            }
        } else {
            val ingredients = parsed!!
            var selected by remember(parsed) {
                mutableStateOf(ingredients.associateWith { true })
            }
            Text(
                "Erkannte Zutaten (${ingredients.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(ingredients, key = { it.name }) { ingredient ->
                    SelectableRow(
                        title = ingredient.name,
                        supportingText = ingredient.quantity,
                        checked = selected[ingredient] == true,
                        onCheckedChange = { checked ->
                            selected = selected.toMutableMap().apply { put(ingredient, checked) }
                        }
                    )
                }
                item {
                    val chosen = ingredients.filter { selected[it] == true }
                    Button(
                        onClick = {
                            onAdd(chosen.map { DraftItem(name = it.name, detail = it.quantity) })
                        },
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
