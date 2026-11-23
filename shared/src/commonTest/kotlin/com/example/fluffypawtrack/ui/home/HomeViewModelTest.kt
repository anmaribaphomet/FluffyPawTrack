package com.example.fluffypawtrack.ui.home

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeNotificationRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.ui.components.badgeText
import com.example.fluffypawtrack.ui.components.notificationsTabDescription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun countsTheSignedInUsersUnreadNotifications() = runTest {
        val vm = HomeViewModel(FakeNotificationRepository(), FakeAuthRepository(initialUser = SampleData.demoUser))

        assertEquals(2, vm.uiState.first { it.unreadNotifications > 0 }.unreadNotifications)
    }

    @Test
    fun newAndReadNotificationsUpdateTheCount() = runTest {
        val notifications = FakeNotificationRepository()
        val vm = HomeViewModel(notifications, FakeAuthRepository(initialUser = SampleData.demoUser))
        vm.uiState.first { it.unreadNotifications == 2 }

        notifications.notify(SampleData.demoUser.id, "Nuevo reporte", "Cerca de ti", NotificationType.Report)
        vm.uiState.first { it.unreadNotifications == 3 }

        notifications.markAllAsRead(SampleData.demoUser.id)
        assertEquals(0, vm.uiState.first { it.unreadNotifications == 0 }.unreadNotifications)
    }

    @Test
    fun signingOutClearsTheBadge() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)
        val vm = HomeViewModel(FakeNotificationRepository(), auth)
        // Collection runs: the signed-in count reaches the state before the sign-out.
        vm.uiState.first { it.unreadNotifications == 2 }

        auth.signOut()

        assertEquals(0, vm.uiState.first { it.unreadNotifications == 0 }.unreadNotifications)
    }

    @Test
    fun badgeLabelIncludesTheCount() {
        assertEquals("Notificaciones", notificationsTabDescription(0))
        assertEquals("Notificaciones, 1 sin leer", notificationsTabDescription(1))
        assertEquals("Notificaciones, 3 sin leer", notificationsTabDescription(3))
    }

    @Test
    fun badgeTextCapsLargeCounts() {
        assertEquals("9", badgeText(9))
        assertEquals("99+", badgeText(100))
    }
}
