package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.ProfileRules
import com.example.fluffypawtrack.domain.model.Role
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeProfileEditingTest {

    private val affiliate = SampleData.users.first { it.role == Role.Affiliate }

    @Test
    fun updateProfileChangesNamePhoneAndPhotoAndRefreshesTheSession() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)

        val updated = auth.updateProfile(" Ana ", " López ", "662 123 4567", "memory://photo/1").getOrThrow()

        assertEquals("Ana", updated.firstName)
        assertEquals("López", updated.lastName)
        assertEquals("6621234567", updated.phone)
        assertEquals("memory://photo/1", updated.avatarUrl)
        assertEquals(SampleData.demoUser.email, updated.email)
        assertEquals(updated, auth.currentUser.value)
    }

    @Test
    fun updateProfileKeepsRoleAndOrganization() = runTest {
        val auth = FakeAuthRepository(initialUser = affiliate)

        val updated = auth.updateProfile("Nuevo", "Nombre", "6620000000", null).getOrThrow()

        assertEquals(Role.Affiliate, updated.role)
        assertEquals(affiliate.organizationId, updated.organizationId)
    }

    @Test
    fun savedChangesSurviveSigningInAgain() = runTest {
        val auth = FakeAuthRepository()
        val email = SampleData.demoUser.email!!
        auth.signIn(email, SampleData.DEMO_PASSWORD).getOrThrow()
        auth.updateProfile("Renombrada", "", null, null).getOrThrow()
        auth.signOut()

        val again = auth.signIn(email, SampleData.DEMO_PASSWORD).getOrThrow()

        assertEquals("Renombrada", again.firstName)
        assertEquals(null, again.phone)
    }

    @Test
    fun rejectsBlankFirstNameAndPhonesThatAreNotTenDigits() = runTest {
        val auth = FakeAuthRepository(initialUser = SampleData.demoUser)

        val blankName = auth.updateProfile("  ", "López", "6621234567", null)
        val shortPhone = auth.updateProfile("Ana", "López", "66212345", null)
        val lettersInPhone = auth.updateProfile("Ana", "López", "66212345ab", null)

        assertEquals(ProfileRules.FIRST_NAME_REQUIRED, blankName.exceptionOrNull()?.message)
        assertEquals(ProfileRules.PHONE_INVALID, shortPhone.exceptionOrNull()?.message)
        assertEquals(ProfileRules.PHONE_INVALID, lettersInPhone.exceptionOrNull()?.message)
        assertEquals(SampleData.demoUser, auth.currentUser.value)
    }

    @Test
    fun anonymousSessionsAndSignedOutCallersCannotEditAProfile() = runTest {
        val auth = FakeAuthRepository()
        assertTrue(auth.updateProfile("Ana", "", null, null).isFailure)

        auth.signInAnonymously().getOrThrow()
        assertTrue(auth.updateProfile("Ana", "", null, null).isFailure)
    }
}
