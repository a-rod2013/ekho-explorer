package com.comp350sec001group2.ekhoexplorer.ui.map

import android.content.Context
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.maps.MapLibreMap

// Displays individual and grouped observation markers
fun displayClusters(
    context: Context,
    map: MapLibreMap,
    observations: List<ObservationMarker>,
    markerObservations: MutableMap<Long, ObservationMarker>,
    markerClusters: MutableMap<Long, ObservationCluster>
) {
    // Remove only the observation markers
    val markerIds = markerObservations.keys + markerClusters.keys

    for (marker in map.markers.toList()) {
        if (marker.id in markerIds) {
            map.removeMarker(marker)
        }
    }

    markerObservations.clear()
    markerClusters.clear()

    // Group observations within the distance buffer
    val clusters = groupObservations(observations)

    for (cluster in clusters) {
        if (cluster.count == 1) {
            val observation = cluster.observations.first()

            val icon = createObservationIcon(
                context = context,
                category = observation.category
            )

            val marker = map.addMarker(
                MarkerOptions()
                    .position(observation.location)
                    .title(observation.displayName)
                    .icon(icon)
            )

            markerObservations[marker.id] = observation
        } else {
            val marker = map.addMarker(
                MarkerOptions()
                    .position(cluster.location)
                    .title("${cluster.count} Observations")
                    .icon(createClusterIcon(context, cluster.count))
            )

            markerClusters[marker.id] = cluster
        }
    }
}
