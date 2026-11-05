
package com.comp350sec001group2.ekhoexplorer.ui.map

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ObservationForm(
    photoUri: Uri,
    onSave: (
        commonName: String,
        scientificName: String,
        category: ObservationCategory,
        description: String
    ) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current

    var commonName by rememberSaveable { mutableStateOf("") }
    var scientificName by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable {
        mutableStateOf(ObservationCategory.PLANT)
    }

    // Load the captured observation photo
    val photoBitmap = remember(photoUri) {
        context.contentResolver.openInputStream(photoUri)?.use {
            BitmapFactory.decodeStream(it)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // Larger photo preview
        photoBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Observation photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Common Name - Required
        Text(
            text = "Common Name",
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium
        )

        OutlinedTextField(
            value = commonName,
            onValueChange = { commonName = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Scientific Name - Optional
        Text(
            text = "Scientific Name (Optional)",
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium
        )

        OutlinedTextField(
            value = scientificName,
            onValueChange = { scientificName = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        // Category selection
        Text(
            text = "Category",
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            ObservationCategory.entries
                .chunked(2)
                .forEach { rowCategories ->

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rowCategories.forEach { option ->

                            FilterChip(
                                selected = category == option,
                                onClick = { category = option },
                                label = {
                                    Text(
                                        option.name.lowercase()
                                            .replaceFirstChar {
                                                it.uppercase()
                                            }
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
        }

        // Description
        Text(
            text = "Description",
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.labelMedium
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2,
            maxLines = 3
        )

        // Standard Android action button arrangement
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                enabled = commonName.isNotBlank(),
                onClick = {
                    onSave(
                        commonName.trim(),
                        scientificName.trim(),
                        category,
                        description.trim()
                    )
                }
            ) {
                Text("Save Observation")
            }
        }
    }
}
