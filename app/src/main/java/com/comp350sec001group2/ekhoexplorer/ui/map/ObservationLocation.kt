package com.comp350sec001group2.ekhoexplorer.ui.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import org.maplibre.android.geometry.LatLng
import android.util.Log

// Gets the phone's location for an observation
fun getObservationLocation(
    context: Context,
    onLocationReceived: (LatLng?) -> Unit
) {
    // Check whether location permission was already granted
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    Log.d("ObservationLocation", "Fine permission: $fineGranted")
    Log.d("ObservationLocation", "Coarse permission: $coarseGranted")

    if (!fineGranted && !coarseGranted) {
        Log.e("ObservationLocation", "Location permission missing")
        onLocationReceived(null)
        return
    }

    val locationClient =
        LocationServices.getFusedLocationProviderClient(context)

    // Request the phone's current location
    val priority = if (fineGranted) {
        Priority.PRIORITY_HIGH_ACCURACY
    } else {
        Priority.PRIORITY_BALANCED_POWER_ACCURACY
    }

    val cancellationToken = CancellationTokenSource()

    locationClient.getCurrentLocation(
        priority,
        cancellationToken.token
    ).addOnSuccessListener { location ->
        if (location != null) {
            Log.d(
                "ObservationLocation",
                "GPS location: ${location.latitude}, ${location.longitude}"
            )

            onLocationReceived(
                LatLng(location.latitude, location.longitude)
            )
        } else {
            Log.e("ObservationLocation", "GPS returned null")
            onLocationReceived(null)
        }
    }.addOnFailureListener { exception ->
        Log.e("ObservationLocation", "GPS request failed", exception)
        onLocationReceived(null)
    }
}
