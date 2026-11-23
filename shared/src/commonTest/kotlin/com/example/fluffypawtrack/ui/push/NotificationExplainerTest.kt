package com.example.fluffypawtrack.ui.push

import com.example.fluffypawtrack.platform.NotificationPermissionStatus
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationExplainerTest {
    private val pending = NotificationPermissionStatus(
        apiLevel = 33,
        granted = false,
        permanentlyDenied = false,
        dismissed = false,
    )

    @Test
    fun showsOnAndroid13WhenThePermissionIsStillMissing() {
        assertTrue(shouldShowNotificationExplainer(pending))
        assertTrue(shouldShowNotificationExplainer(pending.copy(apiLevel = 35)))
    }

    @Test
    fun skipsOlderAndroidVersionsThatNeedNoRuntimePermission() {
        assertFalse(shouldShowNotificationExplainer(pending.copy(apiLevel = 32)))
    }

    @Test
    fun skipsWhenGranted() {
        assertFalse(shouldShowNotificationExplainer(pending.copy(granted = true)))
    }

    @Test
    fun skipsWhenPermanentlyDenied() {
        assertFalse(shouldShowNotificationExplainer(pending.copy(permanentlyDenied = true)))
    }

    @Test
    fun skipsAfterNotNow() {
        assertFalse(shouldShowNotificationExplainer(pending.copy(dismissed = true)))
    }
}
