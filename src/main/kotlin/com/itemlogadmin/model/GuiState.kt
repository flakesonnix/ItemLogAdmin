package com.itemlogadmin.model

import java.util.UUID

sealed class GuiState {
    data class PlayerList(val page: Int, val query: String?) : GuiState()
    data class EventList(
        val playerId: UUID?,
        val page: Int,
        val filter: String?,
        val materialFilter: String? = null,
        val timeFilter: TimeFilter? = null,
    ) : GuiState()
    data class EventDetails(val eventId: UUID) : GuiState()
    data class RestoreConfirm(val eventId: UUID, val targetId: UUID) : GuiState()

    enum class TimeFilter(val displayName: String, val hours: Long) {
        LAST_HOUR("Last Hour", 1),
        LAST_DAY("Last 24h", 24),
        LAST_WEEK("Last Week", 24 * 7),
        LAST_MONTH("Last 30d", 24 * 30),
        ALL("All Time", -1),
    }
}
