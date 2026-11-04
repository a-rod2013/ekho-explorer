package com.comp350sec001group2.ekhoexplorer.ui.map

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun AddMarker(
    onObservationSaved: (
        commonName: String,
        scientificName: String,
        category: ObservationCategory,
        description: String,
        photoUri: Uri
    ) -> Unit
) {
    val context = LocalContext.current

    // Controls when the observation form is shown
    var showForm by remember { mutableStateOf(false) }

    // Stores the photo from the current observation
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    // Opens the camera and saves the photo
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            Log.d("AddMarker", "Photo saved: $photoUri")
            showForm = true
        } else {
            Log.d("AddMarker", "Photo capture cancelled")
        }
    }

    // Opens the camera with a new photo file
    Button(onClick = {
        val directory = File(context.filesDir, "observations")
        directory.mkdirs()

        val photoFile = File.createTempFile(
            "observation_",
            ".jpg",
            directory
        )

        photoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )

        cameraLauncher.launch(photoUri!!)
    }) {
        Text("Add Marker")
    }

    // Shows the observation form after taking a photo
    if (showForm) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text("New Observation") },
            text = {
                ObservationForm(
                    photoUri = photoUri!!,
                    onSave = { commonName, scientificName, category, description ->

                        // Send the observation details and photo to MapScreen
                        photoUri?.let { uri ->
                            onObservationSaved(
                                commonName,
                                scientificName,
                                category,
                                description,
                                uri
                            )
                        }

                        showForm = false
                    },
                    onCancel = {
                        showForm = false
                    }
                )
            },
            confirmButton = {}
        )
    }
}
