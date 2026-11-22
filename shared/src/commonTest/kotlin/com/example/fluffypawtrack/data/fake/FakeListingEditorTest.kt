package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.ListingRules
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.NewListing
import com.example.fluffypawtrack.domain.model.UserProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeListingEditorTest {

    private val patitas = SampleData.demoAffiliate
    private val huellitas = patitas.copy(id = "user-huellitas", organizationId = "org-huellitas")

    private var signedIn: UserProfile? = patitas
    private val listings = FakeAdoptionRepository(currentUser = { signedIn })

    private val luna = NewListing(
        name = "Luna",
        species = "Gato",
        breed = "Mestizo",
        age = "1 año",
        vaccination = "Completa",
        description = "Gata tranquila y juguetona.",
        photoUrls = listOf("content://media/picker/0/7"),
    )

    private fun Result<*>.message(): String? = exceptionOrNull()?.message

    @Test
    fun affiliatePublishesAnAvailableListingForTheirCenter() = runTest {
        val created = listings.createListing(luna).getOrThrow()

        assertEquals("org-patitas", created.organizationId)
        assertEquals(ListingStatus.Available, created.status)
        assertEquals(luna.photoUrls, created.photoUrls)
        assertTrue(listings.observeListingsBy("org-patitas").first().contains(created))
        assertTrue(listings.observeAvailableListings().first().contains(created))
    }

    @Test
    fun listingNeedsAtLeastOnePhoto() = runTest {
        assertEquals(ListingRules.MISSING_PHOTO, listings.createListing(luna.copy(photoUrls = emptyList())).message())
        assertEquals(ListingRules.MISSING_PHOTO, listings.createListing(luna.copy(photoUrls = listOf(" "))).message())
        assertEquals(ListingRules.MISSING_PHOTO, listings.updateListing("listing-1", luna.copy(photoUrls = emptyList())).message())
    }

    @Test
    fun listingNeedsEveryField() = runTest {
        assertEquals(ListingRules.MISSING_FIELDS, listings.createListing(luna.copy(vaccination = "")).message())
    }

    @Test
    fun onlyAdoptionCenterAffiliatesPublish() = runTest {
        signedIn = huellitas
        assertEquals("Solo los centros de adopción afiliados pueden publicar animales", listings.createListing(luna).message())
        signedIn = SampleData.demoUser
        assertEquals("Solo los centros de adopción afiliados pueden publicar animales", listings.createListing(luna).message())
    }

    @Test
    fun affiliateEditsTheirListingKeepingItsStatus() = runTest {
        val before = listings.getListing("listing-1")!!
        val edited = listings.updateListing("listing-1", luna.copy(name = "Luna Editada")).getOrThrow()

        assertEquals("Luna Editada", edited.name)
        assertEquals(before.status, edited.status)
        assertEquals(edited, listings.getListing("listing-1"))

        signedIn = huellitas
        assertEquals("Solo el centro de este animal puede editarlo", listings.updateListing("listing-1", luna).message())
        assertEquals("El animal no existe", listings.updateListing("missing", luna).message())
    }
}
