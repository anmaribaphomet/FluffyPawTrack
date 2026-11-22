package com.example.fluffypawtrack.di

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.FakeReportRepository
import com.example.fluffypawtrack.data.supabase.SupabaseConfig
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

class AppContainerTest {
    @Test
    fun withoutConfigUsesFakesAndNoClient() {
        val container = AppContainer()

        assertIs<FakeAuthRepository>(container.authRepository)
        assertIs<FakeReportRepository>(container.reportRepository)
        assertFalse(container.usesSupabase)
        assertNull(container.supabaseClient)
    }

    @Test
    fun withBlankConfigUsesFakesAndNoClient() {
        val container = AppContainer(supabaseConfig = SupabaseConfig(url = " ", publishableKey = ""))

        assertIs<FakeAuthRepository>(container.authRepository)
        assertIs<FakeReportRepository>(container.reportRepository)
        assertFalse(container.usesSupabase)
        assertNull(container.supabaseClient)
    }

    @Test
    fun configToStringHidesTheKey() {
        val text = SupabaseConfig(url = "https://example.supabase.co", publishableKey = "test-publishable-key").toString()

        assertFalse(text.contains("test-publishable-key"))
    }
}
