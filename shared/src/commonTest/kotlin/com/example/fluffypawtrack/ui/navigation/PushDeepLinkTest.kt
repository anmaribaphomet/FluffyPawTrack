package com.example.fluffypawtrack.ui.navigation

import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.Role
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PushDeepLinkTest {
    private val user = SampleData.demoUser.copy(role = Role.User, isAnonymous = false)

    @Test
    fun aUserOpensTheReportDetail() {
        assertEquals(Route.ReportDetail("report-1"), reportRouteFor(user, "report-1"))
    }

    @Test
    fun anAffiliateOpensTheCaseDetail() {
        val affiliate = user.copy(role = Role.Affiliate, organizationId = "org-1")

        assertEquals(Route.CaseDetail("report-1"), reportRouteFor(affiliate, "report-1"))
    }

    @Test
    fun anAdminOpensTheSupervisionDetail() {
        assertEquals(Route.AdminReportDetail("report-1"), reportRouteFor(user.copy(role = Role.Admin), "report-1"))
    }

    @Test
    fun waitsForAnAccountSession() {
        assertNull(reportRouteFor(null, "report-1"))
        assertNull(reportRouteFor(user.copy(isAnonymous = true), "report-1"))
    }

    @Test
    fun ignoresAMissingReportId() {
        assertNull(reportRouteFor(user, " "))
    }
}
