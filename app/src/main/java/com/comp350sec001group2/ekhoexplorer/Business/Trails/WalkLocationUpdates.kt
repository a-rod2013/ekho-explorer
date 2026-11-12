package com.comp350sec001group2.ekhoexplorer.Business.Trails

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class WalkLocationUpdates(context: Context) {

    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun start(onFix: (Fix) -> Unit) {
        stop()
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, UPDATE_INTERVAL_MILLIS)
            .setMinUpdateDistanceMeters(0f)
            .build()
        val newCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                for (location in result.locations) {
                    onFix(
                        Fix(
                            point = LatLngPoint(location.latitude, location.longitude),
                            // A reading with no accuracy is treated as too vague to use.
                            accuracyMeters = if (location.hasAccuracy()) location.accuracy else Float.MAX_VALUE,
                            timeMillis = location.time,
                        ),
                    )
                }
            }
        }
        callback = newCallback
        try {
            client.requestLocationUpdates(request, newCallback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            callback = null // permission was taken away: the session finds out through its access check
        }
    }

    fun stop() {
        callback?.let { client.removeLocationUpdates(it) }
        callback = null
    }

    private companion object {
        const val UPDATE_INTERVAL_MILLIS = 2_000L
    }
}
