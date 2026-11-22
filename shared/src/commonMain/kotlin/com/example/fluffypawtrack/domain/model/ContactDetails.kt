package com.example.fluffypawtrack.domain.model

/**
 * Contact data of the other party of an accepted request. For an organization it is the
 * organization's name, phone and email, not those of the person who answered.
 */
data class ContactDetails(
    val fullName: String,
    val phone: String,
    val email: String,
)
