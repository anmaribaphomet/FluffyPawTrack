package com.example.fluffypawtrack.ui.notifications

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeNotificationRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.AppNotification
import com.example.fluffypawtrack.domain.model.NotificationType
import com.example.fluffypawtrack.domain.repository.NotificationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.test.assertFalse
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
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

    @Test
    fun fakeRepositoryNeverReportsALoadFailure() = runTest {
        val state = viewModel().uiState.first { it.notifications.isNotEmpty() }
        assertFalse(state.loadFailed)
    }

    @Test
    fun failedLoadShowsTheErrorUntilRetrySucceeds() = runTest {
        val repository = FailingNotificationRepository()
        val vm = NotificationsViewModel(repository, FakeAuthRepository(initialUser = SampleData.demoUser))
        assertTrue(vm.uiState.first { it.loadFailed }.notifications.isEmpty())

        vm.onRetry()

        val state = vm.uiState.first { !it.loadFailed }
        assertEquals(listOf(SampleData.demoUser.id), repository.reloadedFor)
        assertEquals(1, state.notifications.size)
    }

    /** Fails every load until [reload] is called; then it returns one notification. */
    private class FailingNotificationRepository : NotificationRepository {
        private val delegate = FakeNotificationRepository()
        private val failed = MutableStateFlow(true)
        val reloadedFor = mutableListOf<String>()

        override fun observeNotifications(userId: String): Flow<List<AppNotification>> =
            failed.map { if (it) emptyList() else listOf(notification()) }

        override fun observeLoadFailed(userId: String): Flow<Boolean> = failed

        override suspend fun reload(userId: String) {
            reloadedFor += userId
            failed.value = false
        }

        override suspend fun markAsRead(notificationId: String) = delegate.markAsRead(notificationId)

        override suspend fun markAllAsRead(userId: String) = delegate.markAllAsRead(userId)

        override suspend fun notify(userId: String, title: String, message: String, type: NotificationType) =
            delegate.notify(userId, title, message, type)

        @OptIn(ExperimentalTime::class)
        private fun notification() = AppNotification(
            id = "n1",
            title = "Nuevo reporte",
            message = "Hay un reporte cerca de ti",
            receivedAt = Instant.fromEpochSeconds(0),
            type = NotificationType.Report,
        )
    }
}
