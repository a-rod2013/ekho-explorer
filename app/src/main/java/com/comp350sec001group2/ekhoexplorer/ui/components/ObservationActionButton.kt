package com.comp350sec001group2.ekhoexplorer.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalDraft
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import com.comp350sec001group2.ekhoexplorer.ui.map.ObservationForm
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

@Composable
fun ObservationActionButton(
    publishToFeed: Boolean = false,
    onSaved: () -> Unit = {}
) {
    val dimens = AppStyle.dimens
    val colors = AppStyle.colors

    val context = LocalContext.current
    val app = context.applicationContext as EkhoApp
    val scope = rememberCoroutineScope()

    var capturedPhoto by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var draftId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    val locationClient = remember(context) {
        LocationServices.getFusedLocationProviderClient(context)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        error = if (permissions.values.any { it }) {
            "Location enabled. Tap Save Observation again."
        } else {
            "Location permission is needed to save this observation."
        }
    }

    CameraActionButton(
        description = if (publishToFeed) {
            "New feed observation"
        } else {
            "New journal entry"
        },
        onPhotoCaptured = { uri ->
            draftId = UUID.randomUUID().toString()
            capturedPhoto = uri.toString()
            error = null
        },
        onError = { message ->
            error = message
        }
    )

    if (capturedPhoto == null) {
        error?.let { message ->
            AlertDialog(
                onDismissRequest = { error = null },
                title = { Text("Camera unavailable") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { error = null }) {
                        Text("OK")
                    }
                }
            )
        }
    }

    capturedPhoto?.let { photo ->
        AlertDialog(
            onDismissRequest = {
                if (!isSaving) {
                    capturedPhoto = null
                    draftId = null
                    error = null
                }
            },
            title = {
                Text(
                    if (publishToFeed) {
                        "New Feed Observation"
                    } else {
                        "New Journal Entry"
                    }
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(dimens.sm)
                ) {
                    error?.let { message ->
                        Text(
                            text = message,
                            color = colors.textPrimary
                        )
                    }

                    if (isSaving) {
                        Text("Saving observation…")
                    }

                    ObservationForm(
                        photoUri = Uri.parse(photo),
                        showVisibility = !publishToFeed,
                        saveButtonText = if (publishToFeed) {
                            "Post observation"
                        } else {
                            "Save Observation"
                        },
                        onSave = {
                                commonName,
                                scientificName,
                                category,
                                description,
                                selectedVisibility ->

                            val visibility = if (publishToFeed) {
                                "PUBLIC"
                            } else {
                                selectedVisibility
                            }

                            val user = EhkoCache.activeUser
                            val entryId = draftId

                            val hasLocationPermission =
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.ACCESS_FINE_LOCATION
                                ) == PackageManager.PERMISSION_GRANTED ||
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) == PackageManager.PERMISSION_GRANTED

                            if (!isSaving) {
                                when {
                                    !user.isSignedIn -> {
                                        error = "Sign in before saving an observation."
                                    }

                                    !hasLocationPermission -> {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }

                                    entryId != null -> {
                                        isSaving = true
                                        error = null

                                        scope.launch {
                                            var uploaded = false

                                            try {
                                                val cancellation =
                                                    CancellationTokenSource()

                                                val location = try {
                                                    withTimeoutOrNull(20_000L) {
                                                        locationClient.getCurrentLocation(
                                                            Priority.PRIORITY_HIGH_ACCURACY,
                                                            cancellation.token
                                                        ).await()
                                                    }
                                                } finally {
                                                    cancellation.cancel()
                                                }

                                                if (location == null) {
                                                    error =
                                                        "Could not get your location. Turn on Location and try again."
                                                } else {
                                                    val uri = Uri.parse(photo)

                                                    // Prepare the photo once for both uploads.
                                                    val photos =
                                                        app.container.photoPreparer.prepare(
                                                            listOf {
                                                                context.contentResolver
                                                                    .openInputStream(uri)
                                                                    ?: throw IllegalStateException(
                                                                        "Could not open the photo."
                                                                    )
                                                            }
                                                        )

                                                    // Save the observation to Firebase.
                                                    app.container.observationRepository
                                                        .addObservation(
                                                            observationId = entryId,
                                                            userId = user.userId,
                                                            commonName = commonName,
                                                            scientificName = scientificName,
                                                            category = category.name,
                                                            description = description,
                                                            visibility = visibility,
                                                            latitude = location.latitude,
                                                            longitude = location.longitude,
                                                            photos = photos
                                                        )

                                                    uploaded = true

                                                    // Keep a local Journal photo.
                                                    val savedPath =
                                                        app.container.journalPhotoStorage
                                                            .importPhoto(
                                                                ownerId = user.userId,
                                                                source = uri
                                                            )

                                                    val timestamp =
                                                        System.currentTimeMillis()

                                                    val entry = JournalDraft(
                                                        id = entryId,
                                                        ownerId = user.userId,
                                                        title = commonName,
                                                        notes = buildString {
                                                            appendLine("Scientific name: $scientificName")
                                                            appendLine("Category: $category")
                                                            appendLine("Description: $description")
                                                            appendLine("Visibility: $visibility")
                                                            appendLine("Location: ${location.latitude}, ${location.longitude}")
                                                        }.trimEnd(),
                                                        photoPaths = listOf(savedPath),
                                                        createdAtMillis = timestamp,
                                                        updatedAtMillis = timestamp
                                                    )

                                                    app.container.journalRepository
                                                        .saveDraft(entry)

                                                    // Only public observations go to Feed.
                                                    if (
                                                        publishToFeed &&
                                                        visibility == "PUBLIC"
                                                    ) {
                                                        val caption = buildString {
                                                            appendLine(commonName)

                                                            if (scientificName.isNotBlank()) {
                                                                appendLine(scientificName)
                                                            }

                                                            appendLine(
                                                                "Category: ${category.name}"
                                                            )

                                                            if (description.isNotBlank()) {
                                                                appendLine()
                                                                append(description)
                                                            }
                                                        }.trim()

                                                        app.container.feedRepository
                                                            .addPost(
                                                                caption = caption,
                                                                imageUrls = emptyList(),
                                                                photos = photos
                                                            )
                                                    }

                                                    capturedPhoto = null
                                                    draftId = null
                                                    onSaved()
                                                }
                                            } catch (e: CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                error = if (uploaded) {
                                                    "The observation reached Firebase, but a later save failed. Check Journal and Feed before retrying."
                                                } else {
                                                    "Could not save the observation. Please try again."
                                                }
                                            } finally {
                                                isSaving = false
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        onCancel = {
                            if (!isSaving) {
                                capturedPhoto = null
                                draftId = null
                                error = null
                            }
                        }
                    )
                }
            },
            confirmButton = {}
        )
    }
}
