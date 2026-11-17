package com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.views

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalDraft
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import com.comp350sec001group2.ekhoexplorer.ui.components.CameraActionButton
import com.comp350sec001group2.ekhoexplorer.ui.map.ObservationForm
import com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.viewmodels.JournalViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.viewmodels.journalViewModelFactory
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
fun FieldJournalScreen(
    onOpenSpecies: () -> Unit = {},
    viewModel: JournalViewModel = viewModel(factory = journalViewModelFactory)
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = dimens.lg,
                    vertical = dimens.sm
                ),
            verticalArrangement = Arrangement.spacedBy(dimens.md)
        ) {
            if (capturedPhoto == null) {
                error?.let { message ->
                    Text(
                        text = message,
                        color = colors.textPrimary
                    )
                }
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No journal entries yet.",
                        color = colors.textSecondary
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(dimens.sm),
                    verticalArrangement = Arrangement.spacedBy(dimens.sm)
                ) {
                    items(entries, key = { it.id }) { entry ->
                        ObservationCard(entry = entry)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(dimens.lg)
        ) {
            CameraActionButton(
                description = "New journal entry",
                onPhotoCaptured = { uri ->
                    draftId = UUID.randomUUID().toString()
                    capturedPhoto = uri.toString()
                    error = null
                },
                onError = { message ->
                    error = message
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
                Text("New Journal Entry")
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
                        Text("Saving journal entry…")
                    }

                    ObservationForm(
                        photoUri = Uri.parse(photo),
                        onSave = {
                                commonName,
                                scientificName,
                                category,
                                description,
                                visibility ->

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
                                        error = "Sign in before adding a journal entry."
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
                                                    error = "Could not get your location. Turn on Location and try again."
                                                } else {
                                                    val uri = Uri.parse(photo)

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
                                                        notes = """
                                                            Scientific name: $scientificName
                                                            Category: $category
                                                            Description: $description
                                                            Visibility: $visibility
                                                            Location: ${location.latitude}, ${location.longitude}
                                                        """.trimIndent(),
                                                        photoPaths = listOf(savedPath),
                                                        createdAtMillis = timestamp,
                                                        updatedAtMillis = timestamp
                                                    )

                                                    app.container.journalRepository
                                                        .saveDraft(entry)

                                                    capturedPhoto = null
                                                    draftId = null
                                                }
                                            } catch (e: CancellationException) {
                                                throw e
                                            } catch (e: Exception) {
                                                error = if (uploaded) {
                                                    "Uploaded to Firebase, but the local Journal copy could not be saved."
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
