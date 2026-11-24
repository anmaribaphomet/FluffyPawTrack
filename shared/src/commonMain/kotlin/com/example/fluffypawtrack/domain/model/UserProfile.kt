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
    /** Profile photo reference (Storage URL, or a local ref in fake mode); null shows the default icon. */
    val avatarUrl: String? = null,
)
