package de.romerlap.einkaufsliste.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    onDismiss: () -> Unit,
    onOpenApiKey: () -> Unit,
    onOpenReport: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            "Einstellungen",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenApiKey() }
                .padding(bottom = 4.dp),
            leadingContent = { Icon(Icons.Filled.Key, contentDescription = null) },
            headlineContent = { Text("Mistral API-Schlüssel") },
            supportingContent = { Text("KI-Funktionen für Foto, Rezept und Recherche") }
        )
        ListItem(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenReport() }
                .padding(bottom = 16.dp),
            leadingContent = { Icon(Icons.Filled.BugReport, contentDescription = null) },
            headlineContent = { Text("Diagnose-Bericht") },
            supportingContent = { Text("Fehler und Gerätedaten als Bericht kopieren oder teilen") }
        )
    }
}
