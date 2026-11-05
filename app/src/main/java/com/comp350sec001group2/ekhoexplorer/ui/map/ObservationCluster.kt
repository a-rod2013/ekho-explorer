package com.comp350sec001group2.ekhoexplorer.ui.map

import android.location.Location
import org.maplibre.android.geometry.LatLng

// Stores observations grouped at one location
data class ObservationCluster(
    val observations: List<ObservationMarker>
) {
    val location: LatLng
        get() = observations.first().location

    val count: Int
        get() = observations.size
}

// Calculate distance between two GPS locations
fun distanceBetween(
    first: LatLng,
    second: LatLng
): Float {
    val results = FloatArray(1)

    Location.distanceBetween(
        first.latitude,
        first.longitude,
        second.latitude,
        second.longitude,
        results
    )

    return results[0]
}

// Group nearby observations within 10 meters
fun groupObservations(
    observations: List<ObservationMarker>,
    radiusMeters: Float = 10f
): List<ObservationCluster> {

    val groups = mutableListOf<MutableList<ObservationMarker>>()

    for (observation in observations) {
        val group = groups.firstOrNull { existing ->
            existing.any {
                distanceBetween(
                    it.location,
                    observation.location
                ) <= radiusMeters
            }
        }

        if (group != null) {
            group.add(observation)
        } else {
            groups.add(mutableListOf(observation))
        }
    }

    return groups.map { ObservationCluster(it) }
}
