package com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.viewmodels.ObservationDetailsState
import com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.viewmodels.ObservationDetailsViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.viewmodels.observationDetailsViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.theme.Spacing
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun ObservationDetailsScreen(
    entryId: String,
    onBack: () -> Unit,
    viewModel: ObservationDetailsViewModel = viewModel(
        factory = observationDetailsViewModelFactory(entryId),
    ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.lg)
    ) {
        when (val current = state) {
            ObservationDetailsState.Loading -> {
                CircularProgressIndicator()
            }

            ObservationDetailsState.Error -> {
                Text("Unable to load this observation.")
            }

            is ObservationDetailsState.Loaded -> {
                val entry = current.entry

                if (entry == null) {
                    Text("This observation is unavailable or has been deleted.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(Spacing.lg)
                    ) {
                        item {
                            Text(
                                text = entry.title.ifBlank { "Untitled" },
                                style = MaterialTheme.typography.headlineSmall,
                            )
                        }

                        item {
                            val createdDate =
                                DateFormat.getDateTimeInstance()
                                    .format(Date(entry.createdAtMillis))

                            Text(
                                text = "Entry created: $createdDate",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }

                        if (entry.photoPaths.isEmpty()) {
                            item {
                                Text("No photos attached.")
                            }
                        } else {
                            items(
                                count = entry.photoPaths.size,
                            ) { index ->
                                val path = entry.photoPaths[index]
                                val photoLabel =
                                    "Photo ${index + 1} of " + entry.photoPaths.size

                                Column {
                                    AsyncImage(
                                        model = File(path),
                                        contentDescription = "$photoLabel: ${entry.title}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(4f / 3f),
                                        contentScale = ContentScale.Fit,
                                    )

                                    Text(
                                        text = photoLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }

                        item {
                            Text(
                                text = "Notes",
                                style = MaterialTheme.typography.titleMedium,
                            )

                            Spacer(
                                modifier = Modifier.height(Spacing.sm),
                            )

                            Text(
                                text = entry.notes.ifBlank {
                                    "No notes provided."
                                },
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}
