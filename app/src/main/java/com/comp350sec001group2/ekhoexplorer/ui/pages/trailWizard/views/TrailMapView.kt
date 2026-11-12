package com.comp350sec001group2.ekhoexplorer.ui.pages.trailWizard.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.comp350sec001group2.ekhoexplorer.Business.Trails.LatLngPoint
import com.comp350sec001group2.ekhoexplorer.Business.Trails.TrailLimits
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

private val DEFAULT_CENTER = LatLng(39.5, -98.35)

@Composable
fun TrailMapView(
    points: List<LatLngPoint>,
    selectedPin: Int?,
    onMapTap: (LatLngPoint) -> Unit,
    onPinTap: (Int) -> Unit,
    modifier: Modifier = Modifier,
    userLocation: LatLngPoint? = null,
) {
    val context = LocalContext.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    val tapped by rememberUpdatedState(onMapTap)
    val pinTapped by rememberUpdatedState(onPinTap)
    val markerIndex = remember { HashMap<Long, Int>() }
    val trailColor = AppStyle.colors.accent.toArgb()

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { loaded ->
                map = loaded
                loaded.cameraPosition = CameraPosition.Builder().target(DEFAULT_CENTER).zoom(3.0).build()
                loaded.uiSettings.isZoomGesturesEnabled = true
                loaded.addOnMapClickListener { latLng ->
                    tapped(LatLngPoint(latLng.latitude, latLng.longitude))
                    true
                }
                loaded.setOnMarkerClickListener { marker ->
                    val index = markerIndex[marker.id]
                    if (index != null) pinTapped(index)
                    index != null
                }
                loaded.setStyle(Style.Builder().fromUri(STYLE_URL)) { styleReady = true }
            }
        }
    }

    // Start on the user, once, if the trail has no pins yet.
    var centred by remember { mutableStateOf(false) }
    LaunchedEffect(map, userLocation, styleReady) {
        val m = map ?: return@LaunchedEffect
        if (centred || userLocation == null || !styleReady) return@LaunchedEffect
        centred = true
        m.cameraPosition = CameraPosition.Builder().target(LatLng(userLocation.lat, userLocation.lng)).zoom(14.0).build()
    }

    // Redraw the pins and the line whenever they change.
    LaunchedEffect(map, styleReady, points, selectedPin) {
        val m = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect
        m.clear()
        markerIndex.clear()
        if (points.size >= 2) {
            m.addPolyline(PolylineOptions().addAll(points.map { LatLng(it.lat, it.lng) }).color(trailColor).width(5f))
        }
        // A recorded walk can hold thousands of points: show just its start and end, not a marker each.
        val labelled = if (points.size > TrailLimits.MAX_PINS) listOf(0, points.lastIndex) else points.indices.toList()
        labelled.forEach { i ->
            val p = points[i]
            val marker = m.addMarker(
                MarkerOptions()
                    .position(LatLng(p.lat, p.lng))
                    .title(
                        when {
                            points.size > TrailLimits.MAX_PINS -> if (i == 0) "Start" else "End"
                            i == selectedPin -> "Pin ${i + 1}: tap the map to move it"
                            else -> "Pin ${i + 1}"
                        },
                    ),
            )
            markerIndex[marker.id] = i
            if (i == selectedPin) m.selectMarker(marker) else m.deselectMarker(marker)
        }
    }

    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier.fillMaxSize())
}
