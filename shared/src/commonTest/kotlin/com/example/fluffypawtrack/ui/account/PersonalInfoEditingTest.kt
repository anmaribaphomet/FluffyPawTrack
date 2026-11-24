package com.example.fluffypawtrack.ui.account

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.data.fake.SampleData
import com.example.fluffypawtrack.domain.model.ProfileRules
import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.model.UserProfile
import com.example.fluffypawtrack.domain.repository.AuthRepository
import com.example.fluffypawtrack.ui.auth.FailingAuthRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PersonalInfoEditingTest {

    private val demo = SampleData.demoUser

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun <T> TestScope.collecting(state: StateFlow<T>): StateFlow<T> {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
        return state
    }

    @Test
    fun editStartsFromTheCurrentDataAndCancelDiscardsChanges() = runTest {
        val vm = PersonalInfoViewModel(FakeAuthRepository(initialUser = demo))
        val state = collecting(vm.uiState)
        assertTrue(state.value.canEdit)
        assertFalse(state.value.isEditing)

        vm.onEdit()
        assertEquals(PersonalInfoDraft(demo.firstName, demo.lastName, demo.phone.orEmpty(), demo.avatarUrl), state.value.draft)

        vm.onFirstNameChange("Otra")
        vm.onCancel()

        assertNull(state.value.draft)
        assertEquals(demo.firstName, state.value.firstName)
    }

    @Test
    fun saveUpdatesTheProfileAndShowsTheConfirmation() = runTest {
        val auth = FakeAuthRepository(initialUser = demo)
        val vm = PersonalInfoViewModel(auth)
        val state = collecting(vm.uiState)

        vm.onEdit()
        vm.onFirstNameChange("Ana")
        vm.onLastNameChange("López")
        vm.onPhoneChange("6621234567")
        vm.onPhotoSelected("memory://photo/7")
        vm.onSave()

        assertNull(state.value.draft)
        assertTrue(state.value.saved)
        assertEquals(PERSONAL_INFO_SAVED, state.value.confirmation)
        assertEquals("Cambios guardados", PERSONAL_INFO_SAVED)
        assertEquals("Ana", state.value.firstName)
        assertEquals("López", state.value.lastName)
        assertEquals("6621234567", state.value.phone)
        assertEquals("memory://photo/7", state.value.avatarUrl)
        assertEquals("Ana", auth.currentUser.value?.firstName)
    }

    @Test
    fun emailIsShownButNeverEdited() = runTest {
        val auth = FakeAuthRepository(initialUser = demo)
        val vm = PersonalInfoViewModel(auth)
        val state = collecting(vm.uiState)

        vm.onEdit()
        vm.onFirstNameChange("Ana")
        vm.onSave()

        assertEquals(demo.email, state.value.email)
        assertEquals(demo.email, auth.currentUser.value?.email)
    }

    @Test
    fun invalidDataShowsFieldErrorsAndKeepsEditing() = runTest {
        val auth = FakeAuthRepository(initialUser = demo)
        val vm = PersonalInfoViewModel(auth)
        val state = collecting(vm.uiState)

        vm.onEdit()
        vm.onFirstNameChange(" ")
        vm.onPhoneChange("123")
        vm.onSave()

        val draft = assertNotNull(state.value.draft)
        assertEquals(ProfileRules.FIRST_NAME_REQUIRED, draft.firstNameError)
        assertEquals(ProfileRules.PHONE_INVALID, draft.phoneError)
        assertFalse(state.value.saved)
        assertEquals(demo, auth.currentUser.value)
    }

    @Test
    fun removingThePhotoSavesNoAvatar() = runTest {
        val auth = FakeAuthRepository(initialUser = demo.copy(avatarUrl = "memory://photo/1"))
        val vm = PersonalInfoViewModel(auth)
        val state = collecting(vm.uiState)

        vm.onEdit()
        vm.onPhotoRemoved()
        vm.onSave()

        assertNull(state.value.avatarUrl)
    }

    @Test
    fun signedOutCallersHaveNothingToEdit() = runTest {
        val vm = PersonalInfoViewModel(FailingAuthRepository())
        val state = collecting(vm.uiState)

        vm.onEdit()

        assertNull(state.value.draft)
        assertFalse(state.value.canEdit)
    }

    @Test
    fun repositoryFailureIsShownAsAReadableMessageAndKeepsTheDraft() = runTest {
        val fake = FakeAuthRepository(initialUser = demo)
        val failing = object : AuthRepository by fake {
            override suspend fun updateProfile(
                firstName: String,
                lastName: String,
                phone: String?,
                photoRef: String?,
            ): Result<UserProfile> = Result.failure(IllegalStateException("Sin conexión. Intenta de nuevo."))
        }
        val vm = PersonalInfoViewModel(failing)
        val state = collecting(vm.uiState)

        vm.onEdit()
        vm.onFirstNameChange("Ana")
        vm.onSave()

        assertEquals("Sin conexión. Intenta de nuevo.", state.value.message)
        assertEquals("Ana", state.value.draft?.firstName)
        assertFalse(state.value.saved)
    }

    @Test
    fun aHangingSaveEndsWithAMessageAndUnlocksCancel() = runTest {
        val fake = FakeAuthRepository(initialUser = demo)
        val hanging = object : AuthRepository by fake {
            override suspend fun updateProfile(
                firstName: String,
                lastName: String,
                phone: String?,
                photoRef: String?,
            ): Result<UserProfile> = awaitCancellation()
        }
        val vm = PersonalInfoViewModel(hanging)
        val state = collecting(vm.uiState)

        vm.onEdit()
        vm.onSave()
        assertTrue(state.value.isSaving)
        advanceTimeBy(PROFILE_SAVE_TIMEOUT_MILLIS + 1)

        assertFalse(state.value.isSaving)
        assertEquals(PROFILE_SAVE_TIMEOUT_MESSAGE, state.value.message)
        vm.onCancel()
        assertNull(state.value.draft)
    }

    @Test
    fun affiliatesAndAdminsCanEditAndKeepTheirRole() = runTest {
        for (user in SampleData.users.filter { it.role != Role.User }) {
            val auth = FakeAuthRepository(initialUser = user)
            val vm = PersonalInfoViewModel(auth)
            val state = collecting(vm.uiState)
            assertTrue(state.value.canEdit, "${user.role} should edit")

            vm.onEdit()
            vm.onLastNameChange("Nuevo")
            vm.onSave()

            assertEquals(user.role, auth.currentUser.value?.role)
            assertEquals(user.organizationId, auth.currentUser.value?.organizationId)
        }
    }

    @Test
    fun anonymousSessionsCannotEdit() = runTest {
        val auth = FakeAuthRepository()
        auth.signInAnonymously().getOrThrow()
        val vm = PersonalInfoViewModel(auth)
        val state = collecting(vm.uiState)

        vm.onEdit()

        assertFalse(state.value.canEdit)
        assertNull(state.value.draft)
    }
}
