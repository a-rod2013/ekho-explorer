package com.comp350sec001group2.ekhoexplorer.ui.pages.trailDetail.views

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.comp350sec001group2.ekhoexplorer.Business.Trails.LatLngPoint
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.PolylineOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style

@Composable
fun TrailPathView(points: List<LatLngPoint>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }
    val trailColor = AppStyle.colors.accent.toArgb()

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { loaded ->
                map = loaded
                loaded.uiSettings.isZoomGesturesEnabled = true
                loaded.setStyle(Style.Builder().fromUri("https://tiles.openfreemap.org/styles/liberty")) { styleReady = true }
            }
        }
    }

    LaunchedEffect(map, styleReady, points) {
        val m = map ?: return@LaunchedEffect
        if (!styleReady || points.size < 2) return@LaunchedEffect
        m.clear()
        val line = points.map { LatLng(it.lat, it.lng) }
        m.addPolyline(PolylineOptions().addAll(line).color(trailColor).width(5f))
        m.addMarker(MarkerOptions().position(line.first()).title("Start"))
        m.addMarker(MarkerOptions().position(line.last()).title("End"))
        val bounds = LatLngBounds.Builder().includes(line).build()
        m.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 64))
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
