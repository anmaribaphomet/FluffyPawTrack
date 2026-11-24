package com.example.fluffypawtrack.ui.auth

import com.example.fluffypawtrack.data.fake.FakeAuthRepository
import com.example.fluffypawtrack.domain.repository.AuthRepository
import kotlinx.coroutines.awaitCancellation

/** How the stubbed remote sign-out misbehaves. */
enum class SignOutFailure { Throws, Hangs }

/** A [FakeAuthRepository] whose sign-out fails or never returns, like a request with no network. */
class BrokenSignOutAuthRepository(
    private val failure: SignOutFailure,
    private val base: FakeAuthRepository = FakeAuthRepository(),
) : AuthRepository by base {
    var signOutCalls = 0
        private set

    override suspend fun signOut() {
        signOutCalls++
        when (failure) {
            SignOutFailure.Throws -> throw IllegalStateException("network unreachable")
            SignOutFailure.Hangs -> awaitCancellation()
        }
    }
}
