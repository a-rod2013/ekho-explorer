package com.comp350sec001group2.ekhoexplorer.ui.species

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.AsyncImage
import com.comp350sec001group2.ekhoexplorer.Business.Species.SpeciesApiResult
import com.comp350sec001group2.ekhoexplorer.Business.Species.SpeciesApiService
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import com.comp350sec001group2.ekhoexplorer.ui.theme.EkhoExplorerTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import androidx.compose.runtime.saveable.rememberSaveable

private fun matchesCategory(
    species: SpeciesApiResult,
    category: String
): Boolean {
    val taxon = species.category

    return when (category) {
        "Plants" -> taxon == "Plantae"
        "Insects" -> taxon == "Insecta"
        "Fungi" -> taxon == "Fungi"
        "Animals" -> taxon in setOf(
            "Animalia",
            "Mammalia",
            "Aves",
            "Reptilia",
            "Amphibia",
            "Actinopterygii",
            "Mollusca",
            "Arachnida"
        )
        else -> true
    }
}

@Composable
fun SpeciesSearchScreen(
    onBack: () -> Unit = {},
    showSearchField: Boolean = false,
    onSpeciesClick: (Int) -> Unit = {}
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    var speciesResults by remember {
        mutableStateOf<List<SpeciesApiResult>>(emptyList())
    }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val apiService = remember { SpeciesApiService() }

    LaunchedEffect(searchText, selectedCategory) {
        speciesResults = emptyList()
        errorMessage = null
        isLoading = false

        if (searchText.isBlank() && selectedCategory == "All") {
            return@LaunchedEffect
        }

        delay(700)
        isLoading = true

        try {
            speciesResults = if (searchText.isBlank()) {
                apiService.browseSpecies(selectedCategory)
            } else {
                apiService.searchSpecies(searchText)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = "Unable to load species. Please try again."
        } finally {
            isLoading = false
        }
    }

    val filteredSpecies = speciesResults.filter {
        matchesCategory(it, selectedCategory)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppStyle.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),

        topBar = {
            if (showSearchField) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = AppStyle.dimens.md,
                            vertical = AppStyle.dimens.sm
                        ),
                    placeholder = {
                        Text("Search by common or scientific name")
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge
                )
            }
        }
    ) { padding ->

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                AppStyle.dimens.md
            ),
            horizontalArrangement = Arrangement.spacedBy(AppStyle.dimens.md),
            verticalArrangement = Arrangement.spacedBy(AppStyle.dimens.md)
        ) {

            item(span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    Text(
                        text = "Explore Categories",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppStyle.colors.textPrimary
                    )

                    Spacer(
                        modifier = Modifier.height(AppStyle.dimens.sm)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Plants", "Animals", "Insects", "Fungi")
                            .forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = {
                                        selectedCategory =
                                            if (selectedCategory == category) {
                                                "All"
                                            } else {
                                                category
                                            }
                                    },
                                    label = {
                                        Text(category)
                                    }
                                )
                            }
                    }

                    Spacer(
                        modifier = Modifier.height(AppStyle.dimens.md)
                    )

                    Text(
                        text = "Species Results",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppStyle.colors.textPrimary
                    )
                }
            }

            if (isLoading) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(AppStyle.dimens.lg),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

            } else if (errorMessage != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = errorMessage ?: "",
                        color = AppStyle.colors.textSecondary
                    )
                }

            } else if (
                searchText.isBlank() &&
                selectedCategory == "All"
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Search by name or select a category to explore.",
                        color = AppStyle.colors.textSecondary
                    )
                }

            } else if (filteredSpecies.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "No matching species found.",
                        color = AppStyle.colors.textSecondary
                    )
                }

            } else {
                items(
                    items = filteredSpecies,
                    key = { species -> species.id }
                ) { species ->

                    Card(
                        onClick = {
                            onSpeciesClick(species.id)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        colors = CardDefaults.cardColors(
                            containerColor = AppStyle.colors.surface
                        )
                    ) {
                        Column {
                            if (species.imageUrl != null) {
                                AsyncImage(
                                    model = species.imageUrl,
                                    contentDescription = species.commonName
                                        ?: species.scientificName,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(4f / 3f)
                                        .background(AppStyle.colors.background),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = AppStyle.colors.textSecondary
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.padding(AppStyle.dimens.md),
                                verticalArrangement = Arrangement.spacedBy(
                                    AppStyle.dimens.sm
                                )
                            ) {
                                Text(
                                    text = species.commonName
                                        ?: species.scientificName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppStyle.colors.textPrimary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = species.scientificName,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = AppStyle.colors.textSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SpeciesSearchScreenPreview() {
    EkhoExplorerTheme {
        SpeciesSearchScreen()
    }
}
