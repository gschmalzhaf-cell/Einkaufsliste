package de.romerlap.einkaufsliste.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.InputChip
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagField(
    tag: String,
    onTagChange: (String) -> Unit,
    existingTags: List<String>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = tag,
            onValueChange = onTagChange,
            label = { Text("Tag (optional, z. B. Grillabend)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (existingTags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                existingTags.forEach { existing ->
                    InputChip(
                        selected = existing == tag.trim(),
                        onClick = {
                            onTagChange(if (existing == tag.trim()) "" else existing)
                        },
                        label = { Text(existing) }
                    )
                }
            }
        }
    }
}
