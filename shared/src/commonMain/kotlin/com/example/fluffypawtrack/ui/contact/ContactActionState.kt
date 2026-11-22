package com.example.fluffypawtrack.ui.contact

import com.example.fluffypawtrack.domain.model.ContactRequestRules
import com.example.fluffypawtrack.domain.model.Report
import com.example.fluffypawtrack.domain.model.ReportCategory
import com.example.fluffypawtrack.domain.model.ReportStatus
import com.example.fluffypawtrack.domain.model.Role
import com.example.fluffypawtrack.domain.model.UserProfile
import com.example.fluffypawtrack.domain.model.contactRecipient

/** How the "Es mi mascota" button looks in a report's detail sheet. */
sealed interface ContactActionState {
    data object Hidden : ContactActionState
    data object Enabled : ContactActionState
    data class Disabled(val reason: String) : ContactActionState
}

/**
 * Only possibly-lost, open reports of someone else get the button, and only `user`
 * accounts can use it. The repository enforces the same rules again on send.
 */
fun contactActionState(report: Report, user: UserProfile?): ContactActionState = when {
    report.category != ReportCategory.PossiblyLost || report.status == ReportStatus.Closed -> ContactActionState.Hidden
    user != null && !user.isAnonymous && user.role != Role.User -> ContactActionState.Hidden
    user != null && report.reporterId == user.id -> ContactActionState.Hidden
    report.contactRecipient == null -> ContactActionState.Disabled(ContactRequestRules.ANONYMOUS_REPORT_NOTICE)
    user == null || user.isAnonymous ->
        ContactActionState.Disabled("Necesitas una cuenta para enviar solicitudes de contacto")
    else -> ContactActionState.Enabled
}
