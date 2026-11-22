package com.example.fluffypawtrack.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.fluffypawtrack.ui.theme.FluffyColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val MemoryScheme = "memory://photo/"

/** Longest edge, in pixels, of a gallery photo decoded for the preview. */
private const val PreviewMaxEdge = 1280

/**
 * Camera thumbnails live only in memory. The fake repositories store the "memory://" key as
 * the photo reference; the Supabase ones compress and upload it ([compressPhoto]).
 */
internal object CapturedPhotos {
    private val photos = mutableMapOf<String, Bitmap>()
    private var nextId = 1

    @Synchronized
    fun put(bitmap: Bitmap): String = "$MemoryScheme${nextId++}".also { photos[it] = bitmap }

    @Synchronized
    fun get(ref: String): Bitmap? = photos[ref]
}

/**
 * The application context the launchers ran in, so [compressPhoto] can read the
 * `content://` refs they returned without a Composable.
 */
internal object PhotoContext {
    @Volatile
    var application: Context? = null
}

@Composable
actual fun rememberPhotoLaunchers(onPhoto: (String) -> Unit): PhotoLaunchers {
    PhotoContext.application = LocalContext.current.applicationContext
    val callback = rememberUpdatedState(onPhoto)
    // TakePicturePreview needs no CAMERA permission and no FileProvider; it returns a thumbnail.
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) callback.value(CapturedPhotos.put(bitmap))
    }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) callback.value(uri.toString())
    }
    return remember(camera, gallery) {
        PhotoLaunchers(
            takePhoto = { camera.launch(null) },
            pickFromGallery = {
                gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
    }
}

@Composable
actual fun PhotoPreview(photoRef: String, contentDescription: String?, modifier: Modifier) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null, photoRef) {
        value = withContext(Dispatchers.IO) { loadBitmap(context, photoRef)?.asImageBitmap() }
    }
    val loaded = image
    if (loaded == null) {
        Box(modifier.background(FluffyColors.Neutral300))
    } else {
        Image(loaded, contentDescription = contentDescription, modifier = modifier, contentScale = ContentScale.Crop)
    }
}

private fun loadBitmap(context: Context, ref: String): Bitmap? {
    if (ref.startsWith(MemoryScheme)) return CapturedPhotos.get(ref)
    return runCatching {
        val uri = Uri.parse(ref)
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= PreviewMaxEdge) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()
}
