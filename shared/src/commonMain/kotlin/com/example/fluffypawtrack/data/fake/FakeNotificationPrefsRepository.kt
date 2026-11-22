package com.example.fluffypawtrack.data.fake

import com.example.fluffypawtrack.domain.model.NotificationPrefs
import com.example.fluffypawtrack.domain.model.Priority
import com.example.fluffypawtrack.domain.repository.NotificationPrefsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory `notification_prefs`: users without a row get the column defaults. */
class FakeNotificationPrefsRepository : NotificationPrefsRepository {
    private val prefs = MutableStateFlow<Map<String, NotificationPrefs>>(emptyMap())

    override fun observe(userId: String): Flow<NotificationPrefs> =
        prefs.map { it[userId] ?: NotificationPrefs() }

    override suspend fun update(
        userId: String,
        radiusKm: Double,
        priorities: Set<Priority>,
        pushEnabled: Boolean,
    ): Result<NotificationPrefs> {
        if (radiusKm <= 0.0) return Result.failure(IllegalStateException(NotificationPrefs.INVALID_RADIUS))
        val saved = NotificationPrefs(radiusKm, priorities, pushEnabled)
        prefs.update { it + (userId to saved) }
        return Result.success(saved)
    }
}
