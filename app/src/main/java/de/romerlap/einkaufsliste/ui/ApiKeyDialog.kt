package de.romerlap.einkaufsliste.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ApiKeyDialog(
    currentKey: String?,
    onDismiss: () -> Unit,
    onSave: (String?) -> Unit
) {
    var key by rememberSaveable { mutableStateOf(currentKey.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Mistral API-Schlüssel") },
        text = {
            Column {
                Text(
                    "Mit einem Schlüssel von console.mistral.ai werden Fotos und Rezepte per KI ausgewertet. Der Schlüssel wird verschlüsselt nur auf diesem Gerät gespeichert.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("API-Schlüssel") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(key.ifBlank { null }) },
                enabled = key.isNotBlank() || currentKey != null
            ) { Text("Speichern") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        }
    )
}
