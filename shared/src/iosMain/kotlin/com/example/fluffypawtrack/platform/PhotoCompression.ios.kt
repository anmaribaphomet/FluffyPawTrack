package com.example.fluffypawtrack.platform

// iOS is not built; the photo picker stub never returns photos to compress.
actual fun compressPhoto(ref: String): ByteArray? = null
