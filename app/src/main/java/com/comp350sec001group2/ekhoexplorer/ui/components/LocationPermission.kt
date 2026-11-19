package com.comp350sec001group2.ekhoexplorer.ui.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat


/** True when the user has allowed precise or approximate location. */
fun Context.hasAnyLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED

/** Opens this app's page in the phone's settings, where location can be allowed again. */
fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    startActivity(intent)
}

/** The two permissions asked for together: precise and approximate. */
val LocationPermissions: Array<String> = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** What a screen needs to ask again: show the system prompt, or open the app's settings page. */
class LocationPermissionRequester internal constructor(
    private val launch: () -> Unit,
    private val context: Context,
) {
    fun request() = launch()
    fun openSettings() = context.openAppSettings()
}

/** [onResult] is told whether location was allowed after the system prompt closes. */
@Composable
fun rememberLocationPermissionRequester(onResult: (granted: Boolean) -> Unit = {}): LocationPermissionRequester {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        onResult(
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        )
    }
    return remember(context, launcher) {
        LocationPermissionRequester(launch = { launcher.launch(LocationPermissions) }, context = context)
    }
}
