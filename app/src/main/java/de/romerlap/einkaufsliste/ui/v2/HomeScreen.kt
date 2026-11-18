package de.romerlap.einkaufsliste.ui.v2

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import de.romerlap.einkaufsliste.ApiKeyStore
import de.romerlap.einkaufsliste.ListViewModel
import de.romerlap.einkaufsliste.ReportLog
import de.romerlap.einkaufsliste.data.ShoppingItem
import de.romerlap.einkaufsliste.ui.ApiKeyDialog
import de.romerlap.einkaufsliste.ui.ManualAddDialog
import de.romerlap.einkaufsliste.ui.PhotoResultSheet
import de.romerlap.einkaufsliste.ui.RecipeSheet
import de.romerlap.einkaufsliste.ui.ReportDialog
import de.romerlap.einkaufsliste.ui.SettingsSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sin

/**
 * V2-Hauptbildschirm: Cozy Seasons.
 * - Saisonale Palette automatisch per Datum (SeasonEngine)
 * - Motion-Layer mit Saison-Partikeln hinter dem Inhalt
 * - Kamera-Loch als saisonaler Anker (pulsierender Ring um das Cutout)
 * - Feder-Physik beim Abhaken, sofort filternde Chips, Cozy-Karten
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: ListViewModel = viewModel()) {
    val season = LocalSeason.current
    val items by viewModel.items.collectAsState()
    val allTags = items.mapNotNull { it.tag }.distinct().sorted()

    var selectedTags by rememberSaveable { mutableStateOf(listOf<String>()) }
    var untaggedSelected by rememberSaveable { mutableStateOf(false) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var showManualDialog by rememberSaveable { mutableStateOf(false) }
    var showRecipeSheet by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showKeyDialog by rememberSaveable { mutableStateOf(false) }
    var showReport by rememberSaveable { mutableStateOf(false) }
    var photoResult by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    var currentKey by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        currentKey = withContext(Dispatchers.IO) { ApiKeyStore.get(context) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap -> photoResult = bitmap }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) cameraLauncher.launch(null) }

    fun startPhotoCapture() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (granted) cameraLauncher.launch(null) else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    val selected = selectedTags.filter { it in allTags }.toSet()
    val visible = if (selected.isEmpty() && !untaggedSelected) {
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
    val open = prioritized.filter { !it.isChecked }
    val done = prioritized.filter { it.isChecked }
    var burstTick by remember { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(season.background)) {
        // Motion-Layer: Saison-Partikel hinter allem
        ParticleLayer(modifier = Modifier.fillMaxSize())
        if (burstTick > 0) {
            ConfettiOverlay(seed = burstTick, season = season)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
        ) {
            // ---- Kopf: Kamera-Loch-Anker + Titel + Saison ----
            Box(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val pulse = 26f + sin(System.nanoTime() / 800_000_000.0).toFloat() * 2.5f
                    drawCircle(season.primary, pulse, center = Offset(cx, cy), alpha = 0.55f)
                    drawCircle(season.primary, pulse + 8f, center = Offset(cx, cy), alpha = 0.18f)
                }
                Text(
                    "Einkaufsliste",
                    style = MaterialTheme.typography.headlineLarge,
                    color = season.primary,
                    modifier = Modifier.padding(start = 24.dp).align(Alignment.CenterStart)
                )
                Text(
                    season.season.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = season.soft,
                    modifier = Modifier
                        .padding(start = 26.dp, bottom = 10.dp)
                        .align(Alignment.BottomStart)
                )
                Row(modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
                    if (items.any { it.isChecked }) {
                        IconButton(onClick = {
                            burstTick++
                            viewModel.deleteChecked()
                        }) {
                            Icon(Icons.Filled.DoneAll, contentDescription = "Erledigte entfernen", tint = season.secondary)
                        }
                    }
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Einstellungen", tint = season.soft)
                    }
                }
            }

            // ---- Chips (filtern sofort) ----
            if (allTags.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTags.isEmpty() && !untaggedSelected,
                            onClick = {
                                selectedTags = emptyList()
                                untaggedSelected = false
                            },
                            label = { Text("Alle") },
                            shape = CircleShape
                        )
                    }
                    item {
                        FilterChip(
                            selected = untaggedSelected,
                            onClick = { untaggedSelected = !untaggedSelected },
                            label = { Text("Ohne Tag") },
                            shape = CircleShape
                        )
                    }
                    items(allTags) { tag ->
                        FilterChip(
                            selected = tag in selectedTags,
                            onClick = {
                                selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                            },
                            label = { Text(tag) },
                            shape = CircleShape
                        )
                    }
                }
            }

            // ---- Liste cozy Karten ----
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (visible.isEmpty()) {
                    item {
                        Text(
                            "Noch nichts auf der Liste.\nTippe unten auf „Hinzufügen“.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = season.soft,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                if (open.isNotEmpty()) {
                    item { SectionPill("Offen", season.secondary) }
                    items(open, key = { it.id }) { item ->
                        CozyCard(
                            item = item,
                            season = season,
                            onToggle = {
                                if (open.size == 1) burstTick++ // letzter offener Artikel: Liste geschafft!
                                viewModel.toggle(item)
                            },
                            onDelete = { viewModel.delete(item) }
                        )
                    }
                }
                if (done.isNotEmpty()) {
                    item { SectionPill("Erledigt", season.primary) }
                    items(done, key = { it.id }) { item ->
                        CozyCard(item = item, season = season, onToggle = { viewModel.toggle(item) }, onDelete = { viewModel.delete(item) })
                    }
                }
                item { Spacer(modifier = Modifier.height(96.dp)) }
            }
        }

        // ---- FAB-Pille unten mittig ----
        ExtendedFloatingActionButton(
            onClick = { showAddSheet = true },
            containerColor = season.primary,
            contentColor = Color.White,
            shape = RoundedCornerShape(28.dp),
            icon = { Icon(Icons.Filled.Add, contentDescription = null) },
            text = { Text("Hinzufügen") },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .shadow(6.dp, RoundedCornerShape(28.dp))
        )
    }

    // ---- Sheets & Dialoge ----
    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }) {
            Text(
                "Wie möchtest du ergänzen?",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            AddOption(Icons.Filled.Edit, "Manuell", "Eintrag selbst tippen") { showManualDialog = true; showAddSheet = false }
            AddOption(Icons.Filled.PhotoCamera, "Per Foto", "Artikel abfotografieren") { showAddSheet = false; startPhotoCapture() }
            AddOption(Icons.Filled.ReceiptLong, "Rezept oder Link", "Zutaten automatisch erkennen") { showRecipeSheet = true; showAddSheet = false }
            Spacer(modifier = Modifier.height(16.dp))
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
    if (showSettings) {
        SettingsSheet(
            onDismiss = { showSettings = false },
            onOpenApiKey = { showKeyDialog = true; showSettings = false },
            onOpenReport = { showReport = true; showSettings = false }
        )
    }
    if (showReport) {
        val report = remember(items) {
            ReportLog.generate(
                context = context,
                openCount = items.count { !it.isChecked },
                doneCount = items.count { it.isChecked },
                tags = allTags,
                apiKeySet = currentKey != null
            )
        }
        ReportDialog(report = report, onDismiss = { showReport = false })
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
}

/**
 * Konfetti-Burst: Saison-farbige Partikel explodieren von der Bildmitte aus,
 * fliegen mit Schwerkraft auseinander und verblassen. Laeuft ~1,4 s.
 */
