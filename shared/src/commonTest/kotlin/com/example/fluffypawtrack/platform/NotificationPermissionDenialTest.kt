package com.example.fluffypawtrack.platform

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NotificationPermissionDenialTest {

    @Test
    fun aSingleDenialOrDismissalIsNotPermanent() {
        assertFalse(isNotificationPermissionPermanentlyDenied(granted = false, denials = 1, showRationale = false))
        assertFalse(isNotificationPermissionPermanentlyDenied(granted = false, denials = 1, showRationale = true))
    }

    @Test
    fun twoDenialsWithoutRationaleArePermanent() {
        assertTrue(isNotificationPermissionPermanentlyDenied(granted = false, denials = 2, showRationale = false))
        assertTrue(isNotificationPermissionPermanentlyDenied(granted = false, denials = 3, showRationale = false))
    }

    @Test
    fun aDismissalFollowedByADenialStillAllowsAskingAgain() {
        // The system still shows the rationale, so it lets the app ask once more.
        assertFalse(isNotificationPermissionPermanentlyDenied(granted = false, denials = 2, showRationale = true))
    }

    @Test
    fun neverWhenGranted() {
        assertFalse(isNotificationPermissionPermanentlyDenied(granted = true, denials = 2, showRationale = false))
    }

    @Test
    fun neverBeforeAnyRequest() {
        assertFalse(isNotificationPermissionPermanentlyDenied(granted = false, denials = 0, showRationale = false))
    }
}
