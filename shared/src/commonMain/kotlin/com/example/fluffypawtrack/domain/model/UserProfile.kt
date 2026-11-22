package com.example.fluffypawtrack.domain.model

enum class Role { User, Affiliate, Admin }

data class UserProfile(
    val id: String,
    val email: String?,
    val firstName: String,
    val lastName: String,
    val phone: String?,
    val role: Role,
    val organizationId: String?,
    /** True for Supabase anonymous sign-in sessions. */
    val isAnonymous: Boolean,
)
