package com.example.fluffypawtrack.platform

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

private const val PREFS = "push_permission"
private const val KEY_DISMISSED = "explainer_dismissed"
private const val KEY_DENIALS = "permission_denials"

@Composable
actual fun rememberNotificationPermissionController(): NotificationPermissionController {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val controller = remember(context, activity) { AndroidNotificationPermission(context, activity) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        controller.onRequestResult(granted)
    }
    controller.launcher = launcher
    // The user may grant or revoke it in system settings while the app is in the background.
    LifecycleResumeEffect(controller) {
        controller.refresh()
        onPauseOrDispose { }
    }
    return controller
}

private class AndroidNotificationPermission(
    private val context: Context,
    private val activity: Activity?,
) : NotificationPermissionController {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private var answeredThisSession = false
    var launcher: ManagedActivityResultLauncher<String, Boolean>? = null

    override var status by mutableStateOf(read())
        private set

    override fun request() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        launcher?.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun dismiss() {
        prefs.edit().putBoolean(KEY_DISMISSED, true).apply()
        refresh()
    }

    fun onRequestResult(granted: Boolean) {
        if (!granted) prefs.edit().putInt(KEY_DENIALS, prefs.getInt(KEY_DENIALS, 0) + 1).apply()
        // After one answer the explainer waits for the next launch.
        answeredThisSession = true
        refresh()
    }

    fun refresh() {
        status = read()
    }

    private fun read(): NotificationPermissionStatus {
        val apiLevel = Build.VERSION.SDK_INT
        val granted = apiLevel < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val permanentlyDenied = apiLevel >= Build.VERSION_CODES.TIRAMISU &&
            isNotificationPermissionPermanentlyDenied(
                granted = granted,
                denials = prefs.getInt(KEY_DENIALS, 0),
                showRationale = activity?.shouldShowRequestPermissionRationale(
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == true,
            )
        return NotificationPermissionStatus(
            apiLevel = apiLevel,
            granted = granted,
            permanentlyDenied = permanentlyDenied,
            dismissed = answeredThisSession || prefs.getBoolean(KEY_DISMISSED, false),
        )
    }
}
