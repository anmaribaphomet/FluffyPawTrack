package com.example.fluffypawtrack.ui.navigation

import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.model.UserProfile

/**
 * Where a tapped push notification about [reportId] leads (RF-03): each role opens the report
 * detail it already has. Null while there is no account session (signed out or anonymous):
 * the link waits until the user signs in.
 */
fun reportRouteFor(user: UserProfile?, reportId: String): Route? {
    if (user == null || user.isAnonymous || reportId.isBlank()) return null
    return when (user.role) {
        Role.User -> Route.ReportDetail(reportId)
        Role.Affiliate -> Route.CaseDetail(reportId)
        Role.Admin -> Route.AdminReportDetail(reportId)
    }
}
