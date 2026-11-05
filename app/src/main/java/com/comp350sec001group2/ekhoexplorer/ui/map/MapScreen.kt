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
import androidx.compose.runtime.rememberCoroutineScope
import com.comp350sec001group2.ekhoexplorer.Business.Cache.EhkoCache
import com.comp350sec001group2.ekhoexplorer.Business.Journal.JournalDraft
import com.comp350sec001group2.ekhoexplorer.EkhoApp
import kotlinx.coroutines.launch
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.collect

@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val app = context.applicationContext as EkhoApp
    val journalRepository = app.container.journalRepository
    val journalPhotoStorage = app.container.journalPhotoStorage
    val coroutineScope = rememberCoroutineScope()

    val activeUser by EhkoCache.activeUserFlow.collectAsState()

    var mapStyleReady by remember {
        mutableStateOf(false)
    }

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

    // Links grouped markers to their observations
    val markerClusters = remember {
        mutableStateMapOf<Long, ObservationCluster>()
    }

    // Tracks the expanded cluster
    var expandedCluster by remember {
        mutableStateOf<ObservationCluster?>(null)
    }

    // Stores markers displayed when a cluster expands
    val expandedMarkers = remember {
        mutableStateListOf<org.maplibre.android.annotations.Marker>()
    }

    val mapView = remember {
        MapLibre.getInstance(context)

        MapView(context).apply {
            onCreate(null)

            getMapAsync { loadedMap ->
                map = loadedMap

                // Handle observation and cluster marker clicks
                loadedMap.setOnMarkerClickListener { marker ->
                    val cluster = markerClusters[marker.id]

                    if (cluster != null) {
                        // Collapse any previously expanded cluster
                        collapseCluster(
                            map = loadedMap,
                            expandedMarkers = expandedMarkers,
                            markerObservations = markerObservations
                        )
                        expandedMarkers.clear()

                        if (expandedCluster == cluster) {
                            // Collapse the selected cluster
                            expandedCluster = null
                        } else {
                            // Expand the selected cluster
                            expandedCluster = cluster

                            expandedMarkers.addAll(
                                expandCluster(
                                    context = context,
                                    map = loadedMap,
                                    cluster = cluster,
                                    markerObservations = markerObservations
                                )
                            )
                        }

                        true
                    } else {
                        // Open details for an individual observation
                        selectedObservation = markerObservations[marker.id]
                        selectedObservation != null
                    }
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
                    mapStyleReady = true
                }
            }
        }
    }
    // Restore saved Journal observations when the Map is ready
    LaunchedEffect(activeUser.userId, mapStyleReady) {

        if (!mapStyleReady) return@LaunchedEffect

        if (!activeUser.isSignedIn) {
            observations.clear()

            map?.let { currentMap ->
                displayClusters(
                    context = context,
                    map = currentMap,
                    observations = observations,
                    markerObservations = markerObservations,
                    markerClusters = markerClusters
                )
            }

            return@LaunchedEffect
        }
        journalRepository.observeDrafts(activeUser.userId)
            .collect { entries ->

                // Reconstruct saved Map observations
                val restoredObservations = entries.mapNotNull {
                    it.toObservationMarker()
                }

                // Replace temporary observations with Room data
                observations.clear()
                observations.addAll(restoredObservations)

                // Reset expanded marker state
                expandedMarkers.clear()
                expandedCluster = null
                selectedObservation = null

                // Refresh the markers on the Map
                map?.let { currentMap ->
                    displayClusters(
                        context = context,
                        map = currentMap,
                        observations = observations,
                        markerObservations = markerObservations,
                        markerClusters = markerClusters,
                    )
                }
                Log.d(
                    "MapJournal",
                    "Restored ${restoredObservations.size} observations",
                )
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
                            // Save the observation to the user's Journal
                            val user = EhkoCache.activeUser

                            if (user.isSignedIn) {
                                coroutineScope.launch {
                                    try {
                                        // Import the observation photo into Journal storage
                                        val savedPhotoPath = journalPhotoStorage.importPhoto(
                                            ownerId = user.userId,
                                            source = photoUri
                                        )

                                        // Create the Journal entry
                                        val timestamp = System.currentTimeMillis()

                                        val journalEntry = JournalDraft(
                                            id = observation.id,
                                            ownerId = user.userId,
                                            title = commonName,
                                            notes = """
                                                Scientific name: $scientificName
                                                Category: $category
                                                Description: $description
                                                Location: ${location.latitude}, ${location.longitude}
                                            """.trimIndent(),
                                            photoPaths = listOf(savedPhotoPath),
                                            createdAtMillis = timestamp,
                                            updatedAtMillis = timestamp
                                        )

                                        // Save the entry to Room
                                        journalRepository.saveDraft(journalEntry)

                                        Log.d(
                                            "MapJournal",
                                            "Journal entry saved: ${journalEntry.id}"
                                        )

                                    } catch (e: Exception) {
                                        Log.e(
                                            "MapJournal",
                                            "Failed to save observation to Journal",
                                            e
                                        )
                                    }
                                }
                            } else {
                                Log.w(
                                    "MapJournal",
                                    "Observation not saved to Journal: user is signed out"
                                )
                            }

                            // Refresh the map markers after saving
                            map?.let { currentMap ->

                                // Remove any expanded markers
                                collapseCluster(
                                    map = currentMap,
                                    expandedMarkers = expandedMarkers,
                                    markerObservations = markerObservations
                                )

                                expandedMarkers.clear()
                                expandedCluster = null

                                // Refresh observation clusters
                                displayClusters(
                                    context = context,
                                    map = currentMap,
                                    observations = observations,
                                    markerObservations = markerObservations,
                                    markerClusters = markerClusters
                                )
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
