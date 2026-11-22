package com.example.fluffypawtrack.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.fluffypawtrack.domain.model.GeoPoint

/** A colored marker drawn by [PawMap]. [id] is what [PawMap] reports back on tap. */
data class MapPin(
    val id: String,
    val location: GeoPoint,
    val color: Color,
    val label: String,
)

/** Downtown Hermosillo, the default camera target. */
val HermosilloCenter = GeoPoint(latitude = 29.0892, longitude = -110.9613)

/**
 * OpenStreetMap-based map with one colored pin per [MapPin].
 *
 * @param onPinClick receives the [MapPin.id] of the tapped pin.
 * @param onMapClick tap on the map outside any pin.
 */
@Composable
expect fun PawMap(
    pins: List<MapPin>,
    onPinClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    center: GeoPoint = HermosilloCenter,
    onMapClick: () -> Unit = {},
)
