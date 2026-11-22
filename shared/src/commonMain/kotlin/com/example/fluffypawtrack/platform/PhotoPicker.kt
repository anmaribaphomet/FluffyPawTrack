package com.example.fluffypawtrack.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Launchers for the two photo sources of a report. */
class PhotoLaunchers(
    val takePhoto: () -> Unit,
    val pickFromGallery: () -> Unit,
)

/**
 * Camera and gallery launchers. [onPhoto] receives a local reference to the chosen photo
 * (a content URI or an in-memory id) that [PhotoPreview] can display.
 */
@Composable
expect fun rememberPhotoLaunchers(onPhoto: (String) -> Unit): PhotoLaunchers

/** Shows the photo behind a reference returned by [rememberPhotoLaunchers], cropped to fill. */
@Composable
expect fun PhotoPreview(photoRef: String, contentDescription: String?, modifier: Modifier = Modifier)
