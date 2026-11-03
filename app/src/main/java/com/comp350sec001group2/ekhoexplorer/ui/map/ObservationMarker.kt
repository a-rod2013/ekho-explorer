package com.comp350sec001group2.ekhoexplorer.ui.map

import org.maplibre.android.geometry.LatLng

// Types of observations users can add to the map
enum class ObservationCategory {
    PLANT,
    ANIMAL
}

// Stores information about an observation
data class ObservationMarker(
    val id: String,                       // Unique ID
    val commonName: String,               // Common name, if known
    val scientificName: String,           // Scientific name, if known
    val category: ObservationCategory,    // Plant or animal
    val description: String,              // Observation details
    val photoUri: String,                 // Location of the photo
    val location: LatLng                  // GPS coordinates
) {
    // Name displayed on the map marker
    val displayName: String
        get() = when {
            commonName.isNotBlank() -> commonName
            scientificName.isNotBlank() -> scientificName
            category == ObservationCategory.PLANT -> "Unidentified Plant"
            else -> "Unidentified Animal"
        }
}
