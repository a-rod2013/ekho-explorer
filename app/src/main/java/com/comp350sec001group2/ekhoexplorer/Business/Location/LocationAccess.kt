package com.comp350sec001group2.ekhoexplorer.Business.Location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat

/**
 * What the app may do with the phone's location right now. Both the trail viewer (sorting by
 * distance) and walking mode (recording a path) read it.
 */
enum class LocationAccess(
    /** The phone's position can be found, so the viewer can sort by distance. */
    val canLocate: Boolean,
    /** A walk can be recorded (needs precise location). */
    val canRecordWalk: Boolean,
    /** A walk keeps recording when the user leaves the app ("allow all the time"). */
    val recordsInBackground: Boolean,
) {
    PRECISE_BACKGROUND(canLocate = true, canRecordWalk = true, recordsInBackground = true),
    PRECISE_FOREGROUND(canLocate = true, canRecordWalk = true, recordsInBackground = false),
    APPROXIMATE(canLocate = true, canRecordWalk = false, recordsInBackground = false),
    DENIED(canLocate = false, canRecordWalk = false, recordsInBackground = false);

    companion object {
        /** [servicesOn] is the phone-wide location switch; when it is off nothing works. */
        fun from(fine: Boolean, coarse: Boolean, background: Boolean, servicesOn: Boolean): LocationAccess = when {
            !servicesOn -> DENIED
            fine && background -> PRECISE_BACKGROUND
            fine -> PRECISE_FOREGROUND
            coarse -> APPROXIMATE
            else -> DENIED
        }
    }
}

fun interface LocationAccessReader {
    fun read(): LocationAccess
}

class AndroidLocationAccessReader(context: Context) : LocationAccessReader {
    private val appContext = context.applicationContext

    override fun read(): LocationAccess {
        fun granted(permission: String) =
            ContextCompat.checkSelfPermission(appContext, permission) == PackageManager.PERMISSION_GRANTED

        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = granted(Manifest.permission.ACCESS_COARSE_LOCATION)
        // Before Android 10 there is no separate "all the time" permission.
        val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            fine
        }
        val manager = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationAccess.from(fine, coarse, background, servicesOn = LocationManagerCompat.isLocationEnabled(manager))
    }
}
