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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import kotlin.math.cos
import kotlin.math.sin

/**
 * V2-Hauptbildschirm: Cozy Seasons.
 * - Saisonale Palette automatisch per Datum (SeasonEngine) + Tipp-Wechsler zum Stöbern
 * - Weiches Himmels-Band im oberen Drittel + Motion-Layer mit Saison-Partikeln
 * - Kamera-Loch als saisonaler Anker (pulsierender Ring um das Cutout)
 * - Einträge per Tipp auf Karte oder Stift-Icon bearbeitbar
 * - Sichtbares KI-Badge + zuverlässige Persistenz des Mistral-API-Schlüssels
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
    var editingItem by remember { mutableStateOf<ShoppingItem?>(null) }
    var showRecipeSheet by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showKeyDialog by rememberSaveable { mutableStateOf(false) }
    var showReport by rememberSaveable { mutableStateOf(false) }
    var photoResult by remember { mutableStateOf<Bitmap?>(null) }

    val context = LocalContext.current
    var currentKey by remember { mutableStateOf<String?>(ApiKeyStore.get(context)) }
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
        // Saisonaler Himmels-Verlauf im oberen Drittel (wie in den V2-Design-Screens)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(310.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            season.sky.copy(alpha = 0.92f),
                            season.sky.copy(alpha = 0.45f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Motion-Layer: Saison-Partikel hinter allem
        ParticleLayer(modifier = Modifier.fillMaxSize())
        if (burstTick > 0) {
            ConfettiOverlay(seed = burstTick, season = season)
        }

        // Notch-Anker: Ring exakt im Statusleisten-Band (Cutout-Position,
        // Pixel 9 Pro: mittig). Je nach Saison mit individuellem Kranz.
        val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        Canvas(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topInset + 10.dp)
        ) {
            val cx = size.width / 2f
            val cy = (topInset.toPx()) / 2f
            val t = System.nanoTime() / 1_000_000_000.0
            val pulse = 26f + sin(t * 1.4).toFloat() * 2.5f
            drawCircle(season.primary, pulse, center = Offset(cx, cy), alpha = 0.55f)
            drawCircle(season.primary, pulse + 8f, center = Offset(cx, cy), alpha = 0.18f)
            when (season.season.particle) {
                Season.ParticleKind.SUN -> {
                    // Sonne wandert langsam um das Kamera-Loch
                    val a = t / 10.0
                    val sx = cx + (cos(a) * 74f).toFloat()
                    val sy = cy + (sin(a) * 28f).toFloat()
                    drawCircle(Color(0xFFF5D77A), 13f, center = Offset(sx, sy), alpha = 0.95f)
                    drawCircle(Color(0xFFF2A93B), 24f, center = Offset(sx, sy), alpha = 0.22f)
                }
                Season.ParticleKind.PETAL -> {
                    // Sanfter Blütenreigen um das Loch
                    for (i in 0 until 6) {
                        val a = t * 0.4 + i * (Math.PI / 3.0)
                        val px = cx + (cos(a) * (pulse + 7f)).toFloat()
                        val py = cy + (sin(a) * (pulse + 7f)).toFloat()
                        drawCircle(season.deco, 4.5f, center = Offset(px, py), alpha = 0.85f)
                    }
                }
                Season.ParticleKind.LEAF -> {
                    // Warme Herbstpunkte kreisen langsam
                    for (i in 0 until 4) {
                        val a = t * 0.35 + i * (Math.PI / 2.0)
                        val px = cx + (cos(a) * (pulse + 8f)).toFloat()
                        val py = cy + (sin(a) * (pulse + 6f)).toFloat()
                        drawCircle(season.secondary, 4f, center = Offset(px, py), alpha = 0.7f)
                    }
                }
                Season.ParticleKind.SNOW -> {
                    // Glitzernde Schneepunkte am Rand
                    for (i in 0 until 6) {
                        val a = i * (Math.PI / 3.0) + sin(t * 0.6 + i) * 0.15
                        val px = cx + (cos(a) * (pulse + 6f)).toFloat()
                        val py = cy + (sin(a) * (pulse + 6f)).toFloat()
                        drawCircle(Color.White, 3.5f, center = Offset(px, py), alpha = 0.9f)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
        ) {
            // ---- Kopf: Titel + Saison-Pille + KI-Status + Aktionen ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 4.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Einkaufsliste",
                        style = MaterialTheme.typography.headlineLarge,
                        color = season.primary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        // Tippbare Saison-Pille: zeigt Jahreszeit & Stimmung, wechselt bei Tipp die Saison-Vorschau
                        Surface(
                            shape = CircleShape,
                            color = season.surface.copy(alpha = 0.75f),
                            shadowElevation = 1.dp,
                            modifier = Modifier.clickable { season.onCycleSeason() }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "${season.season.emoji} ${season.season.label.uppercase()}",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        letterSpacing = 1.2.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = season.primary
                                )
                                Text(
                                    " · ${season.season.mood}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = season.soft
                                )
                            }
                        }
                    }
                }
                // Sichtbares KI-Status-Badge: zeigt sofort, dass der Mistral-Schlüssel aktiv hinterlegt ist
                if (currentKey != null) {
                    Surface(
                        shape = CircleShape,
                        color = season.secondary.copy(alpha = 0.16f),
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clickable { showKeyDialog = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = "Mistral KI aktiv",
                                tint = season.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "KI aktiv",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = season.secondary
                            )
                        }
                    }
                }
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

            // ---- Chips (im Cozy-Look: weiß / Saison-Primary, filtern sofort) ----
            if (allTags.isNotEmpty()) {
                val chipColors = FilterChipDefaults.filterChipColors(
                    containerColor = season.surface.copy(alpha = 0.9f),
                    labelColor = season.ink,
                    selectedContainerColor = season.primary,
                    selectedLabelColor = Color.White
                )
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
                            label = { Text("Alle (${items.size})") },
                            shape = CircleShape,
                            colors = chipColors,
                            border = null
                        )
                    }
                    item {
                        FilterChip(
                            selected = untaggedSelected,
                            onClick = { untaggedSelected = !untaggedSelected },
                            label = { Text("Ohne Tag") },
                            shape = CircleShape,
                            colors = chipColors,
                            border = null
                        )
                    }
                    items(allTags) { tag ->
                        FilterChip(
                            selected = tag in selectedTags,
                            onClick = {
                                selectedTags = if (tag in selectedTags) selectedTags - tag else selectedTags + tag
                            },
                            label = { Text("#$tag") },
                            shape = CircleShape,
                            colors = chipColors,
                            border = null
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
                        CozyEmptyCard(
                            season = season,
                            hasItemsAtAll = items.isNotEmpty(),
                            onAddClick = { showAddSheet = true }
                        )
                    }
                }
                if (open.isNotEmpty()) {
                    item { SectionPill("Offen", open.size, season.secondary) }
                    items(open, key = { it.id }) { item ->
                        CozyCard(
                            item = item,
                            season = season,
                            onToggle = {
                                if (open.size == 1) burstTick++ // letzter offener Artikel: Liste geschafft!
                                viewModel.toggle(item)
                            },
                            onEdit = { editingItem = item },
                            onDelete = { viewModel.delete(item) }
                        )
                    }
                }
                if (done.isNotEmpty()) {
                    item { SectionPill("Erledigt", done.size, season.primary) }
                    items(done, key = { it.id }) { item ->
                        CozyCard(
                            item = item,
                            season = season,
                            onToggle = { viewModel.toggle(item) },
                            onEdit = { editingItem = item },
                            onDelete = { viewModel.delete(item) }
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(104.dp)) }
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
                .shadow(8.dp, RoundedCornerShape(28.dp))
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
            AddOption(
                Icons.Filled.PhotoCamera,
                "Per Foto",
                if (currentKey != null) "Abfotografieren – Mistral-KI erkennt Artikel & Menge"
                else "Artikel oder Einkaufszettel abfotografieren"
            ) { showAddSheet = false; startPhotoCapture() }
            AddOption(
                Icons.Filled.ReceiptLong,
                "Rezept oder Link",
                if (currentKey != null) "Zutaten & Packungsgrößen per Mistral-KI erkennen"
                else "Zutaten automatisch aus Rezept erkennen"
            ) { showRecipeSheet = true; showAddSheet = false }
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
    editingItem?.let { itemToEdit ->
        ManualAddDialog(
            initialItem = itemToEdit,
            onDismiss = { editingItem = null },
            onConfirm = { name, detail, retailHint, tag ->
                viewModel.update(itemToEdit, name, detail, retailHint, tag)
                editingItem = null
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
            currentKey = currentKey,
            onDismiss = { showSettings = false },
            onOpenApiKey = { showKeyDialog = true; showSettings = false },
            onOpenReport = { showReport = true; showSettings = false }
        )
    }
    if (showReport) {
        val report = remember(items, currentKey) {
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
                currentKey = ApiKeyStore.get(context)
                showKeyDialog = false
            }
        )
    }
}

/**
 * Cozy-Leerzustand als illustrierte Willkommens-Karte im Jahreszeiten-Stil.
 */
