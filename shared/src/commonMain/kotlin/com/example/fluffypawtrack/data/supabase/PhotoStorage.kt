package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.platform.compressPhoto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.http.ContentType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Buckets and object paths of supabase/migrations/20261122000400_storage.sql. The first path
 * segment is the folder the upload policies check: the uploader's uid for report and place
 * photos, the organization id for adoption photos.
 */
internal object StoragePaths {
    const val REPORT_PHOTOS = "report-photos"
    const val ADOPTION_PHOTOS = "adoption-photos"
    const val PLACE_PHOTOS = "place-photos"

    /** Profile photos, `avatar-photos/<uid>/<file>` (20261123000100_profile_editing.sql). */
    const val AVATAR_PHOTOS = "avatar-photos"

    fun objectPath(folder: String, fileName: String): String = "$folder/$fileName"

    @OptIn(ExperimentalUuidApi::class)
    fun newPhotoName(): String = "${Uuid.random()}.jpg"

    /**
     * True for photos that only exist on the device (camera or gallery). Bundled `res:` refs
     * (seed data) and URLs are stored as they are.
     */
    fun needsUpload(ref: String): Boolean = ref.startsWith("content://") || ref.startsWith("memory://")
}

/** Compresses device photos and uploads them to Storage, returning the ref to store in the row. */
internal class PhotoUploader(private val client: SupabaseClient) {

    /** The public URL of the uploaded photo, or [ref] itself when it needs no upload. */
    suspend fun store(ref: String, bucket: String, folder: String): String {
        if (!StoragePaths.needsUpload(ref)) return ref
        val bytes = withContext(Dispatchers.IO) { compressPhoto(ref) }
            ?: throw RuleViolation("No pudimos leer la foto. Elige otra e intenta de nuevo.")
        val path = StoragePaths.objectPath(folder, StoragePaths.newPhotoName())
        val photos = client.storage.from(bucket)
        photos.upload(path, bytes) {
            upsert = false
            contentType = ContentType.Image.JPEG
        }
        return photos.publicUrl(path)
    }
}
