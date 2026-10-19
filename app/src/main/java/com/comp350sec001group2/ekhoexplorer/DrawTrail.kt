package com.comp350sec001group2.ekhoexplorer

import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import androidx.compose.ui.graphics.toArgb
import android.graphics.Color
import android.graphics.PointF
import android.view.MotionEvent
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.maplibre.android.annotations.Polyline
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

@Composable
fun DrawTrail(
    mapView: MapView,
    map: MapLibreMap?,
    onTrailFinished: (List<LatLng>) -> Unit
) {
    var isDrawingTrail by remember {
        mutableStateOf(false)
    }

    val trailPoints = remember {
        mutableStateListOf<LatLng>()
    }

    var currentPolyline by remember {
        mutableStateOf<Polyline?>(null)
    }

    DisposableEffect(mapView, map, isDrawingTrail) {
        val currentMap = map

        if (currentMap != null) {
            mapView.setOnTouchListener { _, event ->
                if (!isDrawingTrail) {
                    return@setOnTouchListener false
                }

                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        trailPoints.clear()

                        currentPolyline?.let {
                            currentMap.removePolyline(it)
                        }

                        val point =
                            currentMap.projection.fromScreenLocation(
                                PointF(event.x, event.y)
                            )

                        trailPoints.add(point)

                        currentPolyline = currentMap.addPolyline(
                            PolylineOptions()
                                .addAll(trailPoints)
                                .color(AppStyle.colors.accent.toArgb())
                                .width(5f)
                        )

                        true
                    }

                    MotionEvent.ACTION_MOVE -> {
                        val point =
                            currentMap.projection.fromScreenLocation(
                                PointF(event.x, event.y)
                            )

                        trailPoints.add(point)
                        currentPolyline?.points = trailPoints.toList()

                        true
                    }

                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        true
                    }

                    else -> true
                }
            }
        }

        onDispose {
            mapView.setOnTouchListener(null)
        }
    }

    Button(
        onClick = {
            if (isDrawingTrail) {
                onTrailFinished(trailPoints.toList())
                isDrawingTrail = false
            } else {
                trailPoints.clear()
                isDrawingTrail = true
            }
        }
    ) {
        Text(
            if (isDrawingTrail) "Finish Trail"
            else "Draw Trail"
        )
    }
}
