
package com.comp350sec001group2.ekhoexplorer.ui.map

import android.content.Context
import android.graphics.PointF
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import kotlin.math.sqrt
import android.graphics.Color
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions


// Moves displayed markers away from the user's location
private fun getDisplayLocation(
    location: LatLng,
    userLocation: LatLng?,
    map: MapLibreMap,
    context: Context
): LatLng {
    if (userLocation == null) return location

    val markerPoint = map.projection.toScreenLocation(location)
    val userPoint = map.projection.toScreenLocation(userLocation)

    val dx = markerPoint.x - userPoint.x
    val dy = markerPoint.y - userPoint.y
    val distance = sqrt(dx * dx + dy * dy)

    // Keep markers at least 40dp away from the user marker
    val minimumDistance = 40f * context.resources.displayMetrics.density

    if (distance >= minimumDistance) return location

    // Handle markers at exactly the same screen position
    val directionX = if (distance == 0f) 1f else dx / distance
    val directionY = if (distance == 0f) 0f else dy / distance

    val newPoint = PointF(
        userPoint.x + directionX * minimumDistance,
        userPoint.y + directionY * minimumDistance
    )

    return map.projection.fromScreenLocation(newPoint)
}

// Displays individual and grouped observation markers
fun displayClusters(
    context: Context,
    map: MapLibreMap,
    observations: List<ObservationMarker>,
    markerObservations: MutableMap<Long, ObservationMarker>,
    markerClusters: MutableMap<Long, ObservationCluster>,
    connectorLines: MutableList<Polyline>,
    userLocation: LatLng? = null
) {
    // Remove old connector lines
    for (line in connectorLines.toList()) {
        map.removePolyline(line)
    }
    connectorLines.clear()
    // Remove only the observation markers
    val markerIds = markerObservations.keys + markerClusters.keys

    for (marker in map.markers.toList()) {
        if (marker.id in markerIds) {
            map.removeMarker(marker)
        }
    }

    markerObservations.clear()
    markerClusters.clear()

    // Group observations based on screen distance
    val clusters = groupObservations(
        observations = observations,
        map = map
    )

    for (cluster in clusters) {
        // Calculate a display position without changing saved GPS coordinates
        val displayLocation = getDisplayLocation(
            location = cluster.location,
            userLocation = userLocation,
            map = map,
            context = context
        )

        // Draw a line when the marker is moved
        if (displayLocation != cluster.location) {
            val line = map.addPolyline(
                PolylineOptions()
                    .add(cluster.location)
                    .add(displayLocation)
                    .width(2f)
                    .color(Color.GRAY)
            )

            connectorLines.add(line)
        }

        if (cluster.count == 1) {
            val observation = cluster.observations.first()

            val icon = createObservationIcon(
                context = context,
                category = observation.category
            )

            val marker = map.addMarker(
                MarkerOptions()
                    .position(displayLocation)
                    .title(observation.displayName)
                    .icon(icon)
            )

            markerObservations[marker.id] = observation
        } else {
            val marker = map.addMarker(
                MarkerOptions()
                    .position(displayLocation)
                    .title("${cluster.count} Observations")
                    .icon(createClusterIcon(context, cluster.count))
            )

            markerClusters[marker.id] = cluster
        }
    }
}
