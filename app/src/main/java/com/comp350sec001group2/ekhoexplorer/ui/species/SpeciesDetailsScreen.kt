package com.comp350sec001group2.ekhoexplorer.ui.species

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.comp350sec001group2.ekhoexplorer.Business.Species.SpeciesApiResult
import com.comp350sec001group2.ekhoexplorer.Business.Species.SpeciesApiService
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import kotlinx.coroutines.CancellationException
import androidx.compose.ui.draw.clip

@Composable
fun SpeciesDetailsScreen(
    speciesId: Int,
    onBack: () -> Unit
) {
    val apiService = remember { SpeciesApiService() }

    var species by remember(speciesId) {
        mutableStateOf<SpeciesApiResult?>(null)
    }

    var isLoading by remember(speciesId) {
        mutableStateOf(true)
    }

    var errorMessage by remember(speciesId) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(speciesId) {
        isLoading = true
        errorMessage = null

        try {
            species = apiService.getSpeciesDetails(speciesId)

            if (species == null) {
                errorMessage = "Species details are unavailable."
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = "Unable to load species details."
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = AppStyle.dimens.md)
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = AppStyle.colors.textPrimary
                )
            }

            Text(
                text = "Species Details",
                style = MaterialTheme.typography.titleMedium,
                color = AppStyle.colors.textPrimary
            )
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Text(
                    text = errorMessage ?: "",
                    modifier = Modifier.padding(AppStyle.dimens.md),
                    color = AppStyle.colors.textSecondary
                )
            }

            species != null -> {
                val details = species ?: return@Column

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(
                        AppStyle.dimens.md
                    )
                ) {
                    if (details.imageUrl != null) {
                        AsyncImage(
                            model = details.imageUrl,
                            contentDescription = details.commonName
                                ?: details.scientificName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(MaterialTheme.shapes.medium)
                        )
                    }

                    Text(
                        text = details.commonName
                            ?: details.scientificName,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppStyle.colors.textPrimary
                    )

                    Text(
                        text = details.scientificName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontStyle = FontStyle.Italic,
                        color = AppStyle.colors.textSecondary
                    )

                    HorizontalDivider(
                        color = AppStyle.colors.divider
                    )

                    Text(
                        text = "Species Information",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppStyle.colors.textPrimary
                    )

                    Text(
                        text = "Species ID: ${details.id}",
                        color = AppStyle.colors.textPrimary
                    )

                    Text(
                        text = "Category: ${when (details.category) {
                            "Plantae" -> "Plants"
                            "Aves" -> "Birds"
                            "Mammalia" -> "Mammals"
                            "Insecta" -> "Insects"
                            "Fungi" -> "Fungi"
                            "Reptilia" -> "Reptiles"
                            "Amphibia" -> "Amphibians"
                            "Actinopterygii" -> "Fish"
                            "Arachnida" -> "Arachnids"
                            "Mollusca" -> "Mollusks"
                            else -> details.category ?: "Unknown"
                        }}",
                        color = AppStyle.colors.textPrimary
                    )

                    HorizontalDivider(
                        color = AppStyle.colors.divider
                    )

                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppStyle.colors.textPrimary
                    )

                    Text(
                        text = details.description
                            ?: "No description available.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = AppStyle.colors.textSecondary
                    )

                    Spacer(
                        modifier = Modifier.height(AppStyle.dimens.md)
                    )
                }
            }
        }
    }
}
