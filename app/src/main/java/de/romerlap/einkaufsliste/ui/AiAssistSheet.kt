package de.romerlap.einkaufsliste.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
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
import de.romerlap.einkaufsliste.AisleClassifier
import de.romerlap.einkaufsliste.ApiKeyStore
import de.romerlap.einkaufsliste.DraftItem
import de.romerlap.einkaufsliste.MistralApi
import de.romerlap.einkaufsliste.ReportLog
import de.romerlap.einkaufsliste.RetailCatalog
import de.romerlap.einkaufsliste.ui.components.SelectableRow
import de.romerlap.einkaufsliste.ui.components.TagField
import de.romerlap.einkaufsliste.ui.v2.Season
import kotlinx.coroutines.launch

/**
 * Intelligenter Einkaufs-Assistent:
 * - Mit Mistral-Schlüssel: erstellt aus jeder freien Idee ("Lasagne für 4", "Grillabend")
 *   eine maßgeschneiderte Einkaufsliste inkl. Mengen, Packungsgrößen & Tag (ohne Doppelungen).
 * - Ohne Schlüssel: bietet sofort nutzbare Saison- & Wochen-Vorlagen komplett offline.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistSheet(
    season: Season,
    existingOpenItems: List<String>,
    existingTags: List<String>,
    onDismiss: () -> Unit,
    onAdd: (List<DraftItem>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val hasKey = remember { ApiKeyStore.get(context) != null }

    var idea by rememberSaveable { mutableStateOf("") }
    var tag by rememberSaveable { mutableStateOf("") }
    var drafts by remember { mutableStateOf<List<DraftItem>?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val quickIdeas = remember(season) { seasonalQuickIdeas(season) }

    fun generate(promptText: String) {
        val cleaned = promptText.trim()
        if (cleaned.isEmpty()) return
        val apiKey = ApiKeyStore.get(context)
        if (apiKey == null) {
            val preset = offlinePreset(cleaned, season)
            tag = preset.first
            drafts = preset.second
            return
        }
        loading = true
        error = null
        scope.launch {
            try {
                val res = MistralApi.suggestForPrompt(
                    idea = cleaned,
                    seasonLabel = season.label,
                    existingOpenItems = existingOpenItems,
                    apiKey = apiKey
                )
                if (tag.isBlank() && res.suggestedTag != null) {
                    tag = res.suggestedTag
                }
                drafts = res.items.map {
                    DraftItem(
                        name = it.name,
                        detail = it.quantity,
                        retailHint = it.retailHint ?: RetailCatalog.findHint(it.name)
                    )
                }
            } catch (e: Exception) {
                ReportLog.error("KI-Assistent", "${e.javaClass.simpleName}: ${e.message}")
                error = "KI-Vorschlag fehlgeschlagen. Bitte Verbindung oder Schlüssel prüfen."
            } finally {
                loading = false
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        if (drafts == null) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (hasKey) "KI-Einkaufsassistent" else "Saison- & Ideen-Assistent",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    if (hasKey) "Nenne ein Gericht oder einen Anlass – die KI stellt die Zutaten zusammen und berücksichtigt deine aktuelle Liste."
                    else "Wähle eine passende ${season.label}-Idee (mit Mistral-Schlüssel in den Einstellungen klappt auch jede freie Eingabe).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(quickIdeas) { chip ->
                        SuggestionChip(
                            onClick = {
                                idea = chip
                                if (!hasKey) generate(chip)
                            },
                            label = { Text(chip) }
                        )
                    }
                }

                if (hasKey) {
                    OutlinedTextField(
                        value = idea,
                        onValueChange = { idea = it },
                        label = { Text("Was planst du?") },
                        placeholder = { Text("z. B. Lasagne für 4 Personen, Grillabend, Brunch") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                    Button(
                        onClick = { generate(idea) },
                        enabled = !loading && idea.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 16.dp)
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Einkaufsliste vorschlagen")
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (error != null) {
                    Text(
                        error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }
        } else {
            val list = drafts!!
            var selected by remember(list) { mutableStateOf(list.associateWith { true }) }
            Text(
                "Vorschläge (${list.size})",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            TagField(
                tag = tag,
                onTagChange = { tag = it },
                existingTags = existingTags,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(list, key = { it.name + (it.detail ?: "") }) { draft ->
                    val aisle = AisleClassifier.classify(draft.name)
                    SelectableRow(
                        title = "${aisle.emoji} ${draft.name}",
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
                        onClick = {
                            val tagValue = tag.trim().ifBlank { null }
                            onAdd(chosen.map { it.copy(tag = it.tag ?: tagValue) })
                        },
                        enabled = chosen.isNotEmpty(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text("Ausgewählte übernehmen (${chosen.size})")
                    }
                }
            }
        }
    }
}

private fun seasonalQuickIdeas(season: Season): List<String> = when (season) {
    Season.SPRING -> listOf("Spargel klassisch", "Erdbeerkuchen", "Frühlings-Salat", "Wochen-Basics")
    Season.SUMMER -> listOf("Grillabend für 4", "Caprese & Ciabatta", "Sommer-Bowl", "Wochen-Basics")
    Season.AUTUMN -> listOf("Kürbissuppe", "Flammkuchen", "Apfelkuchen", "Wochen-Basics")
    Season.WINTER -> listOf("Gemütliches Raclette", "Pfannkuchen", "Pasta Bolognese", "Wochen-Basics")
}

private fun offlinePreset(choice: String, season: Season): Pair<String, List<DraftItem>> {
    fun item(name: String, qty: String) = DraftItem(
        name = name,
        detail = qty,
        retailHint = RetailCatalog.findHint(name)
    )
    return when {
        choice.contains("Kürbissuppe", true) -> "Kürbissuppe" to listOf(
            item("Hokkaido-Kürbis", "1 Stück"),
            item("Karotten", "300 g"),
            item("Zwiebeln", "2 Stück"),
            item("Ingwer", "1 kleines Stück"),
            item("Gemüsebrühe", "1 l"),
            item("Kokosmilch", "400 ml"),
            item("Baguette", "1 Stück")
        )
        choice.contains("Flammkuchen", true) -> "Flammkuchen" to listOf(
            item("Flammkuchenteig", "2 Rollen"),
            item("Crème fraîche", "200 g"),
            item("Speckwürfel", "150 g"),
            item("Zwiebeln", "2 Stück"),
            item("Schnittlauch", "1 Bund")
        )
        choice.contains("Apfelkuchen", true) -> "Apfelkuchen" to listOf(
            item("Äpfel", "1 kg"),
            item("Mehl", "250 g"),
            item("Zucker", "150 g"),
            item("Butter", "125 g"),
            item("Eier", "3 Stück"),
            item("Backpulver", "1 Päckchen"),
            item("Zimt", "1 TL")
        )
        choice.contains("Raclette", true) -> "Raclette" to listOf(
            item("Raclettekäse", "600 g"),
            item("Kartoffeln", "1 kg"),
            item("Champignons", "250 g"),
            item("Paprika", "2 Stück"),
            item("Mais", "1 Dose"),
            item("Baguette", "1 Stück")
        )
        choice.contains("Grillabend", true) -> "Grillabend" to listOf(
            item("Bratwurst", "1 Packung"),
            item("Baguette", "2 Stück"),
            item("Kräuterbutter", "1 Rolle"),
            item("Tomaten", "500 g"),
            item("Gurke", "1 Stück"),
            item("Feta", "200 g")
        )
        choice.contains("Spargel", true) -> "Spargel" to listOf(
            item("Spargel", "1 kg"),
            item("Kartoffeln", "750 g"),
            item("Schinken", "200 g"),
            item("Butter", "250 g"),
            item("Zitrone", "1 Stück")
        )
        choice.contains("Pasta", true) -> "Pasta" to listOf(
            item("Spaghetti", "500 g"),
            item("Hackfleisch", "400 g"),
            item("Passierte Tomaten", "500 g"),
            item("Zwiebeln", "1 Stück"),
            item("Knoblauch", "1 Knolle"),
            item("Parmesan", "100 g")
        )
        choice.contains("Pfannkuchen", true) -> "Pfannkuchen" to listOf(
            item("Mehl", "250 g"),
            item("Milch", "500 ml"),
            item("Eier", "3 Stück"),
            item("Butter", "100 g"),
            item("Apfelmus", "1 Glas")
        )
        else -> "${season.label}-Basics" to listOf(
            item("Milch", "1 l"),
            item("Butter", "250 g"),
            item("Eier", "6 Stück"),
            item("Brot", "1 Laib"),
            item("Äpfel", "1 kg"),
            item("Käse", "200 g")
        )
    }
}
