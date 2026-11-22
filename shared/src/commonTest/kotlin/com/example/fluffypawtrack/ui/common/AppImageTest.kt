package com.example.fluffypawtrack.ui.common

import com.example.fluffypawtrack.data.fake.SampleData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppImageTest {

    private val sampleRefs: List<String> =
        SampleData.reports.map { it.photoUrl } +
            SampleData.organizations.map { it.photoUrl } +
            SampleData.adoptionListings.flatMap { it.photoUrls }

    @Test
    fun everySampleImageIsABundledResource() {
        assertTrue(sampleRefs.isNotEmpty())
        sampleRefs.forEach { ref ->
            val source = imageSourceOf(ref)
            assertTrue(source is ImageSource.Bundled, "$ref should point to a bundled drawable")
            assertNotNull(bundledDrawable(source.name), "$ref has no drawable in the AppImage mapping")
        }
    }

    @Test
    fun localPhotosAndUrlsAreClassified() {
        assertEquals(ImageSource.Local("content://media/picker/0/1"), imageSourceOf("content://media/picker/0/1"))
        assertEquals(ImageSource.Local("memory://photo/3"), imageSourceOf("memory://photo/3"))
        assertEquals(ImageSource.Remote("https://example.com/a.jpg"), imageSourceOf("https://example.com/a.jpg"))
        assertEquals(ImageSource.Bundled("sample_missing"), imageSourceOf("res:sample_missing"))
        assertEquals(null, bundledDrawable("sample_missing"))
    }
}
