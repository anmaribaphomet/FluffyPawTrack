package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.NotificationPrefs
import com.example.fluffypawtrack.domain.model.Priority
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeNotificationPrefsRepositoryTest {

    private val prefs = FakeNotificationPrefsRepository()

    @Test
    fun defaultsAreFiveKmEveryPriorityAndPushOn() = runTest {
        val defaults = prefs.observe("user-demo").first()

        assertEquals(5.0, defaults.radiusKm)
        assertEquals(Priority.entries.toSet(), defaults.priorities)
        assertEquals(true, defaults.pushEnabled)
    }

    @Test
    fun updateIsSavedPerUser() = runTest {
        val saved = prefs.update("user-demo", 12.5, setOf(Priority.High, Priority.Urgent), pushEnabled = false).getOrThrow()

        assertEquals(NotificationPrefs(12.5, setOf(Priority.High, Priority.Urgent), false), saved)
        assertEquals(saved, prefs.observe("user-demo").first())
        assertEquals(NotificationPrefs(), prefs.observe("user-2").first())
    }

    @Test
    fun radiusMustBePositive() = runTest {
        val result = prefs.update("user-demo", 0.0, Priority.entries.toSet(), pushEnabled = true)

        assertEquals(NotificationPrefs.INVALID_RADIUS, result.exceptionOrNull()?.message)
        assertEquals(NotificationPrefs(), prefs.observe("user-demo").first())
    }
}
