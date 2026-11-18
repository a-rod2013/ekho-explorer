package com.comp350sec001group2.ekhoexplorer.ui.map

import android.Manifest
import com.comp350sec001group2.ekhoexplorer.ui.components.hasAnyLocationPermission
import com.comp350sec001group2.ekhoexplorer.ui.components.openAppSettings
import android.util.Log
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
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
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.collect
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.maplibre.android.annotations.Marker




@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    showSearch: Boolean = false,
    showFilters: Boolean = false
) {
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

    // Tracks the user's location marker
    var userLocationMarker by remember {
        mutableStateOf<Marker?>(null)
    }

    // Stores observations added during this session
    val observations = remember {
        mutableStateListOf<ObservationMarker>()
    }

    // Stores the map search text
    var searchQuery by remember {
        mutableStateOf("")
    }

// Stores the selected observation categories
    var selectedCategories by remember {
        mutableStateOf(ObservationCategory.values().toSet())
    }

// Observations that match the current search and category filters
    val filteredObservations = observations.filter { observation ->
        val matchesSearch =
            observation.commonName.contains(searchQuery, ignoreCase = true) ||
                observation.scientificName.contains(searchQuery, ignoreCase = true)

        val matchesCategory = observation.category in selectedCategories

        matchesSearch && matchesCategory
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

    val lifecycleOwner = LocalLifecycleOwner.current

    var hasLocationPermission by remember { mutableStateOf(context.hasAnyLocationPermission()) }
    var permissionRequestFinished by remember { mutableStateOf(hasLocationPermission) }
    var appLocationPermissionsDialogDismissed by remember { mutableStateOf(false) }
    var returningFromSettings by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        permissionRequestFinished = true
    }

    val requestLocationPermission: () -> Unit = {
        if (!hasLocationPermission) {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Launch location request when map starts
    LaunchedEffect(Unit) { requestLocationPermission() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val granted = context.hasAnyLocationPermission()
                hasLocationPermission = granted
                if (granted) {
                    appLocationPermissionsDialogDismissed = false
                    returningFromSettings = false
                } else if (returningFromSettings) {
                    returningFromSettings = false
                    permissionRequestFinished = false   // hide our dialog while the system prompt shows
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // For when location permissions are set to "Don't allow"
    // (Android sometimes won't allow app to prompt again (depends on version))
    if (permissionRequestFinished && !hasLocationPermission && !appLocationPermissionsDialogDismissed) {
        AlertDialog(
            onDismissRequest = { appLocationPermissionsDialogDismissed = true },
            title = { Text("Location permission needed") },
            text = {
                Text(
                    "Location access is turned off but is required for certain features"
                )
            },

            confirmButton = {
                Button(onClick = {
                    returningFromSettings = true
                    context.openAppSettings()
                }) {
                    Text("Open Settings")
                }
            },

            dismissButton = {
                TextButton(onClick = { appLocationPermissionsDialogDismissed = true }) {
                    Text("Not now")
                }
            }
        )
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

    // Track the user's live location
    DisposableEffect(map, hasLocationPermission, mapStyleReady) {
        val currentMap = map

        if (currentMap == null || !hasLocationPermission || !mapStyleReady) {
            onDispose { }
        } else {
            val locationClient =
                LocationServices.getFusedLocationProviderClient(context)

            // Request location updates every 5 seconds
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000L
            )
                .setMinUpdateIntervalMillis(3000L)
                .build()

            // Update the user's marker whenever a new location arrives
            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    val location = result.lastLocation ?: return

                    val position = LatLng(
                        location.latitude,
                        location.longitude
                    )

                    if (userLocationMarker == null) {
                        userLocationMarker = currentMap.addMarker(
                            MarkerOptions()
                                .position(position)
                                .title("Your location")
                                .icon(createUserLocationIcon(context))
                        )
                    } else {
                        userLocationMarker?.position = position
                    }
                }
            }

            try {
                locationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                ).addOnFailureListener { exception ->
                    Log.e("EkhoMap", "Location tracking failed", exception)
                }
            } catch (e: SecurityException) {
                Log.e("EkhoMap", "Location permission was lost", e)
            }

            // Stop tracking when leaving the map
            onDispose {
                locationClient.removeLocationUpdates(locationCallback)

                userLocationMarker?.let { marker ->
                    currentMap.removeMarker(marker)
                }

                userLocationMarker = null
            }
        }
    }
    // Restore saved Journal and Firestore observations
    LaunchedEffect(activeUser.userId, mapStyleReady) {

        if (!mapStyleReady) return@LaunchedEffect

        if (!activeUser.isSignedIn) {
            observations.clear()

            map?.let { currentMap ->
                displayClusters(
                    context = context,
                    map = currentMap,
                    observations = filteredObservations,
                    markerObservations = markerObservations,
                    markerClusters = markerClusters
                )
            }

            return@LaunchedEffect
        }

        // Load observations from Firestore
        val firestoreObservations = try {
            app.container.observationRepository
                .getObservations(activeUser.userId)
                .mapNotNull { saved ->

                    val category = runCatching {
                        ObservationCategory.valueOf(saved.category)
                    }.getOrNull() ?: return@mapNotNull null

                    ObservationMarker(
                        id = saved.id,
                        commonName = saved.commonName,
                        scientificName = saved.scientificName,
                        category = category,
                        description = saved.description,
                        photoUri = "",
                        location = LatLng(
                            saved.latitude,
                            saved.longitude
                        )
                    )
                }
        } catch (e: Exception) {
            Log.e("MapObservation", "Failed to load Firestore observations", e)
            emptyList()
        }

        // Continue observing local Journal entries
        journalRepository.observeDrafts(activeUser.userId)
            .collect { entries ->

                val localObservations = entries.mapNotNull {
                    it.toObservationMarker()
                }

                // Combine local and Firestore observations
                // Local entries take priority when IDs match
                val restoredObservations =
                    (localObservations + firestoreObservations)
                        .distinctBy { it.id }

                observations.clear()
                observations.addAll(restoredObservations)

                // Reset expanded marker state
                expandedMarkers.clear()
                expandedCluster = null
                selectedObservation = null



                Log.d(
                    "MapObservation",
                    "Restored ${localObservations.size} local and " +
                        "${firestoreObservations.size} Firestore observations"
                )
            }
    }

    // Refresh map markers when search or filters change
    LaunchedEffect(filteredObservations, mapStyleReady) {
        if (!mapStyleReady) return@LaunchedEffect

        map?.let { currentMap ->
            // Remove any expanded cluster markers
            collapseCluster(
                map = currentMap,
                expandedMarkers = expandedMarkers,
                markerObservations = markerObservations
            )

            expandedMarkers.clear()
            expandedCluster = null

            // Display only matching observations
            displayClusters(
                context = context,
                map = currentMap,
                observations = filteredObservations,
                markerObservations = markerObservations,
                markerClusters = markerClusters
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

        // Observation search panel
        if (showSearch) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search observations") },
                placeholder = { Text("Common or scientific name") },
                singleLine = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.85f)
                    .padding(top = AppStyle.dimens.lg)
            )
        }

        // Observation category filter panel
        if (showFilters) {
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.85f)
                    .padding(top = AppStyle.dimens.lg)
            ) {
                Column(
                    modifier = Modifier.padding(AppStyle.dimens.lg)
                ) {
                    Text("Filter by category")

                    ObservationCategory.values().forEach { category ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = category in selectedCategories,
                                onCheckedChange = { checked ->
                                    selectedCategories = if (checked) {
                                        selectedCategories + category
                                    } else {
                                        selectedCategories - category
                                    }
                                }
                            )

                            Text(
                                when (category) {
                                    ObservationCategory.PLANT -> "Plants"
                                    ObservationCategory.ANIMAL -> "Animals"
                                    ObservationCategory.INSECT -> "Insects"
                                    ObservationCategory.FUNGI -> "Fungi"
                                }
                            )
                        }
                    }
                }
            }
        }

        // Map controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(AppStyle.dimens.lg),
            verticalArrangement = Arrangement.spacedBy(AppStyle.dimens.sm)
        ) {





            // Add an observation
            AddMarker(
                onObservationSaved = {
                        commonName,
                        scientificName,
                        category,
                        description,
                        visibility,
                        photoUri ->

                    // Get the phone's current GPS location
                    getObservationLocation(context) { location ->
                        if (location == null) {
                            Log.e(
                                "EkhoMap",
                                "Could not save observation: location unavailable or permission missing"
                            )

                            return@getObservationLocation
                        }

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
                                    // Save the observation and photo to Firestore
                                    try {
                                        val photos = app.container.photoPreparer.prepare(
                                            listOf {
                                                context.contentResolver.openInputStream(photoUri)
                                                    ?: throw IllegalStateException("Could not open observation photo")
                                            }
                                        )

                                        app.container.observationRepository.addObservation(
                                            observationId = observation.id,
                                            userId = user.userId,
                                            commonName = commonName,
                                            scientificName = scientificName,
                                            category = category.name,
                                            description = description,
                                            visibility = visibility,
                                            latitude = location.latitude,
                                            longitude = location.longitude,
                                            photos = photos
                                        )

                                        Log.d("MapObservation", "Observation saved to Firestore: ${observation.id}")
                                    } catch (e: Exception) {
                                        Log.e("MapObservation", "Failed to save observation to Firestore", e)
                                    }
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
                    }
                }
            )


        }
        // My Location button
        IconButton(
            onClick = {
                getObservationLocation(context) { location ->
                    if (location != null) {
                        map?.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                location,
                                16.0
                            )
                        )
                    } else {
                        Log.e("EkhoMap", "Could not get current location")
                    }
                }
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(AppStyle.dimens.lg)
        ) {
            Icon(
                imageVector = Icons.Filled.MyLocation,
                contentDescription = "My Location"
            )
    }
        // Show details for the selected observation
        selectedObservation?.let { observation ->
            ObservationDetails(
                observation = observation,
                photoRepository = app.container.observationPhotoRepository,
                onDismiss = {
                    selectedObservation = null
                }
            )
        }
    }
}


// Creates the red icon for the user's location
private fun createUserLocationIcon(context: android.content.Context): Icon {
    val size = 64

    val bitmap = Bitmap.createBitmap(
        size,
        size,
        Bitmap.Config.ARGB_8888
    )

    val canvas = Canvas(bitmap)

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Light red outer circle
    paint.color = Color.argb(65, 244, 67, 54)
    canvas.drawCircle(32f, 32f, 30f, paint)

    // White border
    paint.color = Color.WHITE
    canvas.drawCircle(32f, 32f, 19f, paint)

    // Red center
    paint.color = Color.RED
    canvas.drawCircle(32f, 32f, 14f, paint)

    return IconFactory.getInstance(context).fromBitmap(bitmap)
}
