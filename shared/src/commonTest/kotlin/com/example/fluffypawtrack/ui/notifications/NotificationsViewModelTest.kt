package com.example.fluffypawtrack.ui.notifications

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeNotificationRepository
import com.example.fluffypawtrack.data.fake.SampleData
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationsViewModelTest {

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel() = NotificationsViewModel(
        FakeNotificationRepository(),
        FakeAuthRepository(initialUser = SampleData.demoUser),
    )

    @Test
    fun showsTheSignedInUsersNotifications() = runTest {
        val state = viewModel().uiState.first { it.notifications.isNotEmpty() }
        // Newest first.
        assertEquals(listOf("notification-3", "notification-2", "notification-1"), state.notifications.map { it.id })
        assertEquals(2, state.unreadCount)
    }

    @Test
    fun openingANotificationMarksOnlyItAsRead() = runTest {
        val vm = viewModel()
        vm.onNotificationClick("notification-2")
        val state = vm.uiState.first { it.unreadCount == 1 }
        assertTrue(state.notifications.single { it.id == "notification-2" }.isRead)
    }

    @Test
    fun markAllAsReadLeavesNothingUnread() = runTest {
        val vm = viewModel()
        vm.onMarkAllAsRead()
        val state = vm.uiState.first { it.notifications.isNotEmpty() && it.unreadCount == 0 }
        assertTrue(state.notifications.all { it.isRead })
    }

    @Test
    fun signedOutUserSeesNoNotifications() = runTest {
        val vm = NotificationsViewModel(FakeNotificationRepository(), FakeAuthRepository())
        assertTrue(vm.uiState.value.notifications.isEmpty())
    }
}