@Composable
private fun CozyEmptyCard(
    season: SeasonColors,
    hasItemsAtAll: Boolean,
    onAddClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = season.surface.copy(alpha = 0.92f),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp)
            .clickable { onAddClick() }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(56.dp)
                    .background(season.primary.copy(alpha = 0.14f), CircleShape)
            ) {
                Text(season.season.emoji, fontSize = 28.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                if (hasItemsAtAll) "Keine Einträge für diesen Filter"
                else season.season.label + "s-Liste",
                style = MaterialTheme.typography.titleMedium,
                color = season.ink
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                if (hasItemsAtAll) "Wähle oben „Alle“, um wieder alle Artikel zu sehen."
                else season.season.emptyHint,
                style = MaterialTheme.typography.bodyMedium,
                color = season.soft,
                textAlign = TextAlign.Center
            )
        }
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
private fun SectionPill(title: String, count: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 6.dp, start = 4.dp)
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 5.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = color.copy(alpha = 0.8f)
        )
    }
}

/**
 * Cozy-Karte: weiche runde Form (Radius 26 dp), Feder-Physik beim Abhaken,
 * und per Tipp auf die Karte oder das Stift-Symbol direkt bearbeitbar.
 */
@Composable
private fun CozyCard(
    item: ShoppingItem,
    season: SeasonColors,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (item.isChecked) 0.98f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardScale"
    )
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = if (item.isChecked) season.surface.copy(alpha = 0.78f) else season.surface,
        shadowElevation = if (item.isChecked) 1.dp else 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .border(
                width = 1.dp,
                color = season.primary.copy(alpha = if (item.isChecked) 0.08f else 0.14f),
                shape = RoundedCornerShape(26.dp)
            )
            .clickable { onEdit() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            val checkScale by animateFloatAsState(
                targetValue = if (item.isChecked) 1.15f else 1f,
                animationSpec = spring(dampingRatio = 0.4f),
                label = "checkScale"
            )
            // 28-dp Checkbox-Kreis entsprechend dem Cozy-Seasons-Design-System
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .scale(checkScale)
                    .background(
                        if (item.isChecked) season.primary.copy(alpha = 0.18f) else season.background,
                        CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (item.isChecked) season.primary else season.soft.copy(alpha = 0.45f),
                        shape = CircleShape
                    )
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (item.isChecked) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val w = size.width
                        val h = size.height
                        val p = Path().apply {
                            moveTo(w * 0.18f, h * 0.52f)
                            lineTo(w * 0.43f, h * 0.76f)
                            lineTo(w * 0.84f, h * 0.24f)
                        }
                        drawPath(
                            p,
                            color = season.primary,
                            style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = if (item.isChecked) season.soft else season.ink,
                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else null,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.tag != null) {
                        Text(
                            "#${item.tag}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = season.primary,
                            modifier = Modifier
                                .background(season.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                if (item.detail != null) {
                    Text(
                        item.detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = season.soft,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (item.retailHint != null) {
                    Text(
                        "Handelsüblich: ${item.retailHint}",
                        style = MaterialTheme.typography.bodySmall,
                        color = season.soft.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }
            IconButton(
                onClick = onEdit,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = "Bearbeiten",
                    tint = season.soft.copy(alpha = 0.65f),
                    modifier = Modifier.size(19.dp)
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Löschen",
                    tint = season.soft.copy(alpha = 0.55f),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

