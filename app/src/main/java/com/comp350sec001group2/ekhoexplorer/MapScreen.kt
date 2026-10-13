
package com.comp350sec001group2.ekhoexplorer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.annotations.MarkerOptions

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var map by remember { mutableStateOf<MapLibreMap?>(null) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { loadedMap ->
                map = loadedMap
                loadedMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(34.1628, -119.0436))
                    .zoom(14.0)
                    .build()

                loadedMap.uiSettings.isZoomGesturesEnabled = true

                loadedMap.setStyle(
                    Style.Builder()
                        .fromUri("https://tiles.openfreemap.org/styles/liberty")
                ) { style ->
                    android.util.Log.d(
                        "EkhoMap",
                        "Map style loaded. Number of layers: ${style.layers.size}"
                    )

                    // Sample nature observation 1
                    loadedMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(34.1628, -119.0436))
                            .title("California Poppy")
                            .snippet("Sample observation: Native flower")
                    )

                    // Sample nature observation 2
                    loadedMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(34.1650, -119.0410))
                            .title("Western Bluebird")
                            .snippet("Sample observation: Bird sighting")
                    )

                    // Sample nature observation 3
                    loadedMap.addMarker(
                        MarkerOptions()
                            .position(LatLng(34.1605, -119.0460))
                            .title("Monarch Butterfly")
                            .snippet("Sample observation: Insect sighting")
                    )
                }
            }
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

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(onClick = {
                map?.let { currentMap ->
                    currentMap.animateCamera(
                        org.maplibre.android.camera.CameraUpdateFactory.zoomIn()
                    )
                }
            }) {
                Text("+")
            }

            Button(onClick = {
                map?.let { currentMap ->
                    currentMap.animateCamera(
                        org.maplibre.android.camera.CameraUpdateFactory.zoomOut()
                    )
                }
            }) {
                Text("−")
            }
        }
    }
}
