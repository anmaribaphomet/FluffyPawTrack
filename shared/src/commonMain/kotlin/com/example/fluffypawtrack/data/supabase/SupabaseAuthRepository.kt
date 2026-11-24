package com.example.fluffypawtrack.data.supabase

import com.example.fluffypawtrack.domain.model.EmailConfirmationRequiredException
import com.example.fluffypawtrack.domain.model.ProfileRules
import com.example.fluffypawtrack.domain.model.UserProfile
import com.example.fluffypawtrack.domain.repository.AuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Email and anonymous sign-in with Supabase Auth. [currentUser] follows the library's session
 * (restored from storage on start) joined with the user's `profiles` row; the email comes from
 * the session, not from `profiles`.
 */
internal class SupabaseAuthRepository(
    private val client: SupabaseClient,
    scope: CoroutineScope,
    private val queries: QueryRegistry,
    private val photos: PhotoUploader,
) : AuthRepository {
    private val session = MutableStateFlow<UserProfile?>(null)

    override val currentUser: StateFlow<UserProfile?> = session.asStateFlow()

    init {
        scope.launch {
            client.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val user = status.session.user ?: client.auth.currentUserOrNull()
                        if (user != null && session.value?.id != user.id) session.value = loadProfile(user)
                    }
                    is SessionStatus.NotAuthenticated -> session.value = null
                    // Keep the last known user while the library restores or refreshes the session.
                    else -> Unit
                }
            }
        }
    }

    override suspend fun signIn(email: String, password: String): Result<UserProfile> = remoteAuth {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
        signedInProfile()
    }

    override suspend fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String?,
    ): Result<UserProfile> = remoteAuth {
        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            // The on_auth_user_created trigger copies these into `profiles`.
            data = buildJsonObject {
                put("first_name", firstName)
                put("last_name", lastName)
                put("phone", phone)
            }
        }
        // With "Confirm email" on, Supabase creates the account but opens no session.
        if (client.auth.currentSessionOrNull() == null) throw EmailConfirmationRequiredException()
        signedInProfile()
    }

    override suspend fun signInAnonymously(): Result<UserProfile> = remoteAuth {
        client.auth.signInAnonymously()
        signedInProfile()
    }

    override suspend fun updateProfile(
        firstName: String,
        lastName: String,
        phone: String?,
        photoRef: String?,
    ): Result<UserProfile> = remote {
        ProfileRules.problemOf(firstName, phone)?.let { throw RuleViolation(it) }
        val user = client.auth.currentUserOrNull()?.takeUnless { it.isAnonymous == true }
            ?: throw RuleViolation(ProfileRules.SIGN_IN_REQUIRED)
        val avatar = photoRef?.let { photos.store(it, StoragePaths.AVATAR_PHOTOS, folder = user.id) }
        val values = profileUpdateOf(firstName.trim(), lastName.trim(), ProfileRules.normalizedPhone(phone), avatar)
        // RLS (profiles_update_own) limits the row to the caller; no returned row means it refused.
        val row = client.from("profiles").update(values) {
            select()
            filter { eq("id", user.id) }
        }.decodeList<ProfileDto>().firstOrNull() ?: throw RuleViolation("No pudimos guardar tus datos. Intenta de nuevo.")
        userProfileOf(user.id, user.email?.takeIf { it.isNotBlank() }, isAnonymous = false, profile = row)
            .also { session.value = it }
    }

    override suspend fun signOut() {
        try {
            client.auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Throwable) {
            // Offline: forget the local session anyway so the user is signed out of the app.
            client.auth.clearSession()
        }
        session.value = null
        queries.refreshAll()
    }

    private suspend fun signedInProfile(): UserProfile {
        val user = client.auth.currentUserOrNull() ?: error("Supabase returned no user")
        return loadProfile(user).also {
            session.value = it
            queries.refreshAll()
        }
    }

    private suspend fun loadProfile(user: UserInfo): UserProfile {
        val profile = remoteOrNull {
            client.from("profiles").select { filter { eq("id", user.id) } }.decodeList<ProfileDto>().firstOrNull()
        }
        return userProfileOf(
            id = user.id,
            email = user.email?.takeIf { it.isNotBlank() },
            isAnonymous = user.isAnonymous == true,
            profile = profile,
        )
    }
}
