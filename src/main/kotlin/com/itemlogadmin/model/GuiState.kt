package com.itemlogadmin.model

import java.util.UUID

sealed class GuiState {
    data class PlayerList(val page: Int, val query: String?) : GuiState()
    data class EventList(val playerId: UUID?, val page: Int, val filter: String?) : GuiState()
    data class EventDetails(val eventId: UUID) : GuiState()
    data class RestoreConfirm(val eventId: UUID, val targetId: UUID) : GuiState()
}
