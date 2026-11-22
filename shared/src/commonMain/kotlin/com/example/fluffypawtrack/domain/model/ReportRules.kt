package com.example.fluffypawtrack.domain.model

/** Priority rules of the new-report flow (docs/REQUIREMENTS.md RF-01, point 5). */
object ReportRules {

    /**
     * Priority the system proposes for a category; the user may change it unless it is locked.
     *
     * Assumption: RF-01 only fixes "Estado crítico" to urgent. The other
     * mappings are a product assumption pending confirmation.
     */
    fun suggestedPriority(category: ReportCategory): Priority = when (category) {
        ReportCategory.Street -> Priority.Low
        ReportCategory.PossiblyLost -> Priority.Medium
        ReportCategory.Abandoned -> Priority.Medium
        ReportCategory.Mistreated -> Priority.High
        ReportCategory.Injured -> Priority.High
        ReportCategory.Critical -> Priority.Urgent
    }

    /** "Estado crítico" is always urgent; the user cannot lower it. */
    fun isPriorityLocked(category: ReportCategory): Boolean = category == ReportCategory.Critical

    /** The priority that applies when the user asks for [requested] on [category]. */
    fun resolvePriority(category: ReportCategory, requested: Priority): Priority =
        if (isPriorityLocked(category)) suggestedPriority(category) else requested
}
