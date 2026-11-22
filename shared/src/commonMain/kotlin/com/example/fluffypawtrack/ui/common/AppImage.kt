package com.example.fluffypawtrack.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.fluffypawtrack.platform.PhotoPreview
import com.example.fluffypawtrack.ui.theme.FluffyColors
import com.example.fluffypawtrack.ui.theme.FluffyShapes
import fluffypawtrack.shared.generated.resources.Res
import fluffypawtrack.shared.generated.resources.ic_camera
import fluffypawtrack.shared.generated.resources.sample_listing_canela_1
import fluffypawtrack.shared.generated.resources.sample_listing_canela_2
import fluffypawtrack.shared.generated.resources.sample_listing_luna
import fluffypawtrack.shared.generated.resources.sample_listing_michi_1
import fluffypawtrack.shared.generated.resources.sample_listing_michi_2
import fluffypawtrack.shared.generated.resources.sample_listing_toby
import fluffypawtrack.shared.generated.resources.sample_place_adoption_center
import fluffypawtrack.shared.generated.resources.sample_place_dog_refuge
import fluffypawtrack.shared.generated.resources.sample_place_municipal_shelter
import fluffypawtrack.shared.generated.resources.sample_place_plaza
import fluffypawtrack.shared.generated.resources.sample_place_shelter_kennels
import fluffypawtrack.shared.generated.resources.sample_place_vet_clinic
import fluffypawtrack.shared.generated.resources.sample_place_vet_exam
import fluffypawtrack.shared.generated.resources.sample_report_injured_dog
import fluffypawtrack.shared.generated.resources.sample_report_kittens_box
import fluffypawtrack.shared.generated.resources.sample_report_labrador
import fluffypawtrack.shared.generated.resources.sample_report_street_dog
import fluffypawtrack.shared.generated.resources.sample_report_weak_puppy
import fluffypawtrack.shared.generated.resources.sample_report_white_cat
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Where the photo behind an image reference lives. Reports, places and listings store one
 * of these references as their photo:
 * - `res:<drawable_name>`: a sample photo bundled with the app (fake data),
 * - `content://…` or `memory://photo/N`: a photo the user just took or picked ([PhotoPreview]),
 * - `http(s)://…`: a Supabase Storage URL (Phase 2).
 */
internal sealed interface ImageSource {
    data class Bundled(val name: String) : ImageSource
    data class Local(val ref: String) : ImageSource
    data class Remote(val url: String) : ImageSource
}

private const val BundledPrefix = "res:"

internal fun imageSourceOf(ref: String): ImageSource = when {
    ref.startsWith(BundledPrefix) -> ImageSource.Bundled(ref.removePrefix(BundledPrefix))
    // Blank (e.g. a listing that broke the "at least one photo" rule) falls back to the placeholder.
    ref.isBlank() || ref.startsWith("http://") || ref.startsWith("https://") -> ImageSource.Remote(ref)
    else -> ImageSource.Local(ref)
}

/** Sample photos referenced by `SampleData`; credits in `licenses/SAMPLE_IMAGES.md`. */
private val BundledImages: Map<String, DrawableResource> = mapOf(
    "sample_report_street_dog" to Res.drawable.sample_report_street_dog,
    "sample_report_white_cat" to Res.drawable.sample_report_white_cat,
    "sample_report_injured_dog" to Res.drawable.sample_report_injured_dog,
    "sample_report_weak_puppy" to Res.drawable.sample_report_weak_puppy,
    "sample_report_labrador" to Res.drawable.sample_report_labrador,
    "sample_report_kittens_box" to Res.drawable.sample_report_kittens_box,
    "sample_place_municipal_shelter" to Res.drawable.sample_place_municipal_shelter,
    "sample_place_vet_exam" to Res.drawable.sample_place_vet_exam,
    "sample_place_adoption_center" to Res.drawable.sample_place_adoption_center,
    "sample_place_dog_refuge" to Res.drawable.sample_place_dog_refuge,
    "sample_place_plaza" to Res.drawable.sample_place_plaza,
    "sample_place_shelter_kennels" to Res.drawable.sample_place_shelter_kennels,
    "sample_place_vet_clinic" to Res.drawable.sample_place_vet_clinic,
    "sample_listing_canela_1" to Res.drawable.sample_listing_canela_1,
    "sample_listing_canela_2" to Res.drawable.sample_listing_canela_2,
    "sample_listing_michi_1" to Res.drawable.sample_listing_michi_1,
    "sample_listing_michi_2" to Res.drawable.sample_listing_michi_2,
    "sample_listing_toby" to Res.drawable.sample_listing_toby,
    "sample_listing_luna" to Res.drawable.sample_listing_luna,
)

/** The bundled drawable for a `res:` name, or null when the name is unknown. */
internal fun bundledDrawable(name: String): DrawableResource? = BundledImages[name]

/**
 * The one composable every screen uses to show a report, place or listing photo.
 * Unknown or not yet loadable references show a neutral placeholder.
 */
@Composable
fun AppImage(
    ref: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    when (val source = imageSourceOf(ref)) {
        is ImageSource.Bundled -> {
            val drawable = bundledDrawable(source.name)
            if (drawable == null) {
                ImagePlaceholder(modifier)
            } else {
                Image(painterResource(drawable), contentDescription, modifier, contentScale = contentScale)
            }
        }
        // PhotoPreview always crops to fill.
        is ImageSource.Local -> PhotoPreview(source.ref, contentDescription, modifier)
        // TODO(Phase 2): load Supabase Storage URLs with a network image loader.
        is ImageSource.Remote -> ImagePlaceholder(modifier)
    }
}

/** Small square photo for list rows and cards. */
@Composable
internal fun PhotoThumbnail(ref: String, contentDescription: String?, modifier: Modifier = Modifier) {
    AppImage(ref, contentDescription, modifier.size(52.dp).clip(FluffyShapes.Card))
}

@Composable
private fun ImagePlaceholder(modifier: Modifier) {
    Box(modifier.background(FluffyColors.Cream100), contentAlignment = Alignment.Center) {
        Image(painterResource(Res.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(32.dp))
    }
}
