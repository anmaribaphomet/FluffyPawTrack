package com.example.fluffypawtrack

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.fluffypawtrack.data.supabase.SupabaseConfig
import com.example.fluffypawtrack.push.PushNotifications

class MainActivity : ComponentActivity() {
    /** Report of a tapped push notification, until the app has navigated to it. */
    private var openReportId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        PushNotifications.ensureChannel(this)

        // After recreation the launch intent was already handled; keep only a link still pending.
        openReportId = if (savedInstanceState == null) {
            intent.reportId()
        } else {
            savedInstanceState.getString(PushNotifications.EXTRA_REPORT_ID)
        }

        val config = SupabaseConfig(
            url = BuildConfig.SUPABASE_URL,
            publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
        )
        setContent {
            App(config = config, openReportId = openReportId, onReportOpened = { openReportId = null })
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.reportId()?.let { openReportId = it }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PushNotifications.EXTRA_REPORT_ID, openReportId)
    }

    /** The notification's report, ignoring a stale intent replayed from Recents. */
    private fun Intent.reportId(): String? {
        if (flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return null
        return getStringExtra(PushNotifications.EXTRA_REPORT_ID)?.takeIf { it.isNotBlank() }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
