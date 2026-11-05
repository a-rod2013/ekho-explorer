package com.comp350sec001group2.ekhoexplorer.ui.map

import android.net.Uri
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalDraft
import org.maplibre.android.geometry.LatLng
import java.io.File

fun JournalDraft.toObservationMarker(): ObservationMarker? {

    // Read a labeled value from the saved Journal notes
    fun noteValue(label: String): String =
        notes.lineSequence()
            .map { it.trim() }
            .firstOrNull {
                it.startsWith("$label:", ignoreCase = true)
            }
            ?.substringAfter(":")
            ?.trim()
            .orEmpty()

    // Only restore entries with a valid observation category
    val category = ObservationCategory.entries.firstOrNull {
        it.name.equals(
            noteValue("Category"),
            ignoreCase = true
        )
    } ?: return null

    // Read the saved GPS coordinates
    val coordinates = noteValue("Location").split(",", limit = 2)

    if (coordinates.size != 2) return null

    val latitude = coordinates[0].trim().toDoubleOrNull()
        ?: return null

    val longitude = coordinates[1].trim().toDoubleOrNull()
        ?: return null

    if (latitude !in -90.0..90.0 ||
        longitude !in -180.0..180.0
    ) return null

    // Convert the Journal photo path to a URI
    val savedPhotoUri = photoPaths.firstOrNull()?.let { path ->
        Uri.fromFile(File(path)).toString()
    }.orEmpty()

    return ObservationMarker(
        id = id,
        commonName = title,
        scientificName = noteValue("Scientific name"),
        category = category,
        description = noteValue("Description"),
        photoUri = savedPhotoUri,
        location = LatLng(latitude, longitude)
    )
}
