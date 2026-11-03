package com.comp350sec001group2.ekhoexplorer.ui.map

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.comp350sec001group2.ekhoexplorer.ui.theme.AppStyle
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import java.util.UUID

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var map by remember {
        mutableStateOf<MapLibreMap?>(null)
    }

    // Stores observations added during this session
    val observations = remember {
        mutableStateListOf<ObservationMarker>()
    }

    // Tracks which observation the user selects
    var selectedObservation by remember {
        mutableStateOf<ObservationMarker?>(null)
    }

    // Links map markers to their observations
    val markerObservations = remember {
        mutableStateMapOf<Long, ObservationMarker>()
    }

    val mapView = remember {
        MapLibre.getInstance(context)

        MapView(context).apply {
            onCreate(null)

            getMapAsync { loadedMap ->
                map = loadedMap

                // Select the observation when its marker is tapped
                loadedMap.setOnMarkerClickListener { marker ->
                    selectedObservation = markerObservations[marker.id]

                    // True means we handled the marker click
                    selectedObservation != null
                }

                // Set the initial map location
                loadedMap.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(34.1628, -119.0436))
                    .zoom(14.0)
                    .build()

                // Enable zoom gestures
                loadedMap.uiSettings.isZoomGesturesEnabled = true

                // Load the map style
                loadedMap.setStyle(
                    Style.Builder().fromUri(
                        "https://tiles.openfreemap.org/styles/liberty"
                    )
                ) { style ->
                    Log.d(
                        "EkhoMap",
                        "Map style loaded. Number of layers: ${style.layers.size}"
                    )
                }
            }
        }
    }

    // Manage the MapView lifecycle
    DisposableEffect(mapView) {
        mapView.onStart()
        mapView.onResume()

        onDispose {
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Display the MapLibre map
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize()
        )

        // Map controls
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(AppStyle.dimens.lg),
            verticalArrangement = Arrangement.spacedBy(AppStyle.dimens.sm)
        ) {

            // Trail drawing controls
            DrawTrail(
                mapView = mapView,
                map = map,
                onTrailFinished = { points ->
                    Log.d(
                        "DrawTrail",
                        "Trail finished with ${points.size} points"
                    )
                }
            )

            // Add an observation
            AddMarker(
                onObservationSaved = {
                        commonName,
                        scientificName,
                        category,
                        description,
                        photoUri ->

                    // Get the phone's current GPS location
                    getObservationLocation(context) { location ->
                        if (location != null) {

                            // Create the observation
                            val observation = ObservationMarker(
                                id = UUID.randomUUID().toString(),
                                commonName = commonName,
                                scientificName = scientificName,
                                category = category,
                                description = description,
                                photoUri = photoUri.toString(),
                                location = location
                            )

                            // Save the observation in memory
                            observations.add(observation)

                            // Create the icon based on the observation category
                            val markerIcon = createObservationIcon(
                                context = context,
                                category = observation.category
                            )

                            // Add the observation marker to the map
                            val marker = map?.addMarker(
                                MarkerOptions()
                                    .position(observation.location)
                                    .title(observation.displayName)
                                    .snippet(observation.category.name)
                                    .icon(markerIcon)
                            )

                            // Link the marker to its observation
                            if (marker != null) {
                                markerObservations[marker.id] = observation
                            }

                            // Move the camera to the observation
                            map?.animateCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    observation.location,
                                    16.0
                                )
                            )

                            Log.d(
                                "EkhoMap",
                                "Saved ${observation.displayName} at " +
                                    "${location.latitude}, ${location.longitude}"
                            )

                        } else {
                            Log.e(
                                "EkhoMap",
                                "Could not save observation: location unavailable or permission missing"
                            )
                        }
                    }
                }
            )

            // Zoom in
            Button(
                onClick = {
                    map?.animateCamera(
                        CameraUpdateFactory.zoomIn()
                    )
                }
            ) {
                Text("+")
            }

            // Zoom out
            Button(
                onClick = {
                    map?.animateCamera(
                        CameraUpdateFactory.zoomOut()
                    )
                }
            ) {
                Text("-")
            }
        }
    }
    // Show details for the selected observation
    selectedObservation?.let { observation ->
        ObservationDetails(
            observation = observation,
            onDismiss = {
                selectedObservation = null
            }
        )
    }
}
