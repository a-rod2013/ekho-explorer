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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun AddMarker(
    onObservationSaved: (
        commonName: String,
        scientificName: String,
        category: ObservationCategory,
        description: String,
        visibility: String,
        photoUri: Uri
    ) -> Unit
) {
    val context = LocalContext.current

    var showForm by rememberSaveable { mutableStateOf(false) }
    var photoUri by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // Opens the camera and saves the photo
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
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

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            photoFile
        )
        photoUri = uri.toString()
        cameraLauncher.launch(uri)
    }) {
        Text("Add Marker")
    }

    // Shows the observation form after taking a photo
    if (showForm && photoUri != null) {
        AlertDialog(
            onDismissRequest = { showForm = false },
            title = { Text("New Observation") },
            text = {
                ObservationForm(
                    photoUri = Uri.parse(photoUri),
                    onSave = { commonName, scientificName, category, description, visibility ->
                        // Send the observation details and photo to MapScreen
                        photoUri?.let { uri ->
                            onObservationSaved(
                                commonName,
                                scientificName,
                                category,
                                description,
                                visibility,
                                Uri.parse(uri)
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
