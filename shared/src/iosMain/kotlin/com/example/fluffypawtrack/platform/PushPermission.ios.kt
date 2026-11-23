package com.example.fluffypawtrack.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

// iOS is not built; push is Android only, so the explainer never shows (apiLevel 0).
@Composable
actual fun rememberNotificationPermissionController(): NotificationPermissionController = remember {
    object : NotificationPermissionController {
        override val status = NotificationPermissionStatus(
            apiLevel = 0,
            granted = false,
            permanentlyDenied = false,
            dismissed = true,
        )

        override fun request() = Unit

        override fun dismiss() = Unit
    }
}
