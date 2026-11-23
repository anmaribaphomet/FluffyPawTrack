package com.example.fluffypawtrack.ui.affiliate

import com.example.fluffypawtrack.data.fake.FakeAdoptionRepository
import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeNotificationPrefsRepository
import com.example.fluffypawtrack.data.fake.FakeNotificationRepository
import com.example.fluffypawtrack.data.fake.FakeOrganizationRepository
import com.example.fluffypawtrack.data.fake.FakeReportRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.UserProfile

/** The fakes wired like `AppContainer`, signed in as [user] (the Patitas affiliate by default). */
internal class AffiliateFakes(user: UserProfile? = SampleData.demoAffiliate) {
    val auth = FakeAuthRepository(initialUser = user)
    val notifications = FakeNotificationRepository()
    val prefs = FakeNotificationPrefsRepository()
    val organizations = FakeOrganizationRepository(
        currentUser = { auth.currentUser.value },
        updateUsers = { auth.updateUsers(it) },
        notifications = notifications,
    )
    val reports = FakeReportRepository(
        currentUser = { auth.currentUser.value },
        findOrganizationName = { id -> organizations.findOrganization(id)?.name },
        notifications = notifications,
    )
    val adoptions = FakeAdoptionRepository(
        findUser = { id -> auth.findUser(id) },
        currentUser = { auth.currentUser.value },
        findOrganization = { id -> organizations.findOrganization(id) },
    )
}
