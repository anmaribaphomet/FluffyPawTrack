package com.example.fluffypawtrack.ui.auth

import com.example.fluffypawtrack.domain.model.UserProfile
import com.example.fluffypawtrack.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Auth repository whose every call fails with a non-auth error, like a lost connection. */
class FailingAuthRepository(
    private val error: Throwable = IllegalStateException("network unreachable"),
) : AuthRepository {
    override val currentUser: StateFlow<UserProfile?> = MutableStateFlow(null)

    override suspend fun signIn(email: String, password: String): Result<UserProfile> = Result.failure(error)

    override suspend fun signUp(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        phone: String?,
    ): Result<UserProfile> = Result.failure(error)

    override suspend fun signInAnonymously(): Result<UserProfile> = Result.failure(error)

    override suspend fun updateProfile(
        firstName: String,
        lastName: String,
        phone: String?,
        photoRef: String?,
    ): Result<UserProfile> = Result.failure(error)

    override suspend fun signOut() = Unit
}
