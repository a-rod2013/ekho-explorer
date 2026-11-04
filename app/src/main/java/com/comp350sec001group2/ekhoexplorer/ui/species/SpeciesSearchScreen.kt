package com.comp350sec001group2.ekhoexplorer.ui.species

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import com.comp350sec001group2.ekhoexplorer.ui.theme.EkhoExplorerTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.IconButton

data class Species(
    val name: String,
    val scientificName: String,
    val category: String,
    val imageUrl: String
)
val sampleSpecies = listOf(
    Species(
        name = "California Poppy",
        scientificName = "Eschscholzia californica",
        category = "Plants",
        imageUrl = "https://picsum.photos/id/976/800/800"
    ),
    Species(
        name = "California Sagebrush",
        scientificName = "Artemisia californica",
        category = "Plants",
        imageUrl = "https://picsum.photos/id/33/800/800"
    ),
    Species(
        name = "Black Sage",
        scientificName = "Salvia mellifera",
        category = "Plants",
        imageUrl = "https://picsum.photos/id/958/800/800"
    ),
    Species(
        name = "Western Bluebird",
        scientificName = "Sialia mexicana",
        category = "Animals",
        imageUrl = "https://picsum.photos/id/258/800/800"
    ),
    Species(
        name = "Coyote",
        scientificName = "Canis latrans",
        category = "Animals",
        imageUrl = "https://picsum.photos/id/718/800/800"
    ),
    Species(
        name = "Monarch Butterfly",
        scientificName = "Danaus plexippus",
        category = "Insects",
        imageUrl = "https://picsum.photos/id/82/800/800"
    ),
    Species(
        name = "California Mantis",
        scientificName = "Stagmomantis wheelerii",
        category = "Insects",
        imageUrl = "https://picsum.photos/id/18/800/800"
    ),
    Species(
        name = "Fly Agaric",
        scientificName = "Amanita muscaria",
        category = "Fungi",
        imageUrl = "https://picsum.photos/id/977/800/800"
    ),
    Species(
        name = "Split Gill",
        scientificName = "Schizophyllum commune",
        category = "Fungi",
        imageUrl = "https://picsum.photos/id/803/800/800"
    ),
    Species(
        name = "Rounded Earthstar",
        scientificName = "Geastrum saccatum",
        category = "Fungi",
        imageUrl = "https://picsum.photos/id/118/800/800"
    )
)

@Composable
fun SpeciesSearchScreen(
    onBack: () -> Unit = {}
) {
    var searchText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    val filteredSpecies = sampleSpecies.filter { species ->
        (selectedCategory == "All" || species.category == selectedCategory) &&
            (
                searchText.isBlank() ||
                    species.name.contains(searchText, ignoreCase = true) ||
                    species.scientificName.contains(searchText, ignoreCase = true)
                )
    }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppStyle.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppStyle.dimens.lg),
                verticalArrangement = Arrangement.spacedBy(AppStyle.dimens.md)
            ) {
                Row(
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
                        text = "Species Search",
                        style = MaterialTheme.typography.headlineSmall,
                        color = AppStyle.colors.textPrimary
                    )
                }
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(50)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(AppStyle.dimens.lg)
                ) {
                    Text(
                        text = "Explore Categories",
                        color = AppStyle.colors.textPrimary
                    )
                    Spacer(
                        modifier = Modifier.height(AppStyle.dimens.md)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Plants", "Animals", "Insects", "Fungi")
                            .forEach { category ->
                                FilterChip(
                                    selected = selectedCategory == category,
                                    onClick = {
                                        // Tapping again deselects the category
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
                        modifier = Modifier.height(AppStyle.dimens.lg)
                    )
                    Text(
                        text = "Sample Species",
                        color = AppStyle.colors.textPrimary
                    )
                }
            }
            items(
                items = filteredSpecies,
                key = { species -> species.name }
            ) { species ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppStyle.colors.background)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = AppStyle.dimens.lg,
                                vertical = AppStyle.dimens.sm
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = species.imageUrl,
                            contentDescription = species.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(80.dp)
                                .background(AppStyle.colors.surface)
                        )
                        Column(
                            modifier = Modifier.padding(
                                start = AppStyle.dimens.md
                            )
                        ) {
                            Text(
                                text = species.name,
                                color = AppStyle.colors.textPrimary
                            )
                            Text(
                                text = species.scientificName,
                                color = AppStyle.colors.textSecondary
                            )
                        }
                    }
                    HorizontalDivider(
                        color = AppStyle.colors.divider
                    )
                }
            }
            if (filteredSpecies.isEmpty()) {
                item {
                    Text(
                        text = "No matching species found.",
                        modifier = Modifier.padding(AppStyle.dimens.lg),
                        color = AppStyle.colors.textSecondary
                    )
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
