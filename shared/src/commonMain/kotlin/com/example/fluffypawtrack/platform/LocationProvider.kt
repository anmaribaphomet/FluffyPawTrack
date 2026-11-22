package com.example.fluffypawtrack.platform

import androidx.compose.runtime.Composable
import com.example.fluffypawtrack.domain.model.GeoPoint

/**
 * Returns a function that asks for the location permission when needed and then reads the
 * current position. [onResult] receives null when the permission is denied or no fix is available.
 */
@Composable
expect fun rememberLocationRequester(onResult: (GeoPoint?) -> Unit): () -> Unit
