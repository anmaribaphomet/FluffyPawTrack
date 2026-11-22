package com.example.fluffypawtrack.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportRulesTest {

    @Test
    fun suggestsAPriorityForEveryCategory() {
        val expected = mapOf(
            ReportCategory.Street to Priority.Low,
            ReportCategory.PossiblyLost to Priority.Medium,
            ReportCategory.Abandoned to Priority.Medium,
            ReportCategory.Mistreated to Priority.High,
            ReportCategory.Injured to Priority.High,
            ReportCategory.Critical to Priority.Urgent,
        )
        ReportCategory.entries.forEach { category ->
            assertEquals(expected.getValue(category), ReportRules.suggestedPriority(category), "category $category")
        }
    }

    @Test
    fun criticalIsLockedToUrgent() {
        assertTrue(ReportRules.isPriorityLocked(ReportCategory.Critical))
        Priority.entries.forEach { requested ->
            assertEquals(Priority.Urgent, ReportRules.resolvePriority(ReportCategory.Critical, requested))
        }
    }

    @Test
    fun otherCategoriesKeepTheUserChoice() {
        ReportCategory.entries.filter { it != ReportCategory.Critical }.forEach { category ->
            assertFalse(ReportRules.isPriorityLocked(category), "category $category")
            Priority.entries.forEach { requested ->
                assertEquals(requested, ReportRules.resolvePriority(category, requested))
            }
        }
    }
}
