
package com.comp350sec001group2.ekhoexplorer.ui.map

import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import android.graphics.Point

// Stores observations grouped at one location
data class ObservationCluster(
    val observations: List<ObservationMarker>
) {
    val location: LatLng
        get() = observations.first().location

    val count: Int
        get() = observations.size
}

// Groups observations that appear close together on the screen
fun groupObservations(
    observations: List<ObservationMarker>,
    map: MapLibreMap,
    radiusPixels: Float = 60f
): List<ObservationCluster> {

    val groups = mutableListOf<MutableList<ObservationMarker>>()

    for (observation in observations) {
        val point = map.projection.toScreenLocation(observation.location)

        val group = groups.firstOrNull { existing ->
            existing.any { other ->
                val otherPoint = map.projection.toScreenLocation(other.location)

                val dx = point.x - otherPoint.x
                val dy = point.y - otherPoint.y

                kotlin.math.sqrt(
                    (dx * dx + dy * dy).toFloat()
                ) <= radiusPixels
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
