package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.AdoptionRequestStatus
import com.example.fluffypawtrack.domain.model.ContactRequestStatus
import com.example.fluffypawtrack.domain.model.HousingTenure
import com.example.fluffypawtrack.domain.model.HousingType
import com.example.fluffypawtrack.domain.model.ListingStatus
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.domain.model.OrganizationStatus
import com.example.fluffypawtrack.domain.model.OrganizationType
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** The table of supabase/README.md, "Enum mapping", checked in both directions. */
class EnumMappingTest {

    private inline fun <reified E : Enum<E>> assertMapping(expected: Map<E, String>) {
        assertEquals(enumValues<E>().toSet(), expected.keys, "every ${E::class.simpleName} value is mapped")
        expected.forEach { (value, db) ->
            assertEquals(db, value.dbValue)
            assertEquals(value, fromDb<E>(db))
        }
    }

    @Test
    fun userRole() = assertMapping(mapOf(Role.User to "user", Role.Affiliate to "affiliate", Role.Admin to "admin"))

    @Test
    fun organizationType() = assertMapping(
        mapOf(
            OrganizationType.Shelter to "shelter",
            OrganizationType.AdoptionCenter to "adoption_center",
            OrganizationType.Vet to "vet",
            OrganizationType.SafePoint to "safe_point",
        ),
    )

    @Test
    fun organizationStatus() = assertMapping(
        mapOf(
            OrganizationStatus.Pending to "pending",
            OrganizationStatus.Approved to "approved",
            OrganizationStatus.Rejected to "rejected",
        ),
    )

    @Test
    fun reportCategory() = assertMapping(
        mapOf(
            ReportCategory.Street to "street",
            ReportCategory.PossiblyLost to "possibly_lost",
            ReportCategory.Mistreated to "mistreated",
            ReportCategory.Injured to "injured",
            ReportCategory.Critical to "critical",
            ReportCategory.Abandoned to "abandoned",
        ),
    )

    @Test
    fun reportPriority() = assertMapping(
        mapOf(Priority.Low to "low", Priority.Medium to "medium", Priority.High to "high", Priority.Urgent to "urgent"),
    )

    @Test
    fun reportStatus() = assertMapping(
        mapOf(
            ReportStatus.Pending to "pending",
            ReportStatus.OnTheWay to "on_the_way",
            ReportStatus.Sheltered to "sheltered",
            ReportStatus.Closed to "closed",
        ),
    )

    @Test
    fun listingStatus() = assertMapping(mapOf(ListingStatus.Available to "available", ListingStatus.Adopted to "adopted"))

    @Test
    fun housing() {
        assertMapping(mapOf(HousingType.House to "house", HousingType.Apartment to "apartment"))
        assertMapping(mapOf(HousingTenure.Owned to "owned", HousingTenure.Rented to "rented"))
    }

    @Test
    fun adoptionRequestStatus() = assertMapping(
        mapOf(
            AdoptionRequestStatus.Pending to "pending",
            AdoptionRequestStatus.Approved to "approved",
            AdoptionRequestStatus.Rejected to "rejected",
            AdoptionRequestStatus.Cancelled to "cancelled",
        ),
    )

    @Test
    fun contactRequestStatus() = assertMapping(
        mapOf(
            ContactRequestStatus.Pending to "pending",
            ContactRequestStatus.Accepted to "accepted",
            ContactRequestStatus.Rejected to "rejected",
            ContactRequestStatus.Cancelled to "cancelled",
        ),
    )

    @Test
    fun notificationType() = assertMapping(
        mapOf(
            NotificationType.Report to "report",
            NotificationType.Adoption to "adoption",
            NotificationType.Favorite to "favorite",
            NotificationType.General to "general",
        ),
    )

    @Test
    fun unknownDatabaseValueFails() {
        assertFailsWith<IllegalArgumentException> { fromDb<ReportStatus>("lost") }
    }
}
