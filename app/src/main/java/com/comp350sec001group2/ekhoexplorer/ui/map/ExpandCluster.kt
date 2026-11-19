package com.comp350sec001group2.ekhoexplorer.ui.map

import android.content.Context
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import android.graphics.PointF

// Displays observations around an expanded cluster

fun expandCluster(
    context: Context,
    map: MapLibreMap,
    cluster: ObservationCluster,
    markerObservations: MutableMap<Long, ObservationMarker>
): List<Marker> {

    val expandedMarkers = mutableListOf<Marker>()
    val count = cluster.count

    // Space expanded markers 90dp from the center
    val radius = (60 * context.resources.displayMetrics.density).toInt()
    val center = map.projection.toScreenLocation(cluster.location)

    for ((index, observation) in cluster.observations.withIndex()) {
        val angle = 2 * PI * index / count

        // Calculate the marker's display position in pixels
        val offsetX = (radius * cos(angle)).toInt()
        val offsetY = (radius * sin(angle)).toInt()

        val point = android.graphics.PointF(
            center.x + offsetX.toFloat(),
            center.y + offsetY.toFloat()
        )

        // Convert the display position back to a map location
        val position = map.projection.fromScreenLocation(point)

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

        // Keep the original observation and GPS location
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
