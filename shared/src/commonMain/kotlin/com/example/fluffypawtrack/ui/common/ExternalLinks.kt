package com.example.fluffypawtrack.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.domain.model.GeoPoint
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_map
import org.jetbrains.compose.resources.painterResource

// External links (CLAUDE.md "External links"): directions open Google Maps, phones the dialer,
// emails the mail app, and social handles the app or the browser.

/** Google Maps route to [point]: `https://www.google.com/maps/dir/?api=1&destination=LAT,LNG`. */
fun directionsUrl(point: GeoPoint): String =
    "https://www.google.com/maps/dir/?api=1&destination=${point.latitude.plainDecimal()},${point.longitude.plainDecimal()}"

fun phoneUri(phone: String): String = "tel:" + phone.filterNot { it.isWhitespace() }

fun emailUri(email: String): String = "mailto:" + email.trim()

/**
 * A full URL passes through unchanged; a bare handle (optionally with "@") is treated as an
 * Instagram profile. ASSUMPTION: affiliates register Instagram handles.
 */
fun socialUrl(handle: String): String {
    val value = handle.trim()
    if (value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true)) return value
    return "https://www.instagram.com/${value.removePrefix("@")}/"
}

/** Opens [uri] with the app Android resolves; does nothing when no app can handle it. */
fun UriHandler.openSafely(uri: String) {
    runCatching { openUri(uri) }
}

/** "Cómo llegar" pill: opens the Google Maps route to [point]. */
@Composable
fun DirectionsButton(point: GeoPoint, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Row(
        modifier = modifier
            .clip(FluffyShapes.Pill)
            .background(FluffyColors.Sage200, FluffyShapes.Pill)
            .clickable(role = Role.Button, onClickLabel = "Abrir la ruta en Google Maps") {
                uriHandler.openSafely(directionsUrl(point))
            }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Image(painterResource(Res.drawable.ic_map), contentDescription = null, modifier = Modifier.size(16.dp))
        Text("Cómo llegar", style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
    }
}

/** Underlined value that opens [uri] (tel:, mailto: or a web URL) when tapped. */
@Composable
fun LinkText(text: String, uri: String, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    Text(
        text = text,
        modifier = modifier.clickable(role = Role.Button) { uriHandler.openSafely(uri) },
        style = MaterialTheme.typography.bodyMedium,
        color = FluffyColors.Brown900,
        textDecoration = TextDecoration.Underline,
    )
}

/** Label over a tappable value, matching the stacked label/value rows of the detail screens. */
@Composable
fun LabeledLink(label: String, value: String?, uri: (String) -> String, emptyText: String = "Sin datos") {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = FluffyColors.Brown900)
        if (value.isNullOrBlank()) {
            Text(emptyText, style = MaterialTheme.typography.bodySmall, color = FluffyColors.Brown900)
        } else {
            LinkText(value, uri(value))
        }
    }
}

/** Locale-independent decimal text with a dot and no exponent, keeping every significant digit. */
private fun Double.plainDecimal(): String {
    val text = toString()
    val exponentAt = text.indexOfFirst { it == 'E' || it == 'e' }
    if (exponentAt < 0) return text.removeSuffix(".0")
    val negative = text.startsWith('-')
    val mantissa = text.substring(if (negative) 1 else 0, exponentAt)
    val exponent = text.substring(exponentAt + 1).toInt()
    val pointAt = mantissa.indexOf('.').let { if (it < 0) mantissa.length else it }
    val digits = mantissa.replace(".", "")
    val newPoint = pointAt + exponent
    val plain = when {
        newPoint <= 0 -> "0." + "0".repeat(-newPoint) + digits
        newPoint >= digits.length -> digits + "0".repeat(newPoint - digits.length)
        else -> digits.substring(0, newPoint) + "." + digits.substring(newPoint)
    }
    val trimmed = if ('.' in plain) plain.trimEnd('0').removeSuffix(".") else plain
    return if (negative) "-$trimmed" else trimmed
}
