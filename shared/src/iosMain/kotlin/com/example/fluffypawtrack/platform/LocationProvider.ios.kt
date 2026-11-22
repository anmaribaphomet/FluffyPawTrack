package com.example.fluffypawtrack.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.example.fluffypawtrack.domain.model.GeoPoint

// iOS is not built; the stub reports "no location" so the flow falls back to a typed address.
@Composable
actual fun rememberLocationRequester(onResult: (GeoPoint?) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onResult)
    return remember { { callback.value(null) } }
}

// iOS is not built; without a geocoder the flow shows the coordinates.
@Composable
actual fun rememberReverseGeocoder(): ReverseGeocoder = remember { ReverseGeocoder { null } }
