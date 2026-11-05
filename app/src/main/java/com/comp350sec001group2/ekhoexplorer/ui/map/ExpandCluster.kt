package com.comp350sec001group2.ekhoexplorer.ui.map

import android.content.Context
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Displays observations around an expanded cluster
fun expandCluster(
    context: Context,
    map: MapLibreMap,
    cluster: ObservationCluster,
    markerObservations: MutableMap<Long, ObservationMarker>
): List<Marker> {

    val expandedMarkers = mutableListOf<Marker>()
    val count = cluster.count
    // Distance between the cluster and expanded markers
    val radius = 0.0004

    for ((index, observation) in cluster.observations.withIndex()) {
        val angle = 2 * PI * index / count

        // Position markers around the cluster
        val position = LatLng(
            cluster.location.latitude + radius * sin(angle),
            cluster.location.longitude + radius * cos(angle)
        )

        val icon = createObservationIcon(
            context = context,
            category = observation.category
        )

        val marker = map.addMarker(
            MarkerOptions()
                .position(position)
                .title(observation.displayName)
                .icon(icon)
        )

        markerObservations[marker.id] = observation
        expandedMarkers.add(marker)
    }

    return expandedMarkers
}

// Removes expanded markers from the map
fun collapseCluster(
    map: MapLibreMap,
    expandedMarkers: List<Marker>,
    markerObservations: MutableMap<Long, ObservationMarker>
) {
    for (marker in expandedMarkers) {
        markerObservations.remove(marker.id)
        map.removeMarker(marker)
    }
}
