package com.example.fluffypawtrack.platform

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.fluffypawtrack.domain.model.GeoPoint

private val LocationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** A last known fix older than this is considered stale and a fresh one is requested. */
private const val MaxFixAgeMillis = 2 * 60 * 1000L

@Composable
actual fun rememberLocationRequester(onResult: (GeoPoint?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val callback = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) readLocation(context, callback.value) else callback.value(null)
    }
    return remember(context, launcher) {
        {
            if (hasLocationPermission(context)) readLocation(context, callback.value) else launcher.launch(LocationPermissions)
        }
    }
}

private fun hasLocationPermission(context: Context): Boolean = LocationPermissions.any {
    ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
}

/** Uses the platform LocationManager (no Google Play services): a recent last fix, else one fresh fix. */
@SuppressLint("MissingPermission") // Checked by hasLocationPermission or the permission result.
private fun readLocation(context: Context, onResult: (GeoPoint?) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (manager == null || !hasLocationPermission(context)) {
        onResult(null)
        return
    }
    val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
    if (providers.isEmpty()) {
        onResult(null)
        return
    }
    val recent = providers
        .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
        .filter { System.currentTimeMillis() - it.time <= MaxFixAgeMillis }
        .maxByOrNull { it.time }
    if (recent != null) {
        onResult(recent.toGeoPoint())
        return
    }
    // getCurrentLocation times out on its own and then delivers null.
    LocationManagerCompat.getCurrentLocation(
        manager,
        providers.first(),
        CancellationSignal(),
        ContextCompat.getMainExecutor(context),
    ) { location: Location? -> onResult(location?.toGeoPoint()) }
}

private fun Location.toGeoPoint() = GeoPoint(latitude, longitude)
