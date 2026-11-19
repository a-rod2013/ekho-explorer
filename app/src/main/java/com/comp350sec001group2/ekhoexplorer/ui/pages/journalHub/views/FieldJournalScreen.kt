package com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp350sec001group2.ekhoexplorer.ui.components.ObservationActionButton
import com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.viewmodels.JournalViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.viewmodels.journalViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle

@Composable
fun FieldJournalScreen(
    onOpenObservation: (String) -> Unit,
    onOpenSpecies: () -> Unit = {},
    viewModel: JournalViewModel = viewModel(factory = journalViewModelFactory),
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val filteredEntries by viewModel.filteredEntries.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val prefixSuggestions by viewModel.prefixSuggestions.collectAsStateWithLifecycle()

    val dimens = AppStyle.dimens
    val colors = AppStyle.colors

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimens.lg, vertical = dimens.sm),
            verticalArrangement = Arrangement.spacedBy(dimens.md),
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::updateSearchQuery,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search observations") },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                viewModel.updateSearchQuery("")
                            },
                        ) {
                            Text("Clear")
                        }
                    }
                },
            )

            if (prefixSuggestions.isNotEmpty()) {
                Column {
                    Text(
                        text = "Suggested titles and keywords",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary,
                    )

                    prefixSuggestions.forEach { suggestion ->
                        TextButton(
                            onClick = {
                                viewModel.updateSearchQuery(suggestion)
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = suggestion,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textPrimary,
                            )
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No journal entries yet.",
                        color = colors.textSecondary,
                    )
                }
            } else if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No matching observations.",
                        color = colors.textSecondary,
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(dimens.sm),
                    verticalArrangement = Arrangement.spacedBy(dimens.sm),
                ) {
                    items(
                        items = filteredEntries,
                        key = { it.id },
                    ) { entry ->
                        ObservationCard(
                            entry = entry,
                            onClick = {
                                onOpenObservation(entry.id)
                            },
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(dimens.lg)
        ) {
            ObservationActionButton()
        }
    }
}