@Composable
private fun ConfettiOverlay(seed: Int, season: SeasonColors) {
    val progress = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(seed) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 1400,
                easing = androidx.compose.animation.core.LinearEasing
            )
        )
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val p = progress.value
        if (p >= 1f) return@Canvas
        val rand = kotlin.random.Random(seed)
        val cx = size.width / 2f
        val cy = size.height * 0.55f
        repeat(90) { i ->
            val angle = rand.nextFloat() * 2f * Math.PI.toFloat()
            val dist = 0.08f + rand.nextFloat() * 0.35f
            val spin = rand.nextFloat() * 720f * p
            val color = when (i % 4) {
                0 -> season.primary
                1 -> season.secondary
                2 -> season.deco
                else -> season.deco2
            }
            val px = cx + (kotlin.math.cos(angle) * dist * p * size.width)
            val py = cy + (kotlin.math.sin(angle) * dist * p * size.height * 0.6f)
                + 0.30f * p * p * size.height
            val alpha = (1f - p).coerceIn(0f, 1f)
            rotate(spin, pivot = androidx.compose.ui.geometry.Offset(px, py + 6f)) {
                drawRoundRect(
                    color = color,
                    topLeft = androidx.compose.ui.geometry.Offset(px, py),
                    size = androidx.compose.ui.geometry.Size(9f, 14f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f),
                    alpha = alpha
                )
            }
        }
    }
}

@Composable
private fun AddOption(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        leadingContent = { Icon(icon, contentDescription = null) },
        headlineContent = { Text(title) },
        supportingContent = { Text(sub) }
    )
}

@Composable
private fun SectionPill(title: String, color: Color) {
    Text(
        title,
        style = MaterialTheme.typography.titleMedium,
        color = color,
        modifier = Modifier
            .padding(top = 8.dp)
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp)
    )
}

/** Cozy-Karte: weiche runde Form, Feder-Physik beim Abhaken. */
@Composable
private fun CozyCard(
    item: ShoppingItem,
    season: SeasonColors,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (item.isChecked) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardScale"
    )
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = season.surface,
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            val checkScale by animateFloatAsState(
                targetValue = if (item.isChecked) 1.2f else 1f,
                animationSpec = spring(dampingRatio = 0.35f),
                label = "checkScale"
            )
            Box(
                modifier = Modifier
                    .scale(checkScale)
                    .background(
                        if (item.isChecked) season.primary.copy(alpha = 0.2f) else season.background,
                        CircleShape
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.height(20.dp).fillMaxWidth(0.2f)) {
                    val r = size.height / 2f
                    drawCircle(season.soft, r, center = Offset(size.width / 2f, size.height / 2f), alpha = 0.5f)
                    if (item.isChecked) {
                        val p = Path().apply {
                            moveTo(size.width / 2f - r * 0.45f, size.height / 2f)
                            lineTo(size.width / 2f - r * 0.1f, size.height / 2f + r * 0.4f)
                            lineTo(size.width / 2f + r * 0.5f, size.height / 2f - r * 0.5f)
                        }
                        drawPath(p, season.primary, style = Stroke(width = 4f))
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    item.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (item.isChecked) season.soft else season.ink
                )
                if (item.detail != null || item.tag != null) {
                    Text(
                        listOfNotNull(item.tag?.let { "#$it" }, item.detail).joinToString(" · "),
                        style = MaterialTheme.typography.labelMedium,
                        color = season.primary
                    )
                }
                if (item.retailHint != null) {
                    Text(
                        "Handelsüblich: ${item.retailHint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = season.soft
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Löschen", tint = season.soft.copy(alpha = 0.6f))
            }
        }
    }
}
