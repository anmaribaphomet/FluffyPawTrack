package com.example.fluffypawtrack.platform

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.fluffypawtrack.ui.theme.FluffyColors

// iOS is not built; these stubs only keep the source set valid.
@Composable
actual fun rememberPhotoLaunchers(onPhoto: (String) -> Unit): PhotoLaunchers =
    remember { PhotoLaunchers(takePhoto = {}, pickFromGallery = {}) }

@Composable
actual fun PhotoPreview(photoRef: String, contentDescription: String?, modifier: Modifier) {
    Box(modifier.background(FluffyColors.Neutral300))
}
