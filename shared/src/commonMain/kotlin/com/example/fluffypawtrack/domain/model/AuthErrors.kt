package com.example.fluffypawtrack.domain.model

/**
 * Sign-in or sign-up was rejected because of the data entered (unknown email, wrong
 * password, email already registered). It deliberately carries no detail, so callers
 * cannot reveal which part was wrong or whether the email exists.
 */
class InvalidAuthDataException : Exception()

/**
 * The account was created, but the project requires confirming the email before the first
 * session, so the user is not signed in yet.
 */
class EmailConfirmationRequiredException : Exception()
