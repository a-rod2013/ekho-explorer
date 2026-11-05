package com.comp350sec001group2.ekhoexplorer.ui.pages.camera

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.io.File

@Composable
fun CameraScreen(
    onPosted: () -> Unit,
    viewModel: CameraViewModel = viewModel(factory = cameraViewModelFactory)
) {
    val context = LocalContext.current

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        viewModel.finishCapture(success)
    }

    val photoUri = viewModel.photoPath?.let { path ->
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(path)
        )
    }

    fun openCamera() {
        try {
            val directory = File(context.filesDir, "photos")
            check(directory.exists() || directory.mkdirs()) {
                "Could not create photo folder"
            }

            val file = File.createTempFile(
                "observation_",
                ".jpg",
                directory
            )

            // Prevents camera from launching when authentication/saving does not permit a new capture
            if (!viewModel.prepareCapture(file.absolutePath)) {
                file.delete()
                return
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            viewModel.captureFailed()
        }
    }

    LaunchedEffect(viewModel.posted) {
        if (viewModel.posted) {
            viewModel.discardPhoto()
            onPosted()
        }
    }

    BackHandler(
        enabled = viewModel.isPosting || viewModel.isSavingJournal
    ) {
        // Wait for the current save or submission
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Capture an observation",
            style = MaterialTheme.typography.headlineSmall
        )

        if (photoUri == null) {
            Button(
                onClick = { openCamera() },
                enabled = viewModel.pendingPath == null
            ) {
                Text("Take a photo")
            }
        } else {
            AsyncImage(
                model = photoUri,
                contentDescription = "Photo to post",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )

            OutlinedTextField(
                value = viewModel.caption,
                onValueChange = viewModel::updateCaption,
                label = { Text("Caption (optional)") },
                enabled = !viewModel.isPosting,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2
            )

            when {
                viewModel.isSavingJournal -> {
                    Text("Saving to journal…")
                }

                viewModel.journalError != null -> {
                    Text(
                        text = viewModel.journalError
                            ?: "Could not save the journal entry.",
                        color = MaterialTheme.colorScheme.error,
                    )

                    OutlinedButton(
                        onClick = viewModel::retryJournalSave,
                        enabled = !viewModel.isPosting,
                    ) {
                        Text("Retry journal save")
                    }
                }

                viewModel.journalSaved -> {
                    Text("Saved to your private journal. Photos stay on this device.")
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.postPhoto(photoUri) },
                    enabled = !viewModel.isPosting &&
                        !viewModel.posted &&
                        !viewModel.isSavingJournal &&
                        viewModel.journalSaved,
                ) {
                    Text(if (viewModel.isPosting) "Posting…" else "Post to feed")
                }

                OutlinedButton(
                    onClick = { openCamera() },
                    enabled = !viewModel.isPosting &&
                        !viewModel.isSavingJournal &&
                        viewModel.journalSaved &&
                        viewModel.pendingPath == null
                ) {
                    Text("Retake")
                }

                OutlinedButton(
                    onClick = { viewModel.discardPhoto() },
                    enabled = !viewModel.isPosting &&
                    !viewModel.isSavingJournal &&
                    viewModel.journalSaved,
                ) {
                    Text("Close")
                }
            }
        }

        if (viewModel.isPosting) {
            CircularProgressIndicator()
        }

        viewModel.error?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
