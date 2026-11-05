
package com.comp350sec001group2.ekhoexplorer.ui.pages.journalHub.views

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalDraft
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import java.io.File
import java.text.DateFormat
import java.util.Date

@Composable
fun ObservationCard(
    entry: JournalDraft,
) {
    val colors = AppStyle.colors
    val dimens = AppStyle.dimens

    // Extract category and location from observation notes
    val category = entry.notes.lines()
        .firstOrNull { it.startsWith("Category:", ignoreCase = true) }
        ?.substringAfter(":")
        ?.trim()

    val location = entry.notes.lines()
        .firstOrNull { it.startsWith("Location:", ignoreCase = true) }
        ?.substringAfter(":")
        ?.trim()

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            entry.photoPaths.firstOrNull()?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = entry.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(4f / 3f),
                    contentScale = ContentScale.Crop
                )
            }

            Column(
                modifier = Modifier.padding(dimens.md),
                verticalArrangement = Arrangement.spacedBy(dimens.xs)
            ) {
                // Common name
                Text(
                    text = entry.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Date
                Text(
                    text = DateFormat.getDateInstance().format(
                        Date(entry.createdAtMillis)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                // Category
                if (!category.isNullOrBlank()) {
                    Text(
                        text = category.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                // Location - same text size as date
                if (!location.isNullOrBlank()) {
                    Text(
                        text = "Location: $location",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}
