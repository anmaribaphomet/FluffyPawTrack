package com.example.fluffypawtrack.domain.model

enum class ListingStatus { Available, Adopted }

data class AdoptionListing(
    val id: String,
    val organizationId: String,
    val name: String,
    val species: String,
    val breed: String,
    val age: String,
    val vaccination: String,
    val description: String,
    val photoUrls: List<String>,
    val status: ListingStatus,
)
