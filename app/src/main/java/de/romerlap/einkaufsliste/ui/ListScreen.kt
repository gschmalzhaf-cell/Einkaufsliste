package de.romerlap.einkaufsliste.ui

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.romerlap.einkaufsliste.ApiKeyStore
import de.romerlap.einkaufsliste.ListViewModel
import de.romerlap.einkaufsliste.data.ShoppingItem
import de.romerlap.einkaufsliste.ui.theme.ComicCard
import de.romerlap.einkaufsliste.ui.theme.Ink
import de.romerlap.einkaufsliste.ui.theme.comicOutline
import de.romerlap.einkaufsliste.ui.theme.comicSticker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListScreen(viewModel: ListViewModel) {
    val items by viewModel.items.collectAsState()
    val allTags = items.mapNotNull { it.tag }.distinct().sorted()
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showManualDialog by rememberSaveable { mutableStateOf(false) }
    var showRecipeSheet by rememberSaveable { mutableStateOf(false) }
    var showKeyDialog by rememberSaveable { mutableStateOf(false) }
    var selectedTags by rememberSaveable { mutableStateOf(listOf<String>()) }
    var untaggedSelected by rememberSaveable { mutableStateOf(false) }
    var photoResult by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    val currentKey = remember { ApiKeyStore.get(context) }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> photoResult = bitmap }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(null)
    }

    fun startPhotoCapture() {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        if (granted) cameraLauncher.launch(null) else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Einkaufsliste",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                modifier = Modifier.border(2.dp, Ink),
                actions = {
                    if (items.any { it.isChecked }) {
                        IconButton(onClick = { viewModel.deleteChecked() }) {
                            Icon(Icons.Filled.DoneAll, contentDescription = "Erledigte entfernen")
                        }
                    }
                    IconButton(onClick = { showKeyDialog = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Einstellungen")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddSheet = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.comicOutline(16.dp),
                icon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                text = { Text("Hinzufügen") }
            )
        }
    ) { innerPadding ->
        val selected = selectedTags.filter { it in allTags }.toSet()
        val hasSelection = selected.isNotEmpty() || untaggedSelected
        val visible = if (!hasSelection) {
            items
        } else {
            items.filter { (it.tag == null && untaggedSelected) || (it.tag != null && it.tag in selected) }
        }
        val prioritized = visible.sortedBy { item ->
            when {
                item.tag == null -> 0
                item.tag in selected -> 1
                else -> 2
            }
        }

        if (visible.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (items.isEmpty()) "Noch nichts auf der Liste.\nTippe auf „Hinzufügen“."
                    else "Keine Einträge für die gewählte Auswahl.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(8.dp)
            ) {
                if (allTags.isNotEmpty()) {
                    item {
                        TagFilterRow(
                            allTags = allTags,
                            selectedTags = selectedTags,
                            untaggedSelected = untaggedSelected,
                            onToggle = { tag ->
                                selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                            },
                            onToggleUntagged = { untaggedSelected = !untaggedSelected },
                            onClear = {
                                selectedTags = emptyList()
                                untaggedSelected = false
                            }
                        )
                    }
                }
                val open = prioritized.filter { !it.isChecked }
                val done = prioritized.filter { it.isChecked }
                if (open.isNotEmpty()) {
                    item { SectionHeader("Offen") }
                    items(open, key = { it.id }) { item ->
                        ItemRow(item, onToggle = { viewModel.toggle(item) }, onDelete = { viewModel.delete(item) })
                    }
                }
                if (done.isNotEmpty()) {
                    item { SectionHeader("Erledigt") }
                    items(done, key = { it.id }) { item ->
                        ItemRow(item, onToggle = { viewModel.toggle(item) }, onDelete = { viewModel.delete(item) })
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            Text(
                "Wie möchtest du ergänzen?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showManualDialog = true; showAddSheet = false }
                    .padding(bottom = 4.dp),
                leadingContent = { Icon(Icons.Filled.Edit, contentDescription = null) },
                headlineContent = { Text("Manuell") },
                supportingContent = { Text("Eintrag selbst tippen") }
            )
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showAddSheet = false; startPhotoCapture() }
                    .padding(bottom = 4.dp),
                leadingContent = { Icon(Icons.Filled.PhotoCamera, contentDescription = null) },
                headlineContent = { Text("Per Foto") },
                supportingContent = {
                    Text(
                        if (currentKey != null) "Abfotografieren, KI erkennt Artikel mit Menge"
                        else "Abfotografieren, Text wird erkannt"
                    )
                }
            )
            ListItem(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRecipeSheet = true; showAddSheet = false }
                    .padding(bottom = 24.dp),
                leadingContent = { Icon(Icons.Filled.ReceiptLong, contentDescription = null) },
                headlineContent = { Text("Aus Rezept") },
                supportingContent = {
                    Text(
                        if (currentKey != null) "Text oder Link einfügen – KI erkennt Zutaten"
                        else "Text oder Link einfügen, Zutaten erkennen"
                    )
                }
            )
        }
    }

    if (showManualDialog) {
        ManualAddDialog(
            onDismiss = { showManualDialog = false },
            onConfirm = { name, detail, retailHint, tag ->
                viewModel.add(name, detail, retailHint, tag)
                showManualDialog = false
            },
            existingTags = allTags
        )
    }

    if (showRecipeSheet) {
        RecipeSheet(
            onDismiss = { showRecipeSheet = false },
            onAdd = { drafts ->
                viewModel.addAll(drafts)
                showRecipeSheet = false
            },
            existingTags = allTags
        )
    }

    if (showKeyDialog) {
        ApiKeyDialog(
            currentKey = currentKey,
            onDismiss = { showKeyDialog = false },
            onSave = { key ->
                ApiKeyStore.set(context, key)
                showKeyDialog = false
            }
        )
    }

    photoResult?.let { bitmap ->
        PhotoResultSheet(
            bitmap = bitmap,
            onDismiss = { photoResult = null },
            onAdd = { drafts ->
                viewModel.addAll(drafts)
                photoResult = null
            },
            existingTags = allTags
        )
    }
}

@Composable
private fun TagFilterRow(
    allTags: List<String>,
    selectedTags: List<String>,
    untaggedSelected: Boolean,
    onToggle: (String) -> Unit,
    onToggleUntagged: () -> Unit,
    onClear: () -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedTags.isEmpty() && !untaggedSelected,
                onClick = onClear,
                label = { Text("Alle") },
                modifier = Modifier.comicOutline(16.dp)
            )
        }
        item {
            FilterChip(
                selected = untaggedSelected,
                onClick = onToggleUntagged,
                label = { Text("Ohne Tag") },
                modifier = Modifier.comicOutline(16.dp)
            )
        }
        items(allTags) { tag ->
            FilterChip(
                selected = tag in selectedTags,
                onClick = { onToggle(tag) },
                label = { Text(tag) },
                modifier = Modifier.comicOutline(16.dp)
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.headlineSmall,
        color = Ink,
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .comicSticker()
            .padding(horizontal = 10.dp, vertical = 2.dp)
    )
}

@Composable
private fun ItemRow(
    item: ShoppingItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    ComicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = item.isChecked, onCheckedChange = { onToggle() })
            Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (item.tag != null) {
                    Text(
                        "#${item.tag}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            if (item.detail != null) {
                Text(
                    item.detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (item.retailHint != null) {
                Text(
                    "Handelsüblich: ${item.retailHint}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Löschen")
            }
        }
    }
}
