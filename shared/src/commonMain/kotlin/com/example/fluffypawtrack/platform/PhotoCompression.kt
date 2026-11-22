package com.example.fluffypawtrack.platform

/** Longest side, in pixels, of a photo before it is uploaded (REQUIREMENTS §8). */
const val UPLOAD_MAX_EDGE = 1280

/** JPEG quality of uploaded photos. */
const val UPLOAD_JPEG_QUALITY = 80

/**
 * JPEG bytes of a photo returned by [rememberPhotoLaunchers] (`content://` or `memory://`),
 * scaled down to [UPLOAD_MAX_EDGE] on its longest side; null when it cannot be read.
 * Blocking: call it off the main thread.
 */
expect fun compressPhoto(ref: String): ByteArray?
