package com.comp350sec001group2.ekhoexplorer.Business.Location

import android.annotation.SuppressLint
import android.content.Context
import com.comp350sec001group2.ekhoexplorer.Business.Trails.LatLngPoint
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await

/** Where the phone is right now, for sorting trails by distance. */
interface LocationProvider {
    /** The position, or null if it cannot be found (no permission, location off, no fix). */
    suspend fun lastKnown(): LatLngPoint?
}

class FusedLocationProvider(
    context: Context,
    private val access: LocationAccessReader = AndroidLocationAccessReader(context),
) : LocationProvider {

    private val client = LocationServices.getFusedLocationProviderClient(context.applicationContext)

    @SuppressLint("MissingPermission") // checked through LocationAccess just before
    override suspend fun lastKnown(): LatLngPoint? {
        val granted = access.read()
        if (!granted.canLocate) return null
        val location = client.lastLocation.await()
            ?: client.getCurrentLocation(
                if (granted.canRecordWalk) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                CancellationTokenSource().token,
            ).await()
        return location?.let { LatLngPoint(it.latitude, it.longitude) }
    }
}
