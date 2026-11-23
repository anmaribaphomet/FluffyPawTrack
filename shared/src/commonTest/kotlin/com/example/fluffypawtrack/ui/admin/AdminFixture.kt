package com.example.fluffypawtrack.ui.admin

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeNotificationRepository
import com.example.fluffypawtrack.data.fake.FakeOrganizationRepository
import com.example.fluffypawtrack.data.fake.FakeReportRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.UserProfile

/** The fakes wired like `AppContainer`, signed in as admin@correo.com unless [user] says otherwise. */
internal class AdminFixture(user: UserProfile? = SampleData.demoAdmin) {
    val auth = FakeAuthRepository(user)
    val notifications = FakeNotificationRepository()
    val organizations = FakeOrganizationRepository(
        currentUser = { auth.currentUser.value },
        updateUsers = { transform -> auth.updateUsers(transform) },
        notifications = notifications,
    )
    val reports = FakeReportRepository(
        currentUser = { auth.currentUser.value },
        findOrganizationName = { id -> organizations.findOrganization(id)?.name },
        notifications = notifications,
    )
}
