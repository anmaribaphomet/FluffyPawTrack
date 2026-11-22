package com.example.fluffypawtrack.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.fluffypawtrack.domain.model.GeoPoint

// iOS is not built; this stub only keeps the source set valid.
@Composable
actual fun PawMap(
    pins: List<MapPin>,
    onPinClick: (String) -> Unit,
    modifier: Modifier,
    center: GeoPoint,
    onMapClick: () -> Unit,
) {
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
}
