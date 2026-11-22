package com.example.fluffypawtrack.platform

import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.fluffypawtrack.domain.model.GeoPoint
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression.get
import org.maplibre.android.style.expressions.Expression.toColor
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.PropertyFactory.circleColor
import org.maplibre.android.style.layers.PropertyFactory.circleRadius
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeColor
import org.maplibre.android.style.layers.PropertyFactory.circleStrokeWidth
import org.maplibre.android.style.layers.PropertyFactory.textAnchor
import org.maplibre.android.style.layers.PropertyFactory.textColor
import org.maplibre.android.style.layers.PropertyFactory.textField
import org.maplibre.android.style.layers.PropertyFactory.textFont
import org.maplibre.android.style.layers.PropertyFactory.textHaloColor
import org.maplibre.android.style.layers.PropertyFactory.textHaloWidth
import org.maplibre.android.style.layers.PropertyFactory.textOffset
import org.maplibre.android.style.layers.PropertyFactory.textOptional
import org.maplibre.android.style.layers.PropertyFactory.textSize
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

// OpenFreeMap: OpenStreetMap vector tiles, no API key.
private const val StyleUrl = "https://tiles.openfreemap.org/styles/liberty"
private const val SourceId = "paw-pins"
private const val CircleLayerId = "paw-pins-circle"
private const val LabelLayerId = "paw-pins-label"
private const val PropId = "id"
private const val PropColor = "color"
private const val PropLabel = "label"
private const val InitialZoom = 12.5
private const val TapRadiusDp = 14f

@Composable
actual fun PawMap(
    pins: List<MapPin>,
    onPinClick: (String) -> Unit,
    modifier: Modifier,
    center: GeoPoint,
    onMapClick: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val tapRadiusPx = with(LocalDensity.current) { TapRadiusDp * density }
    val currentOnPinClick by rememberUpdatedState(onPinClick)
    val currentOnMapClick by rememberUpdatedState(onMapClick)
    var loadedSource by remember { mutableStateOf<GeoJsonSource?>(null) }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context).apply {
            onCreate(null)
            getMapAsync { map ->
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(center.latitude, center.longitude), InitialZoom))
                map.uiSettings.isRotateGesturesEnabled = false
                map.setStyle(Style.Builder().fromUri(StyleUrl)) { style ->
                    val source = GeoJsonSource(SourceId, FeatureCollection.fromFeatures(emptyList()))
                    style.addSource(source)
                    style.addLayer(
                        CircleLayer(CircleLayerId, SourceId).withProperties(
                            circleColor(toColor(get(PropColor))),
                            circleRadius(9f),
                            circleStrokeColor(android.graphics.Color.WHITE),
                            circleStrokeWidth(2.5f),
                        ),
                    )
                    style.addLayer(
                        SymbolLayer(LabelLayerId, SourceId).withProperties(
                            textField(get(PropLabel)),
                            textFont(arrayOf("Noto Sans Regular")),
                            textSize(11f),
                            textColor(toColor(get(PropColor))),
                            textHaloColor(android.graphics.Color.WHITE),
                            textHaloWidth(1.5f),
                            textAnchor("top"),
                            textOffset(arrayOf(0f, 1.1f)),
                            // Labels may be hidden when they collide; the pin always stays.
                            textOptional(true),
                        ),
                    )
                    loadedSource = source
                }
                map.addOnMapClickListener { latLng ->
                    val point = map.projection.toScreenLocation(latLng)
                    val area = RectF(point.x - tapRadiusPx, point.y - tapRadiusPx, point.x + tapRadiusPx, point.y + tapRadiusPx)
                    val id = map.queryRenderedFeatures(area, CircleLayerId)
                        .firstNotNullOfOrNull { it.getStringProperty(PropId) }
                    if (id != null) currentOnPinClick(id) else currentOnMapClick()
                    true
                }
            }
        }
    }

    LaunchedEffect(loadedSource, pins) {
        loadedSource?.setGeoJson(pins.toFeatureCollection())
    }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            // Bring the view down in order before destroying it when the screen leaves composition.
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun List<MapPin>.toFeatureCollection(): FeatureCollection = FeatureCollection.fromFeatures(
    map { pin ->
        Feature.fromGeometry(Point.fromLngLat(pin.location.longitude, pin.location.latitude)).apply {
            addStringProperty(PropId, pin.id)
            addStringProperty(PropColor, pin.color.toHex())
            addStringProperty(PropLabel, pin.label)
        }
    },
)

private fun androidx.compose.ui.graphics.Color.toHex(): String =
    "#" + (toArgb() and 0xFFFFFF).toString(16).padStart(6, '0')
