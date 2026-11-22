package com.example.fluffypawtrack.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

actual fun compressPhoto(ref: String): ByteArray? {
    val bitmap = if (ref.startsWith(MemoryScheme)) {
        CapturedPhotos.get(ref)
    } else {
        val context = PhotoContext.application ?: return null
        runCatching { decodeContent(context, Uri.parse(ref)) }.getOrNull()
    } ?: return null
    val scaled = scaleDown(bitmap)
    return ByteArrayOutputStream().use { out ->
        if (!scaled.compress(Bitmap.CompressFormat.JPEG, UPLOAD_JPEG_QUALITY, out)) return null
        out.toByteArray()
    }
}

private fun decodeContent(context: Context, uri: Uri): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // ImageDecoder applies the EXIF rotation and decodes straight to the target size.
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val (width, height) = fittedSize(info.size.width, info.size.height)
            decoder.setTargetSize(width, height)
            // Software bitmaps can be compressed; hardware ones cannot.
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    }
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= UPLOAD_MAX_EDGE) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

private fun scaleDown(bitmap: Bitmap): Bitmap {
    val (width, height) = fittedSize(bitmap.width, bitmap.height)
    return if (width == bitmap.width && height == bitmap.height) {
        bitmap
    } else {
        Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
}

/** [width] x [height] scaled so the longest side is at most [UPLOAD_MAX_EDGE], keeping the ratio. */
private fun fittedSize(width: Int, height: Int): Pair<Int, Int> {
    val longest = max(width, height)
    if (longest <= UPLOAD_MAX_EDGE) return width to height
    val scale = UPLOAD_MAX_EDGE.toFloat() / longest
    return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
}
