package com.comp350sec001group2.ekhoexplorer.ui.map

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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

    // Load the observation photo
    val photoBitmap = remember(photoUri) {
        context.contentResolver.openInputStream(photoUri)?.use {
            BitmapFactory.decodeStream(it)
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("New Observation")

        // Show the captured photo
        photoBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Observation photo",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Optional common name
        OutlinedTextField(
            value = commonName,
            onValueChange = { commonName = it },
            label = { Text("Common Name (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        // Optional scientific name
        OutlinedTextField(
            value = scientificName,
            onValueChange = { scientificName = it },
            label = { Text("Scientific Name (Optional)") },
            modifier = Modifier.fillMaxWidth()
        )

        Text("Category")

        // Choose plant or animal
        ObservationCategory.entries.forEach { option ->
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = category == option,
                    onClick = { category = option }
                )

                Text(
                    option.name.lowercase()
                        .replaceFirstChar { it.uppercase() }
                )
            }
        }

        // Optional description
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }

            Button(
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
