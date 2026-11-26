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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun AddMarker(
    onLocationRequired: () -> Boolean,
    onObservationSaved: (
        commonName: String,
        scientificName: String,
        category: ObservationCategory,
        description: String,
        visibility: String,
        photoUri: Uri,
        onResult: (Boolean) -> Unit
    ) -> Unit
) {
    val context = LocalContext.current

    // Preserves the observation form state across process recreation.
    var showForm by rememberSaveable {
        mutableStateOf(false)
    }

    // Stores the photo URI as a string so it can be restored after process recreation.
    var photoUri by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    // Tracks whether the observation failed to save.
    var saveError by remember {
        mutableStateOf(false)
    }

    // Opens the camera and displays the observation form after a successful capture.
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && photoUri != null) {
            Log.d("AddMarker", "Photo saved: $photoUri")
            saveError = false
            showForm = true
        } else {
            Log.d("AddMarker", "Photo capture cancelled")
        }
    }

    // Creates a photo file and launches the camera when location is available.
    Button(
        onClick = {
            if (!onLocationRequired()) return@Button

            saveError = false

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
        }
    ) {
        Text("Add Marker")
    }

    // Displays the observation form while a captured photo is available.
    if (showForm && photoUri != null) {
        AlertDialog(
            onDismissRequest = {
                showForm = false
            },
            title = {
                Text("New Observation")
            },
            text = {
                ObservationForm(
                    photoUri = Uri.parse(photoUri!!),
                    saveError = saveError,
                    onSave = {
                            commonName,
                            scientificName,
                            category,
                            description,
                            visibility ->

                        photoUri?.let { uri ->
                            onObservationSaved(
                                commonName,
                                scientificName,
                                category,
                                description,
                                visibility,
                                Uri.parse(uri)
                            ) { success ->
                                if (success) {
                                    saveError = false
                                    showForm = false
                                } else {
                                    saveError = true
                                }
                            }
                        }
                    },
                    onCancel = {
                        showForm = false
                        saveError = false
                    }
                )
            },
            confirmButton = {}
        )
    }
}
