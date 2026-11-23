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
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.material3.Button
import kotlin.time.Duration.Companion.seconds
import kotlin.math.roundToInt
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeciesSearchScreen(
    onBack: () -> Unit = {},
    showSearchField: Boolean = false,
    onSpeciesClick: (Int) -> Unit = {}
) {
    var searchText by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf("All") }

    var categoryExpanded by remember { mutableStateOf(false) }
    var locationExpanded by remember { mutableStateOf(false) }
    var taxonomyExpanded by remember { mutableStateOf(false) }
    var selectedTaxonomy by rememberSaveable { mutableStateOf("All") }
    var radiusMiles by rememberSaveable { mutableStateOf(5f) }
    var customQuery by rememberSaveable { mutableStateOf("") }
    var customPlaceId by rememberSaveable { mutableStateOf<Int?>(null) }
    var customLatitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var customLongitude by rememberSaveable { mutableStateOf<Double?>(null) }
    var customPlaceName by rememberSaveable { mutableStateOf("") }
    var placeSuggestions by remember { mutableStateOf<List<com.comp350sec001group2.ekhoexplorer.Business.Species.SpeciesPlace>>(emptyList()) }
    var locationRequestVersion by remember { mutableStateOf(0) }
    val context = LocalContext.current
    var permissionDenied by remember { mutableStateOf(false) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionDenied = permissions.values.none { it }
        locationRequestVersion++
    }
    val taxonomyOptions = when (selectedCategory) {
        "Animals" -> listOf("All", "Birds", "Mammals", "Reptiles", "Amphibians", "Fish")
        "Plants" -> listOf("All", "Flowering Plants", "Conifers", "Ferns", "Mosses")
        "Insects" -> listOf("All", "Butterflies & Moths", "Beetles", "Bees & Wasps", "Flies", "Dragonflies")
        "Fungi" -> listOf("All", "Mushroom-forming fungi", "Lichen-forming fungi", "Yeasts")
        else -> listOf("All")
    }
    val scientificTaxon = when (selectedTaxonomy) {
        "Birds" -> "Aves"; "Mammals" -> "Mammalia"; "Reptiles" -> "Reptilia"
        "Amphibians" -> "Amphibia"; "Fish" -> "Actinopterygii"
        "Flowering Plants" -> "Angiospermae"; "Conifers" -> "Pinopsida"
        "Ferns" -> "Polypodiopsida"; "Mosses" -> "Bryophyta"
        "Butterflies & Moths" -> "Lepidoptera"; "Beetles" -> "Coleoptera"
        "Bees & Wasps" -> "Hymenoptera"; "Flies" -> "Diptera"
        "Dragonflies" -> "Odonata"
        "Mushroom-forming fungi" -> "Agaricomycetes"
        "Lichen-forming fungi" -> "Lecanoromycetes"
        "Yeasts" -> "Saccharomycetes"
        else -> null
    }

    var showFilters by rememberSaveable {
        mutableStateOf(false)
    }

    var selectedLocation by rememberSaveable { mutableStateOf("Anywhere") }

    var speciesResults by remember {
        mutableStateOf<List<SpeciesApiResult>>(emptyList())
    }

    var isLoading by remember { mutableStateOf(false) }
    var usedNearbyFallback by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val apiService = remember { SpeciesApiService() }


    LaunchedEffect(selectedLocation, customQuery) {
        if (selectedLocation == "Custom" && customQuery.trim().length >= 2 && customPlaceId == null) {
            delay(450)
            try {
                placeSuggestions = apiService.searchPlaces(customQuery)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                placeSuggestions = emptyList()
            }
        } else {
            placeSuggestions = emptyList()
        }
    }

    LaunchedEffect(searchText, selectedCategory, selectedLocation, selectedTaxonomy,
        radiusMiles, customPlaceId, locationRequestVersion) {
        val query = searchText.trim()

        // Wait until the user finishes typing.
        delay(1500)

        // Require at least 3 characters for name searches.
        if (query.isNotEmpty() && query.length < 3) {
            speciesResults = emptyList()
            isLoading = false
            errorMessage = null
            return@LaunchedEffect
        }

        if (query.isEmpty() && selectedCategory == "All" && selectedTaxonomy == "All" && selectedLocation == "Anywhere") {
            speciesResults = emptyList()
            isLoading = false
            errorMessage = null
            return@LaunchedEffect
        }

        isLoading = true
        errorMessage = null
        usedNearbyFallback = false

        try {
            val placeId = when (selectedLocation) {
                "Custom" -> customPlaceId
                    ?: throw IllegalStateException(
                        "Choose a location from the Custom search results."
                    )
                else -> null
            }
            val coordinates = when (selectedLocation) {
                "Near CSUCI Campus" -> 34.1630 to -119.0426
                "Near Me" -> {
                    val permissionGranted = hasLocationPermission(context)
                    if (!permissionGranted) {
                        if (!permissionDenied) {
                            locationPermissionLauncher.launch(arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            ))
                        }
                        throw IllegalStateException("Allow location access to search Near Me.")
                    }
                    val location = currentLocation(context)
                        ?: throw IllegalStateException("Device location unavailable. Enable Location and try again.")
                    location.latitude to location.longitude
                }
                else -> null
            }
            var results = apiService.browseSpecies(
                category = selectedCategory,
                placeId = placeId,
                latitude = coordinates?.first,
                longitude = coordinates?.second,
                radius = (radiusMiles * 1.609344f).roundToInt(),
                taxonomy = scientificTaxon,
                query = query
            )
            if (
                selectedLocation == "Custom" &&
                results.isEmpty() &&
                customLatitude != null &&
                customLongitude != null
            ) {
                results = apiService.browseSpecies(
                    category = selectedCategory,
                    latitude = customLatitude,
                    longitude = customLongitude,
                    radius = 16,
                    taxonomy = scientificTaxon,
                    query = query
                )

                usedNearbyFallback = results.isNotEmpty()
            }

            speciesResults = results
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            errorMessage = e.message
                ?: "Unable to load species. Please try again."
        } finally {
            isLoading = false
        }
    }

    val filteredSpecies = speciesResults

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = AppStyle.colors.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),

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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AppStyle.dimens.sm)
                    ) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            placeholder = { Text("Search species...") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium
                        )
                        Button(
                            onClick = { showFilters = !showFilters },
                            modifier = Modifier.height(56.dp)
                        ) {
                            Text("Filters")

                            Icon(
                                imageVector = if (showFilters) {
                                    Icons.Filled.KeyboardArrowUp
                                } else {
                                    Icons.Filled.KeyboardArrowDown
                                },
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showFilters,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = AppStyle.dimens.md, bottom = AppStyle.dimens.sm),
                            verticalArrangement = Arrangement.spacedBy(AppStyle.dimens.sm)
                        ) {
                            Text(
                                text = "Filter Species",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(AppStyle.dimens.sm)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Category", style = MaterialTheme.typography.labelMedium)
                                    ExposedDropdownMenuBox(
                                        expanded = categoryExpanded,
                                        onExpandedChange = { categoryExpanded = !categoryExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedCategory,
                                            onValueChange = {},
                                            readOnly = true,
                                            singleLine = true,
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                                            },
                                            modifier = Modifier
                                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = categoryExpanded,
                                            onDismissRequest = { categoryExpanded = false }
                                        ) {
                                            listOf("All", "Animals", "Plants", "Insects", "Fungi")
                                                .forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option) },
                                                        onClick = {
                                                            selectedCategory = option
                                                            selectedTaxonomy = "All"
                                                            categoryExpanded = false
                                                        }
                                                    )
                                                }
                                        }
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Location", style = MaterialTheme.typography.labelMedium)
                                    ExposedDropdownMenuBox(
                                        expanded = locationExpanded,
                                        onExpandedChange = { locationExpanded = !locationExpanded }
                                    ) {
                                        OutlinedTextField(
                                            value = selectedLocation,
                                            onValueChange = {},
                                            readOnly = true,
                                            singleLine = true,
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationExpanded)
                                            },
                                            modifier = Modifier
                                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                                .fillMaxWidth()
                                        )
                                        ExposedDropdownMenu(
                                            expanded = locationExpanded,
                                            onDismissRequest = { locationExpanded = false }
                                        ) {
                                            listOf("Anywhere",
                                                "Near Me",
                                                "Near CSUCI Campus",
                                                "Custom")
                                                .forEach { option ->
                                                    DropdownMenuItem(
                                                        text = { Text(option) },
                                                        onClick = {
                                                            selectedLocation = option
                                                            locationExpanded = false
                                                        }
                                                    )
                                                }
                                        }
                                    }
                                }
                            }
                            if (selectedLocation == "Near Me" ||
                                selectedLocation == "Near CSUCI Campus"
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Search radius",
                                        style = MaterialTheme.typography.labelMedium
                                    )

                                    Text(
                                        text = "${radiusMiles.roundToInt()} miles",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                val radiusOptions = listOf(5f, 10f, 20f, 30f, 40f, 50f)

                                Slider(
                                    value = radiusOptions.indexOf(radiusMiles)
                                        .coerceAtLeast(0).toFloat(),
                                    onValueChange = {
                                        radiusMiles = radiusOptions[it.roundToInt().coerceIn(0, 5)]
                                    },
                                    valueRange = 0f..5f,
                                    steps = 4,
                                    thumb = {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.primary,
                                                    CircleShape
                                                )
                                        )
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            if (selectedLocation == "Custom") {
                                OutlinedTextField(
                                    value = customQuery,
                                    onValueChange = {
                                        customQuery = it
                                        customPlaceId = null
                                        customPlaceName = ""
                                        customLatitude = null
                                        customLongitude = null
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Find a city, county, or place") },
                                    singleLine = true
                                )
                                placeSuggestions.forEach { place ->
                                    TextButton(onClick = {
                                        customPlaceId = place.id
                                        customPlaceName = place.name
                                        customLatitude = place.latitude
                                        customLongitude = place.longitude
                                        customQuery = place.name
                                        placeSuggestions = emptyList()
                                    }) { Text(place.name) }
                                }
                                if (customPlaceId != null) Text("Selected: $customPlaceName")
                            }
                            Text("Taxonomy", style = MaterialTheme.typography.labelMedium)
                            ExposedDropdownMenuBox(
                                expanded = taxonomyExpanded,
                                onExpandedChange = { taxonomyExpanded = !taxonomyExpanded },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = selectedTaxonomy,
                                    onValueChange = {},
                                    readOnly = true,
                                    enabled = selectedCategory != "All",
                                    singleLine = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = taxonomyExpanded)
                                    },
                                    modifier = Modifier
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = taxonomyExpanded,
                                    onDismissRequest = { taxonomyExpanded = false }
                                ) {
                                    taxonomyOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(option) },
                                            onClick = {
                                                selectedTaxonomy = option
                                                taxonomyExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(AppStyle.dimens.sm))
                    Text(
                        text = "${selectedCategory} · ${if (selectedLocation == "Custom") customPlaceName.ifBlank { "Custom" } else selectedLocation}${if (selectedTaxonomy != "All") " · $selectedTaxonomy" else ""}",
                        style = AppStyle.type.caption,
                        color = AppStyle.colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(AppStyle.dimens.sm))

                    if (usedNearbyFallback && selectedLocation == "Custom") {
                        Text(
                            text = "Showing species within 10 miles of $customPlaceName",
                            style = AppStyle.type.caption,
                            color = AppStyle.colors.textSecondary
                        )
                        Spacer(modifier = Modifier.height(AppStyle.dimens.sm))
                    }

                    Text(
                        text = "Species Results",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppStyle.colors.textPrimary
                    )
                    if (searchText.isBlank() && selectedCategory != "All") {
                        Text(
                            text = "Showing up to 50 species. Search by name to find more.",
                            style = AppStyle.type.caption,
                            color = AppStyle.colors.textSecondary
                        )
                    }
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
                selectedCategory == "All" &&
                selectedLocation == "Anywhere"
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = "Search species or select a filter.",
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

private fun hasLocationPermission(context: Context): Boolean =
    context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

private suspend fun currentLocation(context: Context): Location? {
    if (!hasLocationPermission(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
        .filter { manager.isProviderEnabled(it) }
    if (providers.isEmpty()) return null
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        for (provider in providers) {
            val current = withTimeoutOrNull(10.seconds) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val signal = CancellationSignal()
                    continuation.invokeOnCancellation { signal.cancel() }
                    try {
                        manager.getCurrentLocation(provider, signal, context.mainExecutor) { found ->
                            if (continuation.isActive) continuation.resume(found)
                        }
                    } catch (e: SecurityException) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
            }
            if (current != null) return current
        }
    }
    return providers.mapNotNull { provider ->
        try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
    }.maxByOrNull { it.time }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun SpeciesSearchScreenPreview() {
    EkhoExplorerTheme {
        SpeciesSearchScreen()
    }
}
