package com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.viewmodels.JournalViewModel
import com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.viewmodels.journalViewModelFactory
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import androidx.compose.foundation.layout.statusBarsPadding

@Composable
fun FieldJournalScreen(
    onOpenSpecies: () -> Unit = {},
    viewModel: JournalViewModel = viewModel(factory = journalViewModelFactory)
) {
    val entries by viewModel.entries.collectAsStateWithLifecycle()
    val dimens = AppStyle.dimens
    val colors = AppStyle.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = dimens.lg, vertical = dimens.sm),
        verticalArrangement = Arrangement.spacedBy(dimens.md)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Field Journal",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )

            IconButton(
                onClick = onOpenSpecies
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = "Search species",
                    tint = colors.textPrimary
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
                    ObservationCard(
                        entry = entry,
                    )
                }
            }
        }
    }
}
