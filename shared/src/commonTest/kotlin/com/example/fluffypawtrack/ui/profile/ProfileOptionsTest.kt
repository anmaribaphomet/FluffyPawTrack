package com.example.fluffypawtrack.ui.profile

import com.example.fluffypawtrack.domain.model.Role
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileOptionsTest {

    @Test
    fun userKeepsEveryAccountRow() {
        assertEquals(
            listOf(
                ProfileOption.PersonalInfo,
                ProfileOption.MyReports,
                ProfileOption.Favorites,
                ProfileOption.ContactRequests,
                ProfileOption.MyAdoptionRequests,
            ),
            profileOptionsFor(Role.User),
        )
    }

    @Test
    fun affiliateSeesPersonalInfoAndSettings() {
        assertEquals(listOf(ProfileOption.PersonalInfo, ProfileOption.Settings), profileOptionsFor(Role.Affiliate))
    }

    @Test
    fun adminSeesPersonalInfoAndSettings() {
        assertEquals(listOf(ProfileOption.PersonalInfo, ProfileOption.Settings), profileOptionsFor(Role.Admin))
    }
}
